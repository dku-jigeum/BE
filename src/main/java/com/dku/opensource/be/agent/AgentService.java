package com.dku.opensource.be.agent;

import com.dku.opensource.be.agent.react.ReActLoop;
import com.dku.opensource.be.agent.react.ReActResult;
import com.dku.opensource.be.agent.tool.AgentContext;
import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import com.dku.opensource.be.domain.user.UserProfile;
import com.dku.opensource.be.domain.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    // extract_key_dates → decide_calendar_registration 순서 보장 포함
    private static final List<String> CANONICAL_ORDER = List.of(
            "summarize_event",
            "explain_recommendation_reason",
            "analyze_user_impact",
            "extract_key_dates",
            "decide_calendar_registration",
            "recommend_user_action",
            "ask_missing_profile_question"
    );

    private final ReActLoop reActLoop;
    private final AgentPlannerService agentPlannerService;
    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;
    private final LegislationNoticeRepository legislationNoticeRepository;
    private final UserProfileRepository userProfileRepository;

    // ─── 요청 ───────────────────────────────────────────────

    public record RecommendationInput(
            Double similarityScore,
            List<String> matchedKeywords,
            List<String> matchedCategories
    ) {}

    // ─── 응답 카드 DTO ───────────────────────────────────────

    public record DetailPageAnalysisResponse(
            String eventId,
            String userId,
            AnalysisCards analysis
    ) {}

    public record AnalysisCards(
            AgentHeaderCard agentHeader,
            SummaryCard summary,
            RecommendationReasonCard recommendationReason,
            UserImpactCard userImpact,
            KeyDatesCard keyDates,
            RecommendedActionsCard recommendedActions,
            MissingProfileQuestionCard missingProfileQuestion
    ) {}

    public record AgentHeaderCard(String title, String description) {}

    public record SummaryCard(String title, List<String> items) {}

    public record RecommendationReasonCard(String title, String description, List<String> tags) {}

    public record UserImpactCard(
            String title, String impactLevel, String impactType,
            String description, List<String> effects, String uncertainty
    ) {}

    public record KeyDatesCard(
            String title, List<KeyDateItem> dates, CalendarSuggestion calendarSuggestion
    ) {}

    public record KeyDateItem(String type, String label, String date, String dDay) {}

    public record CalendarSuggestion(
            boolean shouldSuggest, String reason,
            String calendarTitle, String calendarDate, String reminder
    ) {}

    public record RecommendedActionsCard(String title, List<ActionItem> actions) {}

    public record ActionItem(String type, String label, String reason, String priority) {}

    public record MissingProfileQuestionCard(
            boolean needInfo, String question, List<String> options, String reason
    ) {}

    // 하위 호환 — 기존 /api/agent/analyze 응답 유지
    public record AnalyzeResponse(
            List<String> summary,
            String impact,
            List<SimilarIssueDto> similarIssues,
            boolean calendarSuggested,
            Integer daysLeft
    ) {}

    public record SimilarIssueDto(String id, String type, String title, String dDay, String reason) {}

    // ─── 메인 엔트리 ─────────────────────────────────────────

    public DetailPageAnalysisResponse analyze(
            String issueId, String issueType, String userId,
            RecommendationInput rec) {

        AgentContext ctx = buildContext(issueId, issueType, userId, rec);

        log.info("[Agent] 플래닝 시작 — issueId={}, type={}, user={}", issueId, issueType, userId);
        AgentPlannerService.PlannerResult plan = agentPlannerService.plan(ctx);

        List<String> orderedTools = sortByCanonicalOrder(plan.selectedTools());
        log.info("[Agent] 분석 시작 — orderedTools={}", orderedTools);
        ReActResult result = reActLoop.runWithPlan(orderedTools, ctx);
        log.info("[Agent] 분석 완료 — calendarSuggested={}", result.isCalendarSuggested());

        return buildDetailResponse(result, ctx);
    }

    // ─── Context 빌드 ────────────────────────────────────────

    public AgentContext buildContext(String issueId, String issueType, String userId,
                                     RecommendationInput rec) {
        if (issueType == null || issueType.isBlank()) {
            throw new IllegalArgumentException("issueType은 필수입니다.");
        }
        // ── 이벤트 로드 ──────────────────────────────────────
        AgentContext.AgentContextBuilder builder = switch (issueType.toLowerCase()) {
            case "bill" -> {
                Bill bill = billRepository.findByBillNo(issueId)
                        .orElseThrow(() -> new IllegalArgumentException("법안 없음: " + issueId));
                String deadline = bill.getDeadline() != null ? bill.getDeadline().toString() : null;
                yield AgentContext.builder()
                        .issueId(issueId).issueType("bill").userId(userId)
                        .issueTitle(bill.getTitle())
                        .issueContent(bill.getContent())
                        .deadline(deadline)
                        .issueSummary(truncate(bill.getContent(), bill.getTitle()))
                        .deadlineType(resolveDeadlineType(deadline));
            }
            case "petition" -> {
                Petition petition = petitionRepository.findByPetitionNo(issueId)
                        .orElseThrow(() -> new IllegalArgumentException("청원 없음: " + issueId));
                String deadline = petition.getDeadline() != null ? petition.getDeadline().toString() : null;
                yield AgentContext.builder()
                        .issueId(issueId).issueType("petition").userId(userId)
                        .issueTitle(petition.getTitle())
                        .issueContent(petition.getContent())
                        .deadline(deadline)
                        .issueSummary(truncate(petition.getContent(), petition.getTitle()))
                        .deadlineType(resolveDeadlineType(deadline));
            }
            case "legislation" -> {
                LegislationNotice notice = legislationNoticeRepository.findByBillId(issueId)
                        .orElseThrow(() -> new IllegalArgumentException("입법예고 없음: " + issueId));
                String deadline = notice.getDeadline() != null ? notice.getDeadline().toString() : null;
                yield AgentContext.builder()
                        .issueId(issueId).issueType("legislation").userId(userId)
                        .issueTitle(notice.getTitle())
                        .issueContent(null)
                        .deadline(deadline)
                        .issueSummary(notice.getTitle())
                        .deadlineType(resolveDeadlineType(deadline));
            }
            default -> throw new IllegalArgumentException("알 수 없는 issueType: " + issueType);
        };

        // ── 사용자 프로필 로드 + 상태 계산 ───────────────────
        userProfileRepository.findByUserId(userId).ifPresentOrElse(
            p -> {
                builder.userAge(p.getAge()).userOccupation(p.getOccupation());
                p.getInterestTags().forEach(builder::userInterest);
                applyProfileStatus(builder, p.getAge(), p.getOccupation(), p.getInterestTags());
            },
            () -> applyProfileStatus(builder, null, null, List.of())
        );

        // ── 추천 근거 ────────────────────────────────────────
        if (rec != null) {
            if (rec.similarityScore() != null) builder.similarityScore(rec.similarityScore());
            if (rec.matchedKeywords() != null) rec.matchedKeywords().forEach(builder::matchedKeyword);
            if (rec.matchedCategories() != null) rec.matchedCategories().forEach(builder::matchedCategory);
        }

        return builder.build();
    }

    /** 원문 앞 500자 truncate. 원문이 없으면 title 사용. */
    private String truncate(String content, String title) {
        if (content != null && !content.isBlank()) {
            return content.length() > 500 ? content.substring(0, 500) + "..." : content;
        }
        return title != null ? title : "";
    }

    /** deadline → deadlineType */
    private String resolveDeadlineType(String deadline) {
        if (deadline == null || deadline.isBlank()) return "always_open";
        try {
            java.time.LocalDate.parse(deadline);
            return "deadline";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 사용자 프로필 필드를 분석해 missingFields / availableFields / profileQuality를 계산한다.
     * 이 계산은 코드에서 수행 — Planner가 서비스 정책을 재판단하지 않도록 한다.
     */
    private void applyProfileStatus(AgentContext.AgentContextBuilder builder,
                                     Integer age, String occupation,
                                     java.util.List<String> interests) {
        if (age != null)                             builder.availableField("age");
        else                                         builder.missingField("age");

        if (occupation != null && !occupation.isBlank()) builder.availableField("occupation");
        else                                              builder.missingField("occupation");

        if (!interests.isEmpty())                    builder.availableField("interests");
        else                                         builder.missingField("interests");

        int available = (age != null ? 1 : 0)
                + (occupation != null && !occupation.isBlank() ? 1 : 0)
                + (!interests.isEmpty() ? 1 : 0);

        String quality = available >= 3 ? "high" : available >= 1 ? "medium" : "low";
        builder.profileQuality(quality);
    }

    private List<String> sortByCanonicalOrder(List<String> tools) {
        return tools.stream()
                .sorted(Comparator.comparingInt(t -> {
                    int idx = CANONICAL_ORDER.indexOf(t);
                    return idx < 0 ? CANONICAL_ORDER.size() : idx;
                }))
                .collect(Collectors.toList());
    }

    // ─── 응답 빌드 ────────────────────────────────────────────

    private DetailPageAnalysisResponse buildDetailResponse(ReActResult result, AgentContext ctx) {
        AnalysisCards cards = new AnalysisCards(
                buildAgentHeader(),
                buildSummaryCard(result),
                buildRecommendationReasonCard(result, ctx),
                buildUserImpactCard(result),
                buildKeyDatesCard(result, ctx),
                buildRecommendedActionsCard(result),
                buildMissingProfileCard(result)
        );
        return new DetailPageAnalysisResponse(ctx.getIssueId(), ctx.getUserId(), cards);
    }

    private AgentHeaderCard buildAgentHeader() {
        return new AgentHeaderCard(
                "AI 맞춤 분석",
                "회원님의 관심사와 이슈 내용을 바탕으로 분석했어요."
        );
    }

    private SummaryCard buildSummaryCard(ReActResult result) {
        List<String> lines = result.getSummary() != null
                ? Arrays.stream(result.getSummary().split("\n"))
                        .map(String::trim).filter(s -> !s.isBlank()).limit(3)
                        .collect(Collectors.toList())
                : List.of("요약 생성에 실패했습니다.");
        return new SummaryCard("AI 한눈에 요약", lines);
    }

    private RecommendationReasonCard buildRecommendationReasonCard(ReActResult result, AgentContext ctx) {
        String raw = result.getRecommendationReasonRaw();
        String description = "회원님의 관심사와 이 이슈의 키워드가 연결되어 추천되었어요.";
        List<String> tags = new ArrayList<>(ctx.getUserInterests());

        if (raw != null && !raw.isBlank()) {
            description = extractLine(raw, "REASON:").orElse(description);
            String matchedUser = extractLine(raw, "MATCHED_USER:").orElse("");
            String matchedEvent = extractLine(raw, "MATCHED_EVENT:").orElse("");
            tags = mergeTags(matchedUser, matchedEvent);
        }
        return new RecommendationReasonCard("왜 추천됐나요?", description, tags);
    }

    private UserImpactCard buildUserImpactCard(ReActResult result) {
        String raw = result.getImpactStructuredRaw();
        if (raw == null || raw.isBlank()) {
            return new UserImpactCard("나에게 미치는 영향", "unknown", "unknown",
                    "영향 분석 정보가 없습니다.", List.of(), "추가 정보가 필요합니다.");
        }
        String level = extractLine(raw, "IMPACT_LEVEL:").orElse("unknown");
        String type = extractLine(raw, "IMPACT_TYPE:").orElse("unknown");
        String summary = extractLine(raw, "SUMMARY:").orElse(result.getImpact() != null ? result.getImpact() : "");
        List<String> effects = extractLine(raw, "EFFECTS:")
                .map(e -> Arrays.stream(e.split("##"))
                        .map(String::trim).filter(s -> !s.isBlank())
                        .collect(Collectors.toList()))
                .orElse(List.of());
        String uncertainty = extractLine(raw, "UNCERTAINTY:").orElse("");
        return new UserImpactCard("나에게 미치는 영향", level, type, summary, effects, uncertainty);
    }

    private KeyDatesCard buildKeyDatesCard(ReActResult result, AgentContext ctx) {
        List<KeyDateItem> dates = new ArrayList<>();
        String keyDatesRaw = result.getKeyDatesRaw();

        if (keyDatesRaw != null && !keyDatesRaw.isBlank()) {
            for (String line : keyDatesRaw.split("\n")) {
                String type = extractPipe(line, "DATE_TYPE:").orElse("unknown");
                String label = extractPipe(line, "DATE_LABEL:").orElse("마감");
                String value = extractPipe(line, "DATE_VALUE:").orElse("미상");
                String dDay = extractPipe(line, "DDAY:").orElse("미상");
                dates.add(new KeyDateItem(type, label, value, dDay));
            }
        }

        CalendarSuggestion suggestion = buildCalendarSuggestion(result, ctx);
        return new KeyDatesCard("놓치면 안 되는 일정", dates.isEmpty() ? List.of(new KeyDateItem("always_open", "참여 마감", "상시", "상시")) : dates, suggestion);
    }

    private CalendarSuggestion buildCalendarSuggestion(ReActResult result, AgentContext ctx) {
        String raw = result.getCalendarDecisionRaw();
        if (raw == null || raw.isBlank()) {
            return new CalendarSuggestion(false, "마감일 정보 없음", "", "", "");
        }
        boolean suggest = extractLine(raw, "SUGGEST:").map("true"::equals).orElse(false);
        String reason = extractLine(raw, "REASON:").orElse("");
        String title = extractLine(raw, "TITLE:").orElse("");
        String date = extractLine(raw, "DATE:").orElse("");
        String reminder = extractLine(raw, "REMINDER:").orElse("3일 전");
        return new CalendarSuggestion(suggest, reason, title, date, reminder);
    }

    private RecommendedActionsCard buildRecommendedActionsCard(ReActResult result) {
        String raw = result.getRecommendedActionsRaw();
        List<ActionItem> actions = new ArrayList<>();

        if (raw != null && !raw.isBlank()) {
            for (String line : raw.split("\n")) {
                if (!line.contains("ACTION:")) continue;
                String type = extractPipe(line, "ACTION:").orElse("view_original");
                String label = extractPipe(line, "LABEL:").orElse("자세히 보기");
                String reason = extractPipe(line, "REASON:").orElse("");
                String priority = extractPipe(line, "PRIORITY:").orElse("medium");
                actions.add(new ActionItem(type, label, reason, priority));
            }
        }

        if (actions.isEmpty()) {
            actions = List.of(new ActionItem("view_original", "원문 보기", "세부 내용을 직접 확인해보세요.", "high"));
        }
        return new RecommendedActionsCard("다음 행동 추천", actions);
    }

    private MissingProfileQuestionCard buildMissingProfileCard(ReActResult result) {
        String raw = result.getMissingProfileQuestionRaw();
        if (raw == null || !raw.contains("NEED_INFO:true")) {
            return new MissingProfileQuestionCard(false, "", List.of(), "");
        }
        String question = extractLine(raw, "QUESTION:").orElse("");
        String optionsStr = extractLine(raw, "OPTIONS:").orElse("");
        List<String> options = optionsStr.isBlank() ? List.of()
                : Arrays.asList(optionsStr.split("\\|"));
        String reason = extractLine(raw, "REASON:").orElse("");
        return new MissingProfileQuestionCard(true, question, options, reason);
    }

    // ─── 파싱 유틸 ────────────────────────────────────────────

    private java.util.Optional<String> extractLine(String text, String prefix) {
        if (text == null) return java.util.Optional.empty();
        return Arrays.stream(text.split("\n"))
                .filter(l -> l.startsWith(prefix))
                .map(l -> l.substring(prefix.length()).trim())
                .filter(s -> !s.isBlank())
                .findFirst();
    }

    /** 파이프(|) 구분 포맷에서 값 추출: "ACTION:type|LABEL:텍스트|..." */
    private java.util.Optional<String> extractPipe(String line, String prefix) {
        if (line == null) return java.util.Optional.empty();
        return Arrays.stream(line.split("\\|"))
                .filter(seg -> seg.startsWith(prefix))
                .map(seg -> seg.substring(prefix.length()).trim())
                .filter(s -> !s.isBlank())
                .findFirst();
    }

    private List<String> mergeTags(String... csv) {
        List<String> tags = new ArrayList<>();
        for (String s : csv) {
            if (s != null && !s.isBlank()) {
                Arrays.stream(s.split(",")).map(String::trim)
                        .filter(t -> !t.isBlank()).forEach(tags::add);
            }
        }
        return tags.stream().distinct().collect(Collectors.toList());
    }

    // ─── 유사 이슈 파싱 (SimilarEventsController용) ──────────

    public List<SimilarIssueDto> parseSimilarIssues(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("없음")) return Collections.emptyList();
        List<SimilarIssueDto> results = new ArrayList<>();
        for (String line : raw.split("\n")) {
            if (!line.contains("SIMILAR:")) continue;
            String type = extractPipe(line, "SIMILAR:").map(t -> t.replaceAll("[\\[\\]]", "")).orElse("bill");
            String id = extractPipe(line, "ID:").orElse("");
            String title = extractPipe(line, "TITLE:").orElse("");
            String dDay = extractPipe(line, "DDAY:").orElse("미상");
            String reason = extractPipe(line, "REASON:").orElse("");
            results.add(new SimilarIssueDto(id, type, title, dDay, reason));
        }
        return results.stream().limit(3).collect(Collectors.toList());
    }
}
