package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.domain.bookmark.UserBookmark;
import com.dku.opensource.be.domain.bookmark.UserBookmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * remove_bookmark — 사용자가 이슈를 관심 목록(북마크)에서 빼달라고 할 때 사용.
 * 입력: "type:id". userId 는 컨텍스트에서만 취득. add_bookmark 의 대칭.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveBookmarkTool implements AgentTool {

    private final UserBookmarkRepository bookmarkRepository;

    @Override public String name() { return "remove_bookmark"; }

    @Override public String description() {
        return "사용자가 이슈를 관심 목록(북마크)에서 빼달라고 할 때 사용합니다. 입력은 'type:id' 형식입니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String userId = ctx.getUserId();
        if (userId == null || userId.isBlank()) {
            return "REMOVED:false|MESSAGE:로그인이 필요해요.";
        }
        if (input == null || !input.contains(":")) {
            return "REMOVED:false|MESSAGE:뺄 이슈를 type:id 형식으로 지정해야 해요.";
        }
        String id = input.split(":", 2)[1].trim();

        UserBookmark existing = bookmarkRepository.findByUserIdAndEventId(userId, id).orElse(null);
        if (existing == null) {
            return "REMOVED:false|MESSAGE:관심 목록에 없던 이슈예요.";
        }
        bookmarkRepository.deleteByUserIdAndEventId(userId, id);
        log.info("[RemoveBookmark] userId={}, eventId={}", userId, id);
        return "REMOVED:true|TITLE:" + existing.getTitle() + "|MESSAGE:관심 목록에서 뺐어요.";
    }
}
