package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.domain.calendar.UserCalendarEvent;
import com.dku.opensource.be.domain.calendar.UserCalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * remove_calendar_event — 사용자가 일정을 캘린더에서 지워달라고 할 때 사용.
 * 입력: "type:id". userId 는 컨텍스트에서만 취득. register_calendar_event 의 대칭.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveCalendarEventTool implements AgentTool {

    private final UserCalendarEventRepository calendarEventRepository;

    @Override public String name() { return "remove_calendar_event"; }

    @Override public String description() {
        return "사용자가 일정을 캘린더에서 지워달라고 할 때 사용합니다. 입력은 'type:id' 형식입니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String userId = ctx.getUserId();
        if (userId == null || userId.isBlank()) {
            return "REMOVED:false|MESSAGE:로그인이 필요해요.";
        }
        if (input == null || !input.contains(":")) {
            return "REMOVED:false|MESSAGE:지울 일정을 type:id 형식으로 지정해야 해요.";
        }
        String id = input.split(":", 2)[1].trim();

        UserCalendarEvent existing = calendarEventRepository.findByUserIdAndEventId(userId, id).orElse(null);
        if (existing == null) {
            return "REMOVED:false|MESSAGE:캘린더에 등록돼 있지 않은 일정이에요.";
        }
        calendarEventRepository.deleteByUserIdAndEventId(userId, id);
        log.info("[RemoveCalendar] userId={}, eventId={}", userId, id);
        return "REMOVED:true|TITLE:" + existing.getCalendarTitle() + "|MESSAGE:캘린더에서 지웠어요.";
    }
}
