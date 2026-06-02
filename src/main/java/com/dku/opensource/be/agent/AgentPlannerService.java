package com.dku.opensource.be.agent;

import com.dku.opensource.be.agent.model.ExaoneClient;
import com.dku.opensource.be.agent.tool.AgentContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent Planner (§9) — ReAct 루프 실행 전, EXAONE이 어떤 Tool을 실행할지 결정한다.
 *
 * 설계 문서 §10.2의 Planner Prompt를 사용하며, JSON 출력을 파싱한다.
 * 파싱 실패 시 컨텍스트 기반 fallback 플랜을 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPlannerService {

    private final ExaoneClient exaoneClient;
    private final ObjectMapper objectMapper;

    // 상세페이지 초기 진입 시 기본 실행 툴 (사용자 클릭 전까지 실행 금지 툴 제외)
    private static final List<String> DEFAULT_AUTO_TOOLS = List.of(
            "summarize_event",
            "explain_recommendation_reason",
            "analyze_user_impact",
            "extract_key_dates",
            "decide_calendar_registration",
            "recommend_user_action"
    );

    public record PlannerResult(
            String relevanceLevel,
            String reason,
            List<String> selectedTools,
            List<SkippedTool> skippedTools,
            double confidence
    ) {}

    public record SkippedTool(String tool, String reason) {}

    public PlannerResult plan(AgentContext ctx) {
        String userPrompt = buildUserPrompt(ctx);
        String systemPrompt = buildSystemPrompt();

        try {
            String raw = exaoneClient.complete(systemPrompt, userPrompt);
            log.debug("[Planner] raw response:\n{}", raw);
            PlannerResult result = parseJson(raw, ctx);
            log.info("[Planner] relevanceLevel={}, selectedTools={}", result.relevanceLevel(), result.selectedTools());
            return result;
        } catch (Exception e) {
            log.warn("[Planner] EXAONE 호출 실패, fallback 플랜 사용: {}", e.getMessage());
            return fallbackPlan(ctx);
        }
    }

    // ─── 프롬프트 빌드 ────────────────────────────────────────

    /**
     * System Prompt — 역할 정의 + 절대 규칙만 포함.
     * 이벤트 데이터, 출력 형식, Tool 목록은 User Prompt에 배치.
     */
    private String buildSystemPrompt() {
        return """
                너는 개인 맞춤형 정책 상세페이지의 Agent Planner다.

                너의 역할은 정책 이벤트와 사용자 정보를 바탕으로
                상세페이지에서 실행할 분석 Tool을 선택하는 것이다.

                절대 규칙:
                - 존재하지 않는 Tool을 만들지 않는다.
                - 법률적 결론을 단정하지 않는다.
                - 사용자 승인 없이 데이터를 변경하는 Tool을 선택하지 않는다.
                - register_calendar_event와 generate_opinion_draft는 초기 상세페이지 진입 시 선택하지 않는다.
                - 응답은 요청된 JSON 형식만 따른다.
                - 한국어로 작성한다.""";
    }

    /**
     * User Prompt — 이번 요청의 목표 + 입력 데이터 + 출력 형식.
     * Tool 선택 규칙과 출력 JSON schema를 함께 배치해 모델이 '이번 답변의 산출물'로 인식하게 한다.
     */
    private String buildUserPrompt(AgentContext ctx) {
        String inputJson = buildInputJson(ctx);

        return """
                아래 입력 데이터를 보고 상세페이지 초기 진입 시 실행할 Tool을 선택하라.

                사용 가능한 Tool:
                1. summarize_event
                2. explain_recommendation_reason
                3. analyze_user_impact
                4. extract_key_dates
                5. decide_calendar_registration
                6. recommend_user_action
                7. find_similar_events
                8. compare_with_similar_events
                9. ask_missing_profile_question

                Tool 선택 규칙:
                - summarize_event는 기본적으로 선택한다.
                - explain_recommendation_reason은 recommendation.similarityScore 또는 matchedKeywords가 있으면 선택한다.
                - analyze_user_impact는 user.interests와 event.summary가 연결되면 선택한다.
                - extract_key_dates는 event.deadlineInfo.type이 "deadline"이거나 type이 "legislation"이면 선택을 고려한다.
                - decide_calendar_registration은 extract_key_dates를 선택한 경우에만 선택한다.
                - recommend_user_action은 기본적으로 선택한다.
                - find_similar_events는 초기 렌더링 비용이 크므로 skippedTools에 넣는다.
                - ask_missing_profile_question은 user.missingFields가 있고 해당 필드가 이번 이벤트 영향 분석에 중요할 때만 선택한다.

                입력 데이터:
                """ + inputJson + """

                반드시 아래 JSON 형식으로만 답변하라.
                JSON 외의 설명 문장은 출력하지 마라.

                {
                  "relevanceLevel": "high | medium | low",
                  "reason": "string",
                  "selectedTools": ["string"],
                  "skippedTools": [
                    {
                      "tool": "string",
                      "reason": "string"
                    }
                  ],
                  "confidence": 0.0
                }""";
    }

    /** 입력 데이터를 JSON 문자열로 직렬화 */
    private String buildInputJson(AgentContext ctx) {
        try {
            var event = new java.util.LinkedHashMap<String, Object>();
            event.put("id", ctx.getIssueId());
            event.put("type", ctx.getIssueType());
            event.put("title", ctx.getIssueTitle());
            event.put("summary", ctx.getIssueSummary());
            event.put("keyPoints", ctx.getKeyPoints());

            var deadlineInfo = new java.util.LinkedHashMap<String, Object>();
            deadlineInfo.put("type", ctx.getDeadlineType() != null ? ctx.getDeadlineType() : "unknown");
            deadlineInfo.put("date", ctx.getDeadline());
            event.put("deadlineInfo", deadlineInfo);

            var user = new java.util.LinkedHashMap<String, Object>();
            user.put("age", ctx.getUserAge());
            user.put("occupation", ctx.getUserOccupation());
            user.put("interests", ctx.getUserInterests());
            user.put("profileKeywords", ctx.getMatchedKeywords().isEmpty()
                    ? ctx.getUserInterests() : ctx.getMatchedKeywords());
            user.put("profileQuality", ctx.getProfileQuality());
            user.put("missingFields", ctx.getMissingFields());
            user.put("availableFields", ctx.getAvailableFields());

            var recommendation = new java.util.LinkedHashMap<String, Object>();
            recommendation.put("similarityScore", ctx.getSimilarityScore());
            recommendation.put("matchedKeywords", ctx.getMatchedKeywords());

            var root = new java.util.LinkedHashMap<String, Object>();
            root.put("event", event);
            root.put("user", user);
            root.put("recommendation", recommendation);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            log.warn("[Planner] 입력 JSON 직렬화 실패: {}", e.getMessage());
            return "{}";
        }
    }

    // ─── JSON 파싱 ────────────────────────────────────────────

    private PlannerResult parseJson(String raw, AgentContext ctx) {
        try {
            // EXAONE이 JSON 블록을 ```json...``` 으로 감쌀 수 있음 — 추출
            String json = extractJsonBlock(raw);
            JsonNode node = objectMapper.readTree(json);

            String relevanceLevel = node.path("relevanceLevel").asText("medium");
            String reason = node.path("reason").asText("");
            double confidence = node.path("confidence").asDouble(0.7);

            List<String> selected = new ArrayList<>();
            node.path("selectedTools").forEach(n -> selected.add(n.asText()));

            List<SkippedTool> skipped = new ArrayList<>();
            node.path("skippedTools").forEach(n ->
                    skipped.add(new SkippedTool(
                            n.path("tool").asText(),
                            n.path("reason").asText())));

            // selectedTools가 비어 있으면 fallback
            if (selected.isEmpty()) return fallbackPlan(ctx);

            return new PlannerResult(relevanceLevel, reason, selected, skipped, confidence);

        } catch (Exception e) {
            log.warn("[Planner] JSON 파싱 실패 ({}), fallback 사용", e.getMessage());
            return fallbackPlan(ctx);
        }
    }

    private String extractJsonBlock(String raw) {
        // ```json ... ``` 또는 { ... } 블록 추출
        Pattern fenced = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```");
        Matcher m = fenced.matcher(raw);
        if (m.find()) return m.group(1);

        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) return raw.substring(start, end + 1);

        return raw;
    }

    // ─── Fallback ─────────────────────────────────────────────

    private PlannerResult fallbackPlan(AgentContext ctx) {
        List<String> tools = new ArrayList<>(DEFAULT_AUTO_TOOLS);

        boolean hasProfile = ctx.getUserOccupation() != null || ctx.getUserAge() != null
                || !ctx.getUserInterests().isEmpty();
        if (!hasProfile) tools.add("ask_missing_profile_question");

        return new PlannerResult(
                "medium",
                "컨텍스트 기반 기본 분석 플랜",
                tools,
                List.of(new SkippedTool("find_similar_events", "사용자 클릭 후 실행"),
                        new SkippedTool("compare_with_similar_events", "사용자 클릭 후 실행"),
                        new SkippedTool("generate_opinion_draft", "사용자 클릭 후 실행")),
                0.5
        );
    }
}
