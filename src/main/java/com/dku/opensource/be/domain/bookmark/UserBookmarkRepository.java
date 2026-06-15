package com.dku.opensource.be.domain.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserBookmarkRepository extends JpaRepository<UserBookmark, Long> {

    Optional<UserBookmark> findByUserIdAndEventId(String userId, String eventId);

    List<UserBookmark> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndEventId(String userId, String eventId);

    // @Transactional: 컨트롤러 밖(에이전트 도구)에서 호출돼도 트랜잭션 보장
    @Transactional
    void deleteByUserIdAndEventId(String userId, String eventId);
}
