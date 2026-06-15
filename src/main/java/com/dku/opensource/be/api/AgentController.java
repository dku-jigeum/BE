package com.dku.opensource.be.api;

import com.dku.opensource.be.agent.AgentChatService;
import com.dku.opensource.be.agent.AgentService;
import com.dku.opensource.be.agent.ChatAgentService;
import com.dku.opensource.be.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * POST /api/agent/analyze — 상세페이지 AI 맞춤 분석 (ReAct + EXAONE)
 *
 * 피드에서 진입 시 matchedKeywords, matchedCategories, similarityScore 를 함께 전달하면
 * '왜 추천됐나요?' 카드 품질이 높아진다.
 * 직접 진입 시에는 해당 필드를 생략해도 된다.
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentChatService agentChatService;
    private final ChatAgentService chatAgentService;

    @PostMapping("/analyze")
    public ApiResponse<AgentService.DetailPageAnalysisResponse> analyze(
            @AuthenticationPrincipal String userId,
            @RequestBody AnalyzeRequest req) {

        AgentService.RecommendationInput rec = new AgentService.RecommendationInput(
                req.similarityScore(),
                req.matchedKeywords(),
                req.matchedCategories()
        );

        return ApiResponse.success(agentService.analyze(req.issueId(), req.issueType(), userId, rec));
    }

    /**
     * POST /api/agent/analyze/stream — 상세페이지 AI 분석 (KAN-44). SSE로 도구 실행 과정을 실시간 전송.
     * tool_start/observation 이벤트 → 마지막에 result(분석 결과) → done.
     */
    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStream(
            @AuthenticationPrincipal String userId,
            @RequestBody AnalyzeRequest req) {
        AgentService.RecommendationInput rec = new AgentService.RecommendationInput(
                req.similarityScore(), req.matchedKeywords(), req.matchedCategories());
        return chatAgentService.analyzeStream(req.issueId(), req.issueType(), userId, rec);
    }

    /**
     * POST /api/agent/chat — 챗봇 Q&A (KAN-41). 자유 질문 → 검색 + 답변 생성.
     * 상세페이지에서 질문 시 issueId/issueType을 함께 전달하면 해당 이슈가 컨텍스트에 포함된다.
     * history를 전달하면 이전 대화를 참고해 멀티턴으로 답한다.
     */
    @PostMapping("/chat")
    public ApiResponse<AgentChatService.ChatResponse> chat(
            @AuthenticationPrincipal String userId,
            @RequestBody ChatRequest req) {
        return ApiResponse.success(
                agentChatService.chat(req.question(), req.issueId(), req.issueType(), userId, req.history()));
    }

    /**
     * POST /api/agent/chat/stream — 챗봇 자율 에이전트 (KAN-44). SSE로 도구 사용 과정을 실시간 전송.
     * EXAONE이 스스로 도구를 골라 멀티스텝으로 돌고, thought/tool_start/observation/answer/sources/done 이벤트를 흘려보낸다.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @AuthenticationPrincipal String userId,
            @RequestBody ChatRequest req) {
        List<ChatAgentService.ChatTurn> history = req.history() == null ? List.of()
                : req.history().stream()
                    .map(t -> new ChatAgentService.ChatTurn(t.role(), t.content()))
                    .toList();
        return chatAgentService.chatStream(req.question(), req.issueId(), req.issueType(), userId, history);
    }

    record ChatRequest(
            String question,
            // 현재 보고 있는 이슈 (optional — 상세페이지에서 질문 시만 전달)
            String issueId,
            String issueType,
            // 이전 대화 (optional — 멀티턴 연속성)
            List<AgentChatService.ChatTurn> history
    ) {}

    record AnalyzeRequest(
            String issueId,
            String issueType,
            // 추천 근거 (optional — 피드 진입 시만 전달)
            Double similarityScore,
            List<String> matchedKeywords,
            List<String> matchedCategories
    ) {}
}
