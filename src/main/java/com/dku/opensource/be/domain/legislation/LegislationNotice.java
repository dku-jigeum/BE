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

    @Column(unique = true, nullable = false, length = 100)
    private String billId;

    @Column(nullable = false, length = 50)
    private String billNo;

    @Column(nullable = false, length = 500)
    private String title;

    private LocalDate deadline;

    @Column(columnDefinition = "vector(1536)", insertable = false, updatable = false)
    private String embeddingVector;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static LegislationNotice of(String billId, String billNo, String title, LocalDate deadline) {
        LegislationNotice notice = new LegislationNotice();
        notice.billId = billId;
        notice.billNo = billNo;
        notice.title = title;
        notice.deadline = deadline;
        return notice;
    }
}
