package com.dku.opensource.be.agent.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * calendar_tool — 마감이 D-30 이하면 캘린더 등록 제안.
 */
@Component
public class CalendarTool implements AgentTool {

    @Override public String name() { return "calendar_tool"; }

    @Override public String description() {
        return "이슈 마감일을 확인해 D-30 이하이면 캘린더 등록을 제안합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        if (ctx.getDeadline() == null || ctx.getDeadline().isBlank()) {
            return "CALENDAR_SUGGEST:false|마감일 정보 없음";
        }
        try {
            LocalDate deadline = LocalDate.parse(ctx.getDeadline());
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
            if (daysLeft >= 0 && daysLeft <= 30) {
                return "CALENDAR_SUGGEST:true|D-" + daysLeft + " 남았습니다. 캘린더 등록을 권장합니다.";
            } else {
                return "CALENDAR_SUGGEST:false|마감까지 " + daysLeft + "일 남아 아직 여유가 있습니다.";
            }
        } catch (Exception e) {
            return "CALENDAR_SUGGEST:false|마감일 파싱 오류";
        }
    }
}
