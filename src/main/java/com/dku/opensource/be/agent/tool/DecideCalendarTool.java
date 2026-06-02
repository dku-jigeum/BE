package com.dku.opensource.be.agent.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * decide_calendar_tool — 추출된 날짜와 사용자 관련성을 고려해 캘린더 등록 제안 여부를 판단한다.
 * extract_key_dates_tool 실행 후 호출을 권장한다.
 * 출력 형식:
 *   SUGGEST:true|false
 *   REASON:판단 이유
 *   TITLE:캘린더 제목
 *   DATE:yyyy-MM-dd or 상시
 *   REMINDER:3일 전
 */
@Component
public class DecideCalendarTool implements AgentTool {

    private static final int SUGGEST_THRESHOLD_DAYS = 30;

    @Override public String name() { return "decide_calendar_registration"; }

    @Override public String description() {
        return "마감일 존재 여부와 사용자 관련성을 고려해 캘린더 등록 제안 여부를 판단합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        if (ctx.getDeadline() == null || ctx.getDeadline().isBlank()) {
            return "SUGGEST:false\nREASON:별도 마감일이 확인되지 않아 캘린더 등록이 꼭 필요하지 않아요.\nTITLE:\nDATE:상시\nREMINDER:";
        }

        try {
            LocalDate deadline = LocalDate.parse(ctx.getDeadline());
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), deadline);

            if (daysLeft < 0) {
                return "SUGGEST:false\nREASON:이미 마감된 이슈입니다.\nTITLE:\nDATE:" + ctx.getDeadline() + "\nREMINDER:";
            }

            String title = buildCalendarTitle(ctx);
            String reminder = daysLeft <= 7 ? "1일 전" : "3일 전";

            if (daysLeft <= SUGGEST_THRESHOLD_DAYS) {
                return String.format(
                        "SUGGEST:true%nREASON:마감까지 D-%d 남아 있으며 사용자 관심사와 관련된 이슈입니다.%nTITLE:%s%nDATE:%s%nREMINDER:%s",
                        daysLeft, title,
                        deadline.format(DateTimeFormatter.ISO_LOCAL_DATE), reminder);
            } else {
                return String.format(
                        "SUGGEST:false%nREASON:마감까지 %d일 남아 여유가 있습니다. 관심 있으면 담아두세요.%nTITLE:%s%nDATE:%s%nREMINDER:%s",
                        daysLeft, title,
                        deadline.format(DateTimeFormatter.ISO_LOCAL_DATE), reminder);
            }
        } catch (Exception e) {
            return "SUGGEST:false\nREASON:마감일 확인 오류\nTITLE:\nDATE:\nREMINDER:";
        }
    }

    private String buildCalendarTitle(AgentContext ctx) {
        String prefix = switch (ctx.getIssueType() != null ? ctx.getIssueType().toLowerCase() : "") {
            case "legislation" -> "입법예고 의견 제출 마감";
            case "petition"    -> "청원 동의 마감";
            default            -> "법안 심사 마감";
        };
        String shortTitle = ctx.getIssueTitle() != null && ctx.getIssueTitle().length() > 20
                ? ctx.getIssueTitle().substring(0, 20) + "..."
                : ctx.getIssueTitle();
        return prefix + ": " + shortTitle;
    }
}
