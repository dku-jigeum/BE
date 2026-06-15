package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.calendar.UserCalendarEvent;
import com.dku.opensource.be.domain.calendar.UserCalendarEventRepository;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * register_calendar_event — 사용자가 일정/마감일을 캘린더에 등록해달라고 할 때 사용.
 * 이슈(type:id)의 마감일을 user_calendar_event 에 저장한다. 멱등(이미 있으면 그대로).
 *
 * 입력: "type:id" 또는 "type:id:date" (date 생략 시 이슈 마감일 사용)
 *   예) "bill:2200063", "petition:5000EA...:2026-07-01"
 * userId 는 입력이 아니라 컨텍스트(ctx.userId)에서만 취득한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterCalendarEventTool implements AgentTool {

    private final UserCalendarEventRepository calendarEventRepository;
    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;
    private final LegislationNoticeRepository legislationNoticeRepository;

    @Override public String name() { return "register_calendar_event"; }

    @Override public String description() {
        return "사용자가 이슈의 마감일을 캘린더에 등록해달라고 할 때 사용합니다. "
                + "입력은 'type:id' 또는 'type:id:날짜(yyyy-MM-dd)'. 등록할 이슈를 먼저 특정해야 합니다.";
    }

    private record IssueInfo(String title, String deadline) {}

    @Override
    public String run(String input, AgentContext ctx) {
        String userId = ctx.getUserId();
        if (userId == null || userId.isBlank()) {
            return "REGISTERED:false|MESSAGE:로그인이 필요해요.";
        }
        if (input == null || !input.contains(":")) {
            return "REGISTERED:false|MESSAGE:등록할 이슈를 type:id 형식으로 지정해야 해요.";
        }

        String[] parts = input.split(":", 3);
        String type = parts[0].trim().toLowerCase();
        String id = parts[1].trim();
        String dateInput = parts.length > 2 ? parts[2].trim() : null;

        IssueInfo issue = lookup(type, id).orElse(null);
        if (issue == null) {
            return "REGISTERED:false|MESSAGE:해당 이슈를 찾지 못했어요.";
        }

        String dateStr = (dateInput != null && !dateInput.isBlank()) ? dateInput : issue.deadline();
        if (dateStr == null || dateStr.isBlank() || "상시".equals(dateStr)) {
            return "REGISTERED:false|MESSAGE:마감일이 없어 캘린더에 등록할 수 없어요.";
        }

        LocalDate parsedDate;
        try {
            parsedDate = parseCalendarDate(dateStr);
        } catch (DateTimeParseException e) {
            return "REGISTERED:false|MESSAGE:날짜 형식을 이해하지 못했어요: " + dateStr;
        }

        if (calendarEventRepository.existsByUserIdAndEventId(userId, id)) {
            return String.format("REGISTERED:true|TITLE:%s|DATE:%s|MESSAGE:이미 캘린더에 등록돼 있어요.",
                    issue.title(), parsedDate);
        }

        try {
            calendarEventRepository.save(UserCalendarEvent.of(
                    userId, id, type, issue.title(), parsedDate, "3일 전"));
        } catch (DataIntegrityViolationException e) {
            return String.format("REGISTERED:true|TITLE:%s|DATE:%s|MESSAGE:이미 캘린더에 등록돼 있어요.",
                    issue.title(), parsedDate);
        }

        log.info("[RegisterCalendar] userId={}, eventId={}, date={}", userId, id, parsedDate);
        return String.format("REGISTERED:true|TITLE:%s|DATE:%s|MESSAGE:캘린더에 등록했어요. 마감 3일 전에 알려드릴게요.",
                issue.title(), parsedDate);
    }

    private Optional<IssueInfo> lookup(String type, String id) {
        return switch (type) {
            case "bill" -> billRepository.findByBillNo(id)
                    .map(b -> new IssueInfo(b.getTitle(), iso(b.getDeadline())));
            case "petition" -> petitionRepository.findByPetitionNo(id)
                    .map(p -> new IssueInfo(p.getTitle(), iso(p.getDeadline())));
            case "legislation" -> legislationNoticeRepository.findByBillId(id)
                    .map(n -> new IssueInfo(n.getTitle(), iso(n.getDeadline())));
            default -> Optional.empty();
        };
    }

    private String iso(LocalDate d) {
        return d != null ? d.toString() : null;
    }

    private LocalDate parseCalendarDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy.M.d"));
        }
    }
}
