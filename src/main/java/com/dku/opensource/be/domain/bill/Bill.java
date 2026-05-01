package com.dku.opensource.be.domain.bill;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String billNo;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDate deadline;

    // pgvector: 배치 INSERT 시 제외, 추천 서비스에서 native query로만 업데이트
    @Column(columnDefinition = "vector(1536)", insertable = false, updatable = false)
    private String embeddingVector;

    @Column(nullable = false)
    private int viewCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static Bill of(String billNo, String title, String content, LocalDate deadline) {
        Bill bill = new Bill();
        bill.billNo = billNo;
        bill.title = title;
        bill.content = content;
        bill.deadline = deadline;
        return bill;
    }

    public void updateEmbedding(String embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
