package com.dku.opensource.be.agent;

import com.dku.opensource.be.agent.react.AgentEvent;
import com.dku.opensource.be.agent.react.ReActLoop;
import com.dku.opensource.be.agent.react.ReActResult;
import com.dku.opensource.be.agent.tool.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 챗봇 자율 ReAct 에이전트 + SSE 스트리밍 (KAN-44).
 *
 * EXAONE이 스스로 도구를 골라 멀티스텝으로 돌고, 진행 상황(thought/tool_start/observation/answer)을
 * SseEmitter로 실시간 전송한다. 끝나면 sources·done 이벤트를 보낸다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAgentService {

    private static final long SSE_TIMEOUT_MS = 120_000L;
    private static final int HISTORY_TURN_LIMIT = 6;

    private final ReActLoop reActLoop;
    private final AgentService agentService;
    private final ThreadPoolTaskExecutor chatExecutor;

    public record SourceIssue(String id, String type, String title, String dDay) {}

    public record ChatTurn(String role, String content) {}

    public SseEmitter chatStream(String question, String issueId, String issueType,
                                 String userId, List<ChatTurn> history) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        AgentContext ctx = (issueId != null && !issueId.isBlank()
                && issueType != null && !issueType.isBlank())
                ? agentService.buildContext(issueId, issueType, userId, null)
                : AgentContext.builder().userId(userId).build();

        String historyText = formatHistory(history);

        chatExecutor.execute(() -> {
            try {
                Consumer<AgentEvent> listener = e -> send(emitter, e.type(), e);
                ReActResult result = reActLoop.runChat(question, historyText, ctx, listener);

                List<SourceIssue> sources = parseSearchResults(result.getSearchIssuesRaw());
                send(emitter, "sources", Map.of("type", "sources", "sources", sources));
                send(emitter, "done", Map.of("type", "done"));
                emitter.complete();
            } catch (Exception ex) {
                log.warn("[ChatAgent] 스트리밍 중단: {}", ex.getMessage());
                try {
                    send(emitter, "error", AgentEvent.error("답변 생성 중 오류가 발생했어요."));
                } catch (Exception ignored) { /* 클라이언트 연결 종료 등 — 무시 */ }
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * 상세페이지 AI 분석 (KAN-44) — /analyze 와 같은 결과를 만들되, 도구 실행 과정을 SSE로 흘려보낸다.
     * tool_start/observation 이벤트 → 마지막에 result(DetailPageAnalysisResponse) → done.
     */
    public SseEmitter analyzeStream(String issueId, String issueType, String userId,
                                    AgentService.RecommendationInput rec) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        chatExecutor.execute(() -> {
            try {
                Consumer<AgentEvent> listener = e -> send(emitter, e.type(), e);
                AgentService.DetailPageAnalysisResponse result =
                        agentService.analyze(issueId, issueType, userId, rec, listener);
                send(emitter, "result", Map.of("type", "result", "result", result));
                send(emitter, "done", Map.of("type", "done"));
                emitter.complete();
            } catch (Exception ex) {
                log.warn("[AnalyzeStream] 중단: {}", ex.getMessage());
                try {
                    send(emitter, "error", AgentEvent.error("분석 중 오류가 발생했어요."));
                } catch (Exception ignored) { /* 연결 종료 — 무시 */ }
                emitter.complete();
            }
        });

        return emitter;
    }

    private void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            // 클라이언트 연결 종료 — 루프를 중단시키기 위해 런타임으로 승격
            throw new RuntimeException("SSE 전송 실패", e);
        }
    }

    private String formatHistory(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) return "(없음)";
        StringBuilder sb = new StringBuilder();
        for (ChatTurn turn : history.stream().skip(Math.max(0, history.size() - HISTORY_TURN_LIMIT)).toList()) {
            String speaker = "assistant".equalsIgnoreCase(turn.role()) ? "어시스턴트" : "사용자";
            sb.append(speaker).append(": ").append(turn.content()).append("\n");
        }
        return sb.toString().trim();
    }

    /** search_issues 출력(RESULT:[type]|ID:..|TITLE:..|DDAY:..)을 sources로 파싱한다. */
    private List<SourceIssue> parseSearchResults(String raw) {
        if (raw == null || raw.isBlank() || !raw.contains("RESULT:")) return List.of();
        List<SourceIssue> results = new ArrayList<>();
        for (String line : raw.split("\n")) {
            if (!line.contains("RESULT:")) continue;
            String type = extractPipe(line, "RESULT:").map(t -> t.replaceAll("[\\[\\]]", "")).orElse("bill");
            String id = extractPipe(line, "ID:").orElse("");
            String title = extractPipe(line, "TITLE:").orElse("");
            String dDay = extractPipe(line, "DDAY:").orElse("미상");
            if (!id.isBlank() && results.stream().noneMatch(s -> s.id().equals(id))) {
                results.add(new SourceIssue(id, type, title, dDay));
            }
        }
        return results;
    }

    private Optional<String> extractPipe(String line, String prefix) {
        return Arrays.stream(line.split("\\|"))
                .filter(seg -> seg.startsWith(prefix))
                .map(seg -> seg.substring(prefix.length()).trim())
                .filter(s -> !s.isBlank())
                .findFirst();
    }
}
