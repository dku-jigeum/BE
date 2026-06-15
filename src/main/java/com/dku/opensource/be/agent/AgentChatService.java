package com.dku.opensource.be.agent;

import com.dku.opensource.be.agent.model.ExaoneClient;
import com.dku.opensource.be.agent.tool.AgentContext;
import com.dku.opensource.be.agent.tool.GetIssueDetailTool;
import com.dku.opensource.be.agent.tool.SearchIssuesTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 챗봇 Q&A (KAN-41) — 자유 질문을 받아 DB 검색 후 EXAONE으로 답변을 생성한다.
 *
 * 단일 턴 고정 파이프라인: search_issues → get_issue_detail(상위 2건) → 답변 생성.
 * 검색이 항상 필요하므로 Planner를 거치지 않는다.
 *
 * 상세페이지에서 질문한 경우 issueId/issueType이 함께 전달되며,
 * 해당 이슈 본문을 "현재 보고 있는 이슈"로 프롬프트에 주입한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatService {

    private static final int DETAIL_FETCH_COUNT = 2;

    private final SearchIssuesTool searchIssuesTool;
    private final GetIssueDetailTool getIssueDetailTool;
    private final ExaoneClient exaoneClient;

    private static final int HISTORY_TURN_LIMIT = 6;

    public record ChatResponse(String answer, List<SourceIssue> sources) {}

    public record SourceIssue(String id, String type, String title, String dDay) {}

    public record ChatTurn(String role, String content) {}

    public ChatResponse chat(String question, String issueId, String issueType, String userId) {
        return chat(question, issueId, issueType, userId, List.of());
    }

    public ChatResponse chat(String question, String issueId, String issueType, String userId, List<ChatTurn> history) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question은 필수입니다.");
        }

        AgentContext ctx = AgentContext.builder().userId(userId).build();

        // 현재 보고 있는 이슈 (상세페이지에서 질문한 경우)
        String currentIssue = loadCurrentIssue(issueId, issueType, ctx);

        log.info("[Chat] 검색 시작 — user={}, question={}, currentIssue={}", userId, question, currentIssue != null);
        String searchRaw = searchIssuesTool.run(question, ctx);
        List<SourceIssue> sources = parseSearchResults(searchRaw);

        if (sources.isEmpty() && currentIssue == null) {
            log.info("[Chat] 검색 결과 없음 — 폴백 응답");
            return new ChatResponse("관련된 법안·청원·입법예고를 찾지 못했어요. 질문을 조금 더 구체적으로 바꿔보시겠어요?", List.of());
        }

        StringBuilder context = new StringBuilder();
        for (SourceIssue s : sources.stream().limit(DETAIL_FETCH_COUNT).toList()) {
            context.append(getIssueDetailTool.run(s.type() + ":" + s.id(), ctx)).append("\n\n");
        }

        String answer = generateAnswer(question, currentIssue, context.toString(), sources, history);
        log.info("[Chat] 답변 생성 완료 — sources={}", sources.size());
        return new ChatResponse(answer, sources);
    }

    /** issueId/issueType이 모두 있으면 현재 이슈 본문을 조회한다. 조회 실패 시 null. */
    private String loadCurrentIssue(String issueId, String issueType, AgentContext ctx) {
        if (issueId == null || issueId.isBlank() || issueType == null || issueType.isBlank()) {
            return null;
        }
        String detail = getIssueDetailTool.run(issueType.toLowerCase() + ":" + issueId, ctx);
        // 조회 실패 메시지는 프롬프트에 넣지 않는다
        return detail.contains("TITLE:") ? detail : null;
    }

    private String generateAnswer(String question, String currentIssue, String context,
                                  List<SourceIssue> sources, List<ChatTurn> history) {
        StringBuilder titles = new StringBuilder();
        sources.forEach(s -> titles.append(String.format("- [%s] %s (%s)%n", s.type(), s.title(), s.dDay())));

        String userPrompt = """
                이전 대화:
                %s

                사용자가 현재 보고 있는 이슈:
                %s

                사용자 질문:
                %s

                관련 이슈 본문:
                %s
                전체 관련 이슈 목록:
                %s
                위 자료를 근거로 사용자 질문에 답변하라.""".formatted(
                formatHistory(history), currentIssue != null ? currentIssue : "(없음)", question, context, titles);

        try {
            return exaoneClient.complete("""
                    너는 한국 국회 법안·국민동의청원·입법예고 정보를 안내하는 어시스턴트다.

                    절대 규칙:
                    - 제공된 자료에 있는 내용만 근거로 답한다. 자료에 없는 내용은 "자료에 없다"고 말한다.
                    - "이 법안", "이 청원", "이 입법예고" 같은 표현은 '사용자가 현재 보고 있는 이슈'를 가리킨다.
                    - '이전 대화'는 맥락 파악용 참고 자료이며, 이어지는 질문의 의도를 이해하는 데만 사용한다.
                    - 법률적 결론을 단정하지 않는다.
                    - 한국어로 3~6문장 이내로 간결하게 답한다.""", userPrompt);
        } catch (Exception e) {
            log.warn("[Chat] 답변 생성 실패: {}", e.getMessage());
            return "답변 생성에 실패했어요. 아래 관련 이슈를 직접 확인해보세요.";
        }
    }

    /** 최근 대화 턴을 프롬프트용 텍스트로 변환한다. 비어 있으면 "(없음)". */
    private String formatHistory(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) return "(없음)";
        StringBuilder sb = new StringBuilder();
        for (ChatTurn turn : history.stream().skip(Math.max(0, history.size() - HISTORY_TURN_LIMIT)).toList()) {
            String speaker = "assistant".equalsIgnoreCase(turn.role()) ? "어시스턴트" : "사용자";
            sb.append(speaker).append(": ").append(turn.content()).append("\n");
        }
        return sb.toString().trim();
    }

    /** SearchIssuesTool 출력 파싱: RESULT:[bill]|ID:...|TITLE:...|DDAY:... */
    private List<SourceIssue> parseSearchResults(String raw) {
        if (raw == null || raw.isBlank() || !raw.contains("RESULT:")) return List.of();
        List<SourceIssue> results = new ArrayList<>();
        for (String line : raw.split("\n")) {
            if (!line.contains("RESULT:")) continue;
            String type = extractPipe(line, "RESULT:").map(t -> t.replaceAll("[\\[\\]]", "")).orElse("bill");
            String id = extractPipe(line, "ID:").orElse("");
            String title = extractPipe(line, "TITLE:").orElse("");
            String dDay = extractPipe(line, "DDAY:").orElse("미상");
            if (!id.isBlank()) results.add(new SourceIssue(id, type, title, dDay));
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
