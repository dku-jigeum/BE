package com.dku.opensource.be.domain.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserCalendarEventRepository extends JpaRepository<UserCalendarEvent, Long> {

    Optional<UserCalendarEvent> findByUserIdAndEventId(String userId, String eventId);

    List<UserCalendarEvent> findByUserId(String userId);

    boolean existsByUserIdAndEventId(String userId, String eventId);

    // @Transactional: 컨트롤러 밖(에이전트 도구)에서 호출돼도 트랜잭션 보장
    @Transactional
    void deleteByUserIdAndEventId(String userId, String eventId);
}
