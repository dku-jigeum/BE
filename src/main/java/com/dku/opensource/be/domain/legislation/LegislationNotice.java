package com.dku.opensource.be.domain.legislation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "legislation_notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegislationNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String noticeNo;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDate deadline;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static LegislationNotice of(String noticeNo, String title,
                                        String content, LocalDate deadline) {
        LegislationNotice notice = new LegislationNotice();
        notice.noticeNo = noticeNo;
        notice.title = title;
        notice.content = content;
        notice.deadline = deadline;
        return notice;
    }
}
