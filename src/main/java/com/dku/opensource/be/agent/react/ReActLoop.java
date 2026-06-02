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
            case "summarize_tool" -> result.setSummary(observation);
            case "impact_analysis_tool" -> result.setImpact(observation);
            case "similar_issues_tool" -> result.setSimilarIssuesRaw(observation);
            case "calendar_tool" -> {
                result.setCalendarSuggested(observation.contains("CALENDAR_SUGGEST:true"));
                // D-day 숫자 추출
                if (observation.contains("D-")) {
                    try {
                        String dPart = observation.substring(observation.indexOf("D-") + 2);
                        result.setDaysLeft(Integer.parseInt(dPart.split("[^0-9]")[0]));
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private String buildSystemPrompt() {
        StringBuilder tools = new StringBuilder();
        toolMap.values().forEach(t ->
                tools.append("- ").append(t.name()).append(": ").append(t.description()).append("\n"));

        return """
                당신은 법안·청원·입법예고를 분석하는 AI 에이전트입니다.
                주어진 Goal을 달성하기 위해 순서대로 Tool을 사용하세요.

                사용 가능한 Tool:
                """ + tools + """

                반드시 다음 형식으로 응답하세요:
                Thought: (다음에 무엇을 할지 생각)
                Action: (tool 이름)
                Action Input: (tool에 전달할 입력)

                Tool 결과(Observation)를 받은 후 다음 Action을 결정하세요.
                모든 Tool 실행이 완료되면:
                Final Answer: (최종 분석 완료 메시지)

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
