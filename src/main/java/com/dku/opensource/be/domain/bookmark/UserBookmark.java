package com.dku.opensource.be.domain.bookmark;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_bookmark",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "issue_type", nullable = false, length = 20)
    private String issueType;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static UserBookmark of(String userId, String eventId, String issueType, String title) {
        UserBookmark b = new UserBookmark();
        b.userId = userId;
        b.eventId = eventId;
        b.issueType = issueType;
        b.title = title;
        return b;
    }
}
