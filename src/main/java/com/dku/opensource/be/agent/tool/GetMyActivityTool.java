package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.domain.bookmark.UserBookmarkRepository;
import com.dku.opensource.be.domain.calendar.UserCalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * get_my_activity — 사용자가 자신이 담은 관심 이슈(북마크)나 등록한 캘린더 일정을 물어볼 때 사용.
 * 입력 불필요, userId는 컨텍스트에서 취득.
 */
@Component
@RequiredArgsConstructor
public class GetMyActivityTool implements AgentTool {

    private static final int MAX_ITEMS = 10;

    private final UserBookmarkRepository bookmarkRepository;
    private final UserCalendarEventRepository calendarEventRepository;

    @Override public String name() { return "get_my_activity"; }

    @Override public String description() {
        return "사용자가 자신이 담은 관심 이슈(북마크)나 등록한 캘린더 일정을 물어볼 때 사용합니다. 입력은 필요 없습니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String userId = ctx.getUserId();
        if (userId == null || userId.isBlank()) {
            return "내 활동을 보려면 로그인이 필요해요.";
        }

        String bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(MAX_ITEMS)
                .map(b -> "- " + b.getTitle())
                .collect(Collectors.joining("\n"));

        String calendar = calendarEventRepository.findByUserId(userId).stream()
                .limit(MAX_ITEMS)
                .map(c -> "- " + c.getCalendarTitle() + " (" + c.getCalendarDate() + ")")
                .collect(Collectors.joining("\n"));

        return "BOOKMARKS:\n" + (bookmarks.isBlank() ? "(없음)" : bookmarks)
                + "\n\nCALENDAR:\n" + (calendar.isBlank() ? "(없음)" : calendar);
    }
}
