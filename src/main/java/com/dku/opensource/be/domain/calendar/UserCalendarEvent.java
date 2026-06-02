package com.dku.opensource.be.domain.calendar;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_calendar_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "issue_type", nullable = false, length = 20)
    private String issueType;

    @Column(name = "calendar_title", nullable = false, length = 300)
    private String calendarTitle;

    @Column(name = "calendar_date", nullable = false)
    private LocalDate calendarDate;

    @Column(length = 20)
    private String reminder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static UserCalendarEvent of(String userId, String eventId, String issueType,
                                        String calendarTitle, LocalDate calendarDate,
                                        String reminder) {
        UserCalendarEvent e = new UserCalendarEvent();
        e.userId = userId;
        e.eventId = eventId;
        e.issueType = issueType;
        e.calendarTitle = calendarTitle;
        e.calendarDate = calendarDate;
        e.reminder = reminder;
        return e;
    }
}
