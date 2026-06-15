package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.bookmark.UserBookmark;
import com.dku.opensource.be.domain.bookmark.UserBookmarkRepository;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * add_bookmark — 사용자가 이슈를 관심 목록(북마크)에 담아달라고 할 때 사용. 멱등.
 * 입력: "type:id" (예: "bill:2200063"). userId 는 컨텍스트에서만 취득.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddBookmarkTool implements AgentTool {

    private final UserBookmarkRepository bookmarkRepository;
    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;
    private final LegislationNoticeRepository legislationNoticeRepository;

    @Override public String name() { return "add_bookmark"; }

    @Override public String description() {
        return "사용자가 이슈를 관심 목록(북마크)에 담아달라고 할 때 사용합니다. 입력은 'type:id' 형식입니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String userId = ctx.getUserId();
        if (userId == null || userId.isBlank()) {
            return "BOOKMARKED:false|MESSAGE:로그인이 필요해요.";
        }
        if (input == null || !input.contains(":")) {
            return "BOOKMARKED:false|MESSAGE:담을 이슈를 type:id 형식으로 지정해야 해요.";
        }
        String[] parts = input.split(":", 2);
        String type = parts[0].trim().toLowerCase();
        String id = parts[1].trim();

        String title = lookupTitle(type, id).orElse(null);
        if (title == null) {
            return "BOOKMARKED:false|MESSAGE:해당 이슈를 찾지 못했어요.";
        }

        if (bookmarkRepository.existsByUserIdAndEventId(userId, id)) {
            return "BOOKMARKED:true|TITLE:" + title + "|MESSAGE:이미 관심 목록에 담겨 있어요.";
        }
        try {
            bookmarkRepository.save(UserBookmark.of(userId, id, type, title));
        } catch (DataIntegrityViolationException e) {
            return "BOOKMARKED:true|TITLE:" + title + "|MESSAGE:이미 관심 목록에 담겨 있어요.";
        }
        log.info("[AddBookmark] userId={}, eventId={}", userId, id);
        return "BOOKMARKED:true|TITLE:" + title + "|MESSAGE:관심 이슈에 담았어요.";
    }

    private Optional<String> lookupTitle(String type, String id) {
        return switch (type) {
            case "bill" -> billRepository.findByBillNo(id).map(b -> b.getTitle());
            case "petition" -> petitionRepository.findByPetitionNo(id).map(p -> p.getTitle());
            case "legislation" -> legislationNoticeRepository.findByBillId(id).map(n -> n.getTitle());
            default -> Optional.empty();
        };
    }
}
