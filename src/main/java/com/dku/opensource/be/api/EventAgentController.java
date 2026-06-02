package com.dku.opensource.be.api;

import com.dku.opensource.be.agent.AgentService;
import com.dku.opensource.be.agent.tool.*;
import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.domain.calendar.UserCalendarEvent;
import com.dku.opensource.be.domain.calendar.UserCalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 클릭 후 실행되는 이벤트 에이전트 엔드포인트.
 *
 * GET  /api/events/{eventId}/similar          — 유사 이슈 조회 (find_similar_events)
 * POST /api/events/{eventId}/compare          — 유사 이슈 비교 (compare_with_similar_events)
 * POST /api/events/{eventId}/opinion-draft    — 의견 초안 생성 (generate_opinion_draft)
 * POST /api/events/{eventId}/calendar         — 캘린더 등록 (register_calendar_event)
 * GET  /api/events/calendar                   — 내 캘린더 목록
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventAgentController {

    private final AgentService agentService;
    private final SimilarIssuesTool similarIssuesTool;
    private final CompareWithSimilarTool compareWithSimilarTool;
    private final GenerateOpinionDraftTool generateOpinionDraftTool;
    private final DecideCalendarTool decideCalendarTool;
    private final UserCalendarEventRepository calendarEventRepository;

    // ── 유사 이슈 ────────────────────────────────────────────

    @GetMapping("/{eventId}/similar")
    public ApiResponse<List<AgentService.SimilarIssueDto>> getSimilarEvents(
            @PathVariable String eventId,
            @RequestParam String issueType,
            @AuthenticationPrincipal String userId) {

        AgentContext ctx = buildContext(eventId, issueType, userId);
        String raw = similarIssuesTool.run("", ctx);
        return ApiResponse.success(agentService.parseSimilarIssues(raw));
    }

    // ── 유사 이슈 비교 ────────────────────────────────────────

    @PostMapping("/{eventId}/compare")
    public ApiResponse<List<CompareResult>> compareSimilarEvents(
            @PathVariable String eventId,
            @AuthenticationPrincipal String userId,
            @RequestBody CompareRequest req) {

        AgentContext ctx = buildContext(eventId, req.issueType(), userId);

        // input: "id1:type1,id2:type2"
        String input = req.similarEventRefs().stream()
                .map(r -> r.id() + ":" + r.type())
                .reduce((a, b) -> a + "," + b).orElse("");

        String raw = compareWithSimilarTool.run(input, ctx);
        return ApiResponse.success(parseCompareResults(raw));
    }

    record CompareRequest(String issueType, List<SimilarRef> similarEventRefs) {}
    record SimilarRef(String id, String type) {}

    record CompareResult(
            String title, List<String> commonPoints,
            List<String> differences, String userRelevanceDifference
    ) {}

    private List<CompareResult> parseCompareResults(String raw) {
        return Arrays.stream(raw.split("---"))
                .map(String::trim)
                .filter(block -> !block.isBlank())
                .map(block -> {
                    String title = extractLine(block, "COMPARE_TITLE:").orElse("");
                    List<String> common = extractLine(block, "COMMON:")
                            .map(s -> Arrays.asList(s.split("##"))).orElse(List.of());
                    List<String> diff = extractLine(block, "DIFF:")
                            .map(s -> Arrays.asList(s.split("##"))).orElse(List.of());
                    String relevance = extractLine(block, "USER_RELEVANCE:").orElse("");
                    return new CompareResult(title, common, diff, relevance);
                })
                .toList();
    }

    // ── 의견 초안 ────────────────────────────────────────────

    @PostMapping("/{eventId}/opinion-draft")
    public ApiResponse<OpinionDraftResponse> getOpinionDraft(
            @PathVariable String eventId,
            @AuthenticationPrincipal String userId,
            @RequestBody OpinionDraftRequest req) {

        AgentContext ctx = buildContext(eventId, req.issueType(), userId);
        String raw = generateOpinionDraftTool.run("stance=" + req.stance(), ctx);

        String stance = extractLine(raw, "STANCE:").orElse(req.stance());
        String draft = extractLine(raw, "DRAFT:").orElse("초안 생성에 실패했습니다. 직접 작성해 주세요.");
        String disclaimer = extractLine(raw, "DISCLAIMER:")
                .orElse("이 초안은 참고용이며, 제출 전 반드시 본인의 의견에 맞게 수정해야 합니다.");

        return ApiResponse.success(new OpinionDraftResponse(stance, draft, disclaimer));
    }

    record OpinionDraftRequest(String issueType, String stance) {}
    record OpinionDraftResponse(String stance, String draft, String disclaimer) {}

    // ── 캘린더 등록 (register_calendar_event) ────────────────

    @Transactional
    @PostMapping("/{eventId}/calendar")
    public ApiResponse<CalendarRegistrationResponse> registerCalendar(
            @PathVariable String eventId,
            @AuthenticationPrincipal String userId,
            @RequestBody CalendarRegistrationRequest req) {

        // 이미 등록된 경우 중복 방지
        if (calendarEventRepository.existsByUserIdAndEventId(userId, eventId)) {
            UserCalendarEvent existing = calendarEventRepository
                    .findByUserIdAndEventId(userId, eventId).orElseThrow();
            return ApiResponse.success(new CalendarRegistrationResponse(
                    true, "이미 등록된 일정입니다.",
                    existing.getId().toString(), existing.getCalendarTitle(),
                    existing.getCalendarDate().toString()));
        }

        // 캘린더 제목/날짜가 없으면 decideCalendarTool로 자동 산출
        String calendarTitle = req.calendarTitle();
        String calendarDate = req.calendarDate();

        if (calendarTitle == null || calendarDate == null) {
            AgentContext ctx = buildContext(eventId, req.issueType(), userId);
            String decision = decideCalendarTool.run("", ctx);
            if (calendarTitle == null)
                calendarTitle = extractLine(decision, "TITLE:").orElse(ctx.getIssueTitle());
            if (calendarDate == null)
                calendarDate = extractLine(decision, "DATE:").orElse("");
        }

        if (calendarDate == null || calendarDate.isBlank() || "상시".equals(calendarDate)) {
            return ApiResponse.success(new CalendarRegistrationResponse(
                    false, "등록할 마감일이 없습니다.", null, calendarTitle, null));
        }

        LocalDate parsedDate;
        try {
            parsedDate = parseCalendarDate(calendarDate);
        } catch (DateTimeParseException e) {
            log.warn("[Calendar] 날짜 파싱 실패 — date={}", calendarDate);
            return ApiResponse.success(new CalendarRegistrationResponse(
                    false, "날짜 형식이 올바르지 않습니다: " + calendarDate, null, calendarTitle, null));
        }

        UserCalendarEvent event = UserCalendarEvent.of(
                userId, eventId, req.issueType(),
                calendarTitle,
                parsedDate,
                req.reminder() != null ? req.reminder() : "3일 전"
        );

        UserCalendarEvent saved;
        try {
            saved = calendarEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 unique constraint 위반 — 이미 등록된 것으로 처리
            UserCalendarEvent existing = calendarEventRepository
                    .findByUserIdAndEventId(userId, eventId).orElseThrow();
            return ApiResponse.success(new CalendarRegistrationResponse(
                    true, "이미 등록된 일정입니다.",
                    existing.getId().toString(), existing.getCalendarTitle(),
                    existing.getCalendarDate().toString()));
        }

        log.info("[Calendar] 등록 완료 — userId={}, eventId={}, date={}", userId, eventId, calendarDate);
        return ApiResponse.success(new CalendarRegistrationResponse(
                true, "캘린더에 등록되었습니다.",
                saved.getId().toString(), saved.getCalendarTitle(),
                saved.getCalendarDate().toString()));
    }

    record CalendarRegistrationRequest(
            String issueType, String calendarTitle,
            String calendarDate, String reminder
    ) {}

    record CalendarRegistrationResponse(
            boolean registered, String message,
            String calendarEventId, String calendarTitle, String calendarDate
    ) {}

    // ── 내 캘린더 목록 ────────────────────────────────────────

    @GetMapping("/calendar")
    public ApiResponse<List<UserCalendarEvent>> getMyCalendar(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(calendarEventRepository.findByUserId(userId));
    }

    // ─── 공통 유틸 ────────────────────────────────────────────

    private LocalDate parseCalendarDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy.M.d"));
        }
    }

    private AgentContext buildContext(String issueId, String issueType, String userId) {
        return agentService.buildContext(issueId, issueType, userId, null);
    }

    private Optional<String> extractLine(String text, String prefix) {
        if (text == null) return Optional.empty();
        return Arrays.stream(text.split("\n"))
                .filter(l -> l.startsWith(prefix))
                .map(l -> l.substring(prefix.length()).trim())
                .filter(s -> !s.isBlank())
                .findFirst();
    }
}
