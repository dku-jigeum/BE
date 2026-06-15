package com.dku.opensource.be.agent.react;

import com.dku.opensource.be.agent.model.ExaoneClient;
import com.dku.opensource.be.agent.tool.AgentContext;
import com.dku.opensource.be.agent.tool.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
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

    /** 챗봇이 사용할 수 있는 도구 화이트리스트 (분석용 도구는 제외). */
    private static final List<String> CHAT_TOOLS = List.of(
            "search_issues", "get_issue_detail", "find_similar_events",
            "compare_with_similar_events", "register_calendar_event", "remove_calendar_event",
            "add_bookmark", "remove_bookmark", "recommend_for_me", "get_my_activity");

    @Value("${agent.react.max-iterations:6}")
    private int maxIterations;

    public ReActLoop(ExaoneClient exaoneClient, List<AgentTool> tools) {
        this.exaoneClient = exaoneClient;
        this.toolMap = tools.stream().collect(Collectors.toMap(AgentTool::name, Function.identity()));
    }

    /**
     * Planner selectedTools를 canonical order로 순차 실행한다.
     * 중복 Tool은 한 번만 실행하고, 알 수 없는 Tool은 건너뛴다.
     */
    public ReActResult runWithPlan(List<String> selectedTools, AgentContext ctx) {
        return runWithPlan(selectedTools, ctx, e -> {});
    }

    /**
     * Planner selectedTools를 canonical order로 순차 실행하며, 각 도구 실행을 listener로 흘려보낸다(SSE용).
     */
    public ReActResult runWithPlan(List<String> selectedTools, AgentContext ctx, Consumer<AgentEvent> listener) {
        ReActResult result = new ReActResult();
        Set<String> executed = new LinkedHashSet<>();

        for (String toolName : selectedTools) {
            if (executed.contains(toolName)) {
                log.warn("[runWithPlan] 중복 Tool 스킵 — {}", toolName);
                continue;
            }
            if (toolName.equals("decide_calendar_registration") && !result.hasCalendarCandidate()) {
                log.debug("[runWithPlan] 캘린더 후보 없음 — decide_calendar_registration skip");
                continue;
            }
            AgentTool tool = toolMap.get(toolName);
            if (tool == null) {
                log.warn("[runWithPlan] 알 수 없는 Tool 스킵 — {}", toolName);
                continue;
            }
            try {
                listener.accept(AgentEvent.toolStart(toolName, ""));
                String observation = tool.run("", ctx);
                collectToolResult(result, toolName, observation, ctx);
                listener.accept(AgentEvent.observation(toolName, summarize(observation)));
                log.debug("[runWithPlan] tool={} 완료", toolName);
            } catch (Exception e) {
                log.error("[runWithPlan] Tool 실행 실패 — tool={}, error={}", toolName, e.getMessage());
            }
            executed.add(toolName);
        }

        result.setFinalAnswer("분석 완료.");
        return result;
    }

    public ReActResult run(String goal, AgentContext ctx) {
        return run(goal, ctx, maxIterations);
    }

    public ReActResult run(String goal, AgentContext ctx, int iterLimit) {
        return runCore(buildSystemPrompt(), "Goal: " + goal + "\n\n", ctx, iterLimit, "분석 완료.", null, e -> {});
    }

    /**
     * 챗봇용 자율 ReAct 루프. EXAONE이 스스로 도구를 고르고 결과를 보고 다음 행동을 정한다.
     * 진행 상황(thought/tool_start/observation/answer)을 listener로 흘려보낸다(SSE용).
     *
     * @param question   사용자 질문
     * @param historyText 이전 대화(포맷된 문자열, 없으면 "(없음)")
     */
    public ReActResult runChat(String question, String historyText, AgentContext ctx,
                               Consumer<AgentEvent> listener) {
        // 상세페이지에서 질문한 경우, 현재 이슈를 프롬프트에 알려 "이 법안/청원" 지시어와 도구 입력(type:id)에 쓰게 한다.
        String currentIssue = (ctx.getIssueId() != null && !ctx.getIssueId().isBlank()
                && ctx.getIssueType() != null && !ctx.getIssueType().isBlank())
                ? "\n\n[내부 참고 — 사용자에게 보이지 마세요]"
                    + "\n현재 보고 있는 이슈 제목: " + (ctx.getIssueTitle() != null ? ctx.getIssueTitle() : "(제목 없음)")
                    + summaryLine(ctx)
                    + "\n도구 입력 전용 식별자: " + ctx.getIssueType() + ":" + ctx.getIssueId()
                    + " (이 식별자는 Action Input에만 쓰고, Final Answer에는 절대 쓰지 마세요)"
                : "";
        String goal = "이전 대화:\n" + (historyText == null || historyText.isBlank() ? "(없음)" : historyText)
                + "\n\n사용자 질문: " + question + currentIssue + "\n\n";
        return runCore(buildChatSystemPrompt(), goal, ctx, maxIterations,
                "죄송해요, 답변을 정리하지 못했어요. 질문을 조금 더 구체적으로 해주시겠어요?",
                new LinkedHashSet<>(CHAT_TOOLS), listener);
    }

    /**
     * ReAct 공통 루프. systemPrompt와 fallback 답변, 허용 도구, 진행 이벤트 listener만 다르게 주입한다.
     *
     * @param allowedTools null이면 모든 도구 허용(분석용), 지정 시 그 집합만 허용(챗봇용)
     */
    private ReActResult runCore(String systemPrompt, String initialHistory, AgentContext ctx,
                                int iterLimit, String fallbackAnswer,
                                Set<String> allowedTools, Consumer<AgentEvent> listener) {
        StringBuilder history = new StringBuilder(initialHistory);
        ReActResult result = new ReActResult();
        Set<String> calledSignatures = new LinkedHashSet<>();

        for (int i = 0; i < iterLimit; i++) {
            String response = exaoneClient.complete(systemPrompt, history.toString());
            log.debug("[ReAct iter {}] response:\n{}", i, response);

            String thought = firstLine(extractAfter(response, "Thought:"));
            if (!thought.isBlank()) listener.accept(AgentEvent.thought(thought));

            // Action이 Final Answer보다 먼저 나오면 tool 먼저 처리
            boolean hasAction = response.contains("Action:");
            boolean hasFinal = response.contains("Final Answer:");
            int actionIdx = hasAction ? response.indexOf("Action:") : Integer.MAX_VALUE;
            int finalIdx = hasFinal ? response.indexOf("Final Answer:") : Integer.MAX_VALUE;

            if (hasFinal && finalIdx < actionIdx) {
                String answer = sanitizeAnswer(extractAfter(response, "Final Answer:"));
                result.setFinalAnswer(answer);
                listener.accept(AgentEvent.answer(answer));
                return result;
            }

            if (!hasAction) {
                // 형식 이탈/도구 불필요 — 응답 자체를 답변으로 사용 (인사·잡담 즉답 포함)
                log.warn("[ReAct] Action 없음 — 응답을 Final Answer로 사용");
                String answer = sanitizeAnswer(hasFinal ? extractAfter(response, "Final Answer:") : response);
                result.setFinalAnswer(answer);
                listener.accept(AgentEvent.answer(answer));
                return result;
            }

            String toolName = Arrays.stream(extractAfter(response, "Action:").split("\n"))
                    .map(String::trim).filter(s -> !s.isBlank()).findFirst().orElse("");
            String toolInput = response.contains("Action Input:")
                    ? extractAfter(response, "Action Input:").split("\n")[0].trim()
                    : "";

            // history에는 Thought + Action + Action Input 까지만 추가 (가짜 Observation 제외)
            history.append(extractUpToActionInput(response)).append("\n");

            String observation;
            if (!calledSignatures.add(toolName + "|" + toolInput)) {
                // 같은 도구·같은 입력 반복 — 무한루프 차단
                observation = "이미 같은 도구를 같은 입력으로 호출했습니다. 다른 행동을 하거나 Final Answer를 작성하세요.";
            } else if (allowedTools != null && !allowedTools.contains(toolName)) {
                // 챗봇 화이트리스트 밖 도구 차단
                observation = "Error: '" + toolName + "'는 사용할 수 없는 도구입니다. 안내된 도구만 사용하거나 Final Answer를 작성하세요.";
            } else {
                listener.accept(AgentEvent.toolStart(toolName, toolInput));
                AgentTool tool = toolMap.get(toolName);
                if (tool == null) {
                    observation = "Error: 알 수 없는 tool — " + toolName;
                } else {
                    try {
                        observation = tool.run(toolInput, ctx);
                        collectToolResult(result, toolName, observation, ctx);
                    } catch (Exception e) {
                        observation = "Error: " + e.getMessage();
                    }
                }
                listener.accept(AgentEvent.observation(toolName, summarize(observation)));
            }

            log.debug("[ReAct iter {}] tool={}, observation={}", i, toolName, observation);
            history.append("Observation: ").append(observation).append("\n\n");
        }

        // iteration cap 도달 — fallback
        if (result.getFinalAnswer() == null) {
            result.setFinalAnswer(fallbackAnswer);
            listener.accept(AgentEvent.answer(fallbackAnswer));
        }
        return result;
    }

    /** Observation을 SSE 표시용으로 1~2줄/120자 이내로 요약한다. */
    private String summarize(String observation) {
        if (observation == null) return "";
        String compact = observation.strip().replaceAll("\\s*\\n\\s*", " ");
        return compact.length() > 120 ? compact.substring(0, 120) + "…" : compact;
    }

    private String firstLine(String text) {
        if (text == null) return "";
        return Arrays.stream(text.split("\n")).map(String::trim)
                .filter(s -> !s.isBlank()).findFirst().orElse("");
    }

    /**
     * 최종 답변 방어 필터 — 텍스트 ReAct가 가끔 답변에 흘리는 내부 스캐폴딩을 걷어낸다.
     * (프롬프트 규칙과 이중 방어: ReAct 제어 토큰 라인 제거 + 누출된 type:id 식별자 제거)
     */
    private String sanitizeAnswer(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            String t = line.strip();
            if (t.startsWith("Thought:") || t.startsWith("Action Input:")
                    || t.startsWith("Action:") || t.startsWith("Observation:")) continue;
            sb.append(line).append("\n");
        }
        String out = sb.toString().strip();
        out = out.replaceFirst("^Final Answer:\\s*", "");
        // 누출된 내부 식별자(bill:123, petition:ABC…) 제거
        out = out.replaceAll("\\b(bill|petition|legislation):[A-Za-z0-9_]+\\b", "");
        // 누출된 도구 이름·명령어 형식 제거 (도구명 + 바로 뒤 괄호만; 예: "add_bookmark('type:id')")
        out = out.replaceAll("\\b(" + String.join("|", CHAT_TOOLS) + ")\\b(\\s*\\([^)\\n]*\\))?", "");
        out = out.replaceAll("'?type:id'?", "");
        // 정리: 빈 괄호·따옴표·중복 공백·공백 앞 구두점
        out = out.replaceAll("\\(\\s*\\)", "").replaceAll("[ \\t]{2,}", " ")
                 .replaceAll(" +([,.!?])", "$1").strip();
        return out;
    }

    /** 현재 이슈 본문 요약을 프롬프트에 붙인다(있으면). 도구 호출 없이도 설명 가능하게. */
    private String summaryLine(AgentContext ctx) {
        String body = ctx.getIssueSummary() != null && !ctx.getIssueSummary().isBlank()
                ? ctx.getIssueSummary() : ctx.getIssueContent();
        if (body == null || body.isBlank()) return "";
        String compact = body.strip().replaceAll("\\s*\\n\\s*", " ");
        if (compact.length() > 400) compact = compact.substring(0, 400) + "…";
        return "\n현재 이슈 본문 요약: " + compact;
    }

    private void collectToolResult(ReActResult result, String toolName, String observation, AgentContext ctx) {
        switch (toolName) {
            case "search_issues", "recommend_for_me" -> result.setSearchIssuesRaw(
                    result.getSearchIssuesRaw() != null
                            ? result.getSearchIssuesRaw() + "\n" + observation
                            : observation);
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
                result.setCalendarSuggested(Arrays.stream(observation.split("\n"))
                        .filter(l -> l.startsWith("SUGGEST:"))
                        .map(l -> l.substring("SUGGEST:".length()).trim())
                        .anyMatch("true"::equals));
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

    /**
     * 챗봇용 시스템 프롬프트. 사용자와 대화하며 필요할 때만 도구를 쓰고,
     * 인사·잡담은 도구 없이 바로 Final Answer로 답한다.
     */
    private String buildChatSystemPrompt() {
        StringBuilder tools = new StringBuilder();
        CHAT_TOOLS.forEach(name -> {
            AgentTool t = toolMap.get(name);
            if (t != null) tools.append("- ").append(t.name()).append(": ").append(t.description()).append("\n");
        });

        return """
                당신은 한국 국회 법안·국민동의청원·입법예고를 안내하는 시민 참여 어시스턴트입니다.
                사용자 질문에 맞게 스스로 도구를 골라 사용하고, 결과를 보고 다음 행동을 결정합니다.
                아래 '사용 가능한 Tool'에 있는 도구만 사용할 수 있습니다(목록에 없는 도구 이름은 쓰지 마세요).

                핵심 규칙:
                - 도구가 필요 없는 인사·잡담·사용법 질문에는 도구를 쓰지 말고 곧바로 Final Answer로 친근하게 답하세요.
                - 특정 주제의 법안·청원을 찾아야 하면 search_issues로 검색하고, 본문이 필요하면 get_issue_detail로 조회하세요.
                - 사용자가 일정·마감일을 캘린더에 "등록"해달라고 하면, 되묻지 말고 즉시 register_calendar_event를 실행하세요.
                  입력은 'type:id' 형식이며, 현재 보고 있는 이슈가 주어졌다면 그 type:id를 그대로 쓰세요. 등록 도구가 마감일을 알아서 조회합니다.
                - 사용자가 이슈를 관심 목록에 "담아"달라고 하면 add_bookmark('type:id'), "빼/삭제"라고 하면 remove_bookmark('type:id')를 실행하세요.
                - 사용자가 일정을 캘린더에서 "지워/삭제"해달라고 하면 remove_calendar_event('type:id')를 실행하세요.
                - "나한테 맞는/추천" 이슈를 물으면 recommend_for_me를 실행하세요(입력 없음).
                - "내가 담은 이슈/내 일정" 등 본인 활동을 물으면 get_my_activity를 실행하세요(입력 없음).
                - 제공된 도구 결과(Observation)에 있는 사실만 근거로 답하고, 없는 내용은 지어내지 마세요.
                - 법률적 결론을 단정하지 마세요.
                - **Final Answer에는 내부 식별자(예: petition:4F91..., 'ID:', 'TYPE:')나 도구 이름을 절대 쓰지 마세요. 이슈는 제목으로만 자연스럽게 언급하세요.**
                - Final Answer는 쉽고 친근한 한국어로 3~5문장 이내로 작성하세요. 사용자가 "쉽게/간단히"를 요청하면 더 평이하게 답하세요.

                사용 가능한 Tool:
                """ + tools + """

                반드시 다음 형식 중 하나로만 응답하세요.

                (1) 도구를 쓸 때:
                Thought: (무엇을 할지 한 문장)
                Action: (tool 이름)
                Action Input: (tool 입력)

                (2) 답할 준비가 됐을 때:
                Thought: (한 문장)
                Final Answer: (사용자에게 줄 최종 답변)

                예시 1 — 인사(도구 불필요):
                Thought: 인사이므로 도구가 필요 없다.
                Final Answer: 안녕하세요! 관심 있는 법안·청원·입법예고가 있으면 무엇이든 물어보세요.

                예시 2 — 검색이 필요한 질문:
                Thought: 전세사기 관련 이슈를 찾아야 한다.
                Action: search_issues
                Action Input: 전세사기 특별법

                예시 3 — 캘린더 등록 요청(현재 이슈 petition:ABC123 보는 중):
                Thought: 사용자가 등록을 원하므로 바로 등록한다.
                Action: register_calendar_event
                Action Input: petition:ABC123
                """;
    }

    private String extractAfter(String text, String marker) {
        int idx = text.indexOf(marker);
        if (idx < 0) return "";
        return text.substring(idx + marker.length()).trim();
    }

    // Thought + Action + Action Input 까지만 추출 (모델이 뒤에 붙인 가짜 Observation 제거)
    private String extractUpToActionInput(String response) {
        if (!response.contains("Action Input:")) return response;
        int inputIdx = response.indexOf("Action Input:");
        int newlineIdx = response.indexOf("\n", inputIdx);
        return response.substring(0, newlineIdx > 0 ? newlineIdx : response.length());
    }
}
