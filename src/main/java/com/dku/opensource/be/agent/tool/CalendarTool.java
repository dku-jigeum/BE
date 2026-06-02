package com.dku.opensource.be.agent.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * extract_key_dates_tool — 이슈에서 중요 날짜를 추출하고 D-day를 계산한다.
 * 출력 형식(줄바꿈 구분):
 *   DATE_TYPE:type|DATE_LABEL:라벨|DATE_VALUE:yyyy-MM-dd or 상시|DDAY:D-N or 상시
 */
@Component
public class CalendarTool implements AgentTool {

    @Override public String name() { return "extract_key_dates"; }

    @Override public String description() {
        return "이슈의 마감일·시행일 등 중요 날짜를 추출하고 D-day를 계산합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        if (ctx.getDeadline() == null || ctx.getDeadline().isBlank()) {
            return "DATE_TYPE:always_open|DATE_LABEL:참여 마감|DATE_VALUE:상시|DDAY:상시";
        }
        try {
            LocalDate deadline = LocalDate.parse(ctx.getDeadline());
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
            String ddayStr = daysLeft >= 0 ? "D-" + daysLeft : "마감";
            String dateLabel = resolveLabel(ctx.getIssueType());
            return String.format("DATE_TYPE:%s|DATE_LABEL:%s|DATE_VALUE:%s|DDAY:%s",
                    resolveType(ctx.getIssueType()),
                    dateLabel,
                    deadline.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    ddayStr);
        } catch (Exception e) {
            return "DATE_TYPE:unknown|DATE_LABEL:마감 정보|DATE_VALUE:확인 불가|DDAY:미상";
        }
    }

    private String resolveType(String issueType) {
        return switch (issueType != null ? issueType.toLowerCase() : "") {
            case "legislation" -> "opinion_deadline";
            case "petition"    -> "petition_deadline";
            default            -> "review_date";
        };
    }

    private String resolveLabel(String issueType) {
        return switch (issueType != null ? issueType.toLowerCase() : "") {
            case "legislation" -> "의견 제출 마감일";
            case "petition"    -> "청원 동의 마감일";
            default            -> "심사 마감일";
        };
    }
}
