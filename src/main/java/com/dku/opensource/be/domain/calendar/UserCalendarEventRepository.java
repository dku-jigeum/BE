package com.dku.opensource.be.domain.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCalendarEventRepository extends JpaRepository<UserCalendarEvent, Long> {

    Optional<UserCalendarEvent> findByUserIdAndEventId(String userId, String eventId);

    List<UserCalendarEvent> findByUserId(String userId);

    boolean existsByUserIdAndEventId(String userId, String eventId);
}
