package com.dku.opensource.be.agent.react;

import com.dku.opensource.be.agent.model.ExaoneClient;
import com.dku.opensource.be.agent.tool.AgentContext;
import com.dku.opensource.be.agent.tool.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ReAct 패턴 구현: Thought → Action → Observation 루프.
 *
 * 모델이 다음 형식으로 응답하면 파싱해서 Tool을 호출한다:
 *   Thought: ...
 *   Action: tool_name
 *   Action Input: ...
 *
 * "Final Answer:" 를 포함한 응답이 오면 루프를 종료한다.
 */
@Slf4j
@Component
public class ReActLoop {

    private final ExaoneClient exaoneClient;
    private final Map<String, AgentTool> toolMap;

    @Value("${agent.react.max-iterations:6}")
    private int maxIterations;

    public ReActLoop(ExaoneClient exaoneClient, List<AgentTool> tools) {
        this.exaoneClient = exaoneClient;
        this.toolMap = tools.stream().collect(Collectors.toMap(AgentTool::name, Function.identity()));
    }

    public ReActResult run(String goal, AgentContext ctx) {
        String systemPrompt = buildSystemPrompt();
        StringBuilder history = new StringBuilder();
        history.append("Goal: ").append(goal).append("\n\n");

        ReActResult result = new ReActResult();

        for (int i = 0; i < maxIterations; i++) {
            String response = exaoneClient.complete(systemPrompt, history.toString());
            log.debug("[ReAct iter {}] response:\n{}", i, response);

            // Action이 Final Answer보다 먼저 나오면 tool 먼저 처리
            boolean hasAction = response.contains("Action:");
            boolean hasFinal = response.contains("Final Answer:");
            int actionIdx = hasAction ? response.indexOf("Action:") : Integer.MAX_VALUE;
            int finalIdx = hasFinal ? response.indexOf("Final Answer:") : Integer.MAX_VALUE;

            if (hasFinal && finalIdx < actionIdx) {
                String answer = extractAfter(response, "Final Answer:");
                result.setFinalAnswer(answer.trim());
                break;
            }

            if (!hasAction) {
                log.warn("[ReAct] Action 없음 — 루프 종료");
                if (hasFinal) result.setFinalAnswer(extractAfter(response, "Final Answer:").trim());
                else result.setFinalAnswer(response.trim());
                break;
            }

            String toolName = extractAfter(response, "Action:").split("\n")[0].trim();
            String toolInput = response.contains("Action Input:")
                    ? extractAfter(response, "Action Input:").split("\n")[0].trim()
                    : "";

            // history에는 Thought + Action + Action Input 까지만 추가 (가짜 Observation 제외)
            String historyEntry = extractUpToActionInput(response, toolInput);
            history.append(historyEntry).append("\n");

            AgentTool tool = toolMap.get(toolName);
            String observation;
            if (tool == null) {
                observation = "Error: 알 수 없는 tool — " + toolName;
            } else {
                try {
                    observation = tool.run(toolInput, ctx);
                    // 결과 수집
                    collectToolResult(result, toolName, observation, ctx);
                } catch (Exception e) {
                    observation = "Error: " + e.getMessage();
                }
            }

            log.debug("[ReAct iter {}] tool={}, observation={}", i, toolName, observation);
            history.append("Observation: ").append(observation).append("\n\n");
        }

        if (result.getFinalAnswer() == null) {
            result.setFinalAnswer("분석 완료.");
        }
        return result;
    }

    private void collectToolResult(ReActResult result, String toolName, String observation, AgentContext ctx) {
        switch (toolName) {
            case "summarize_event"               -> result.setSummary(observation);
            case "analyze_user_impact"           -> {
                result.setImpact(observation);
                result.setImpactStructuredRaw(observation);
            }
            case "find_similar_events"           -> result.setSimilarIssuesRaw(observation);
            case "explain_recommendation_reason" -> result.setRecommendationReasonRaw(observation);
            case "extract_key_dates"             -> {
                result.setKeyDatesRaw(observation);
                if (observation.contains("D-")) {
                    try {
                        String dPart = observation.substring(observation.indexOf("D-") + 2);
                        result.setDaysLeft(Integer.parseInt(dPart.split("[^0-9]")[0]));
                    } catch (Exception ignored) {}
                }
            }
            case "decide_calendar_registration"  -> {
                result.setCalendarDecisionRaw(observation);
                result.setCalendarSuggested(observation.contains("SUGGEST:true"));
            }
            case "recommend_user_action"         -> result.setRecommendedActionsRaw(observation);
            case "ask_missing_profile_question"  -> result.setMissingProfileQuestionRaw(observation);
            case "compare_with_similar_events"   -> result.setSimilarIssuesRaw(
                    result.getSimilarIssuesRaw() != null
                            ? result.getSimilarIssuesRaw() + "\n" + observation
                            : observation);
        }
    }

    private String buildSystemPrompt() {
        StringBuilder tools = new StringBuilder();
        toolMap.values().forEach(t ->
                tools.append("- ").append(t.name()).append(": ").append(t.description()).append("\n"));

        return """
                당신은 개인 맞춤형 정책 상세페이지 분석 AI 에이전트입니다.
                주어진 Goal에 명시된 순서대로 Tool을 사용해 분석을 완료하세요.

                규칙:
                - register_calendar_event 와 generate_opinion_draft_tool 은 사용자 승인 전에 실행하지 마세요.
                - 사용자 프로필이 부족한 경우 ask_missing_profile_tool 을 선택적으로 실행하세요.
                - 법률 결론을 단정하지 마세요.

                사용 가능한 Tool:
                """ + tools + """

                반드시 다음 형식으로 응답하세요:
                Thought: (다음에 무엇을 할지 생각)
                Action: (tool 이름)
                Action Input: (tool에 전달할 입력)

                Tool 결과(Observation)를 받은 후 다음 Action을 결정하세요.
                모든 Tool 실행이 완료되면:
                Final Answer: 분석 완료

                한국어로 응답하세요.
                """;
    }

    private String extractAfter(String text, String marker) {
        int idx = text.indexOf(marker);
        if (idx < 0) return "";
        return text.substring(idx + marker.length()).trim();
    }

    // Thought + Action + Action Input 까지만 추출 (모델이 뒤에 붙인 가짜 Observation 제거)
    private String extractUpToActionInput(String response, String toolInput) {
        if (!response.contains("Action Input:")) return response;
        int inputIdx = response.indexOf("Action Input:");
        int endIdx = inputIdx + "Action Input:".length() + toolInput.length() + 5;
        // 줄바꿈 기준으로 Action Input 줄 끝까지만
        int newlineIdx = response.indexOf("\n", inputIdx);
        return response.substring(0, newlineIdx > 0 ? newlineIdx : Math.min(endIdx, response.length()));
    }
}
