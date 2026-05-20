package com.dku.opensource.be.domain.legislation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LegislationNoticeRepository extends JpaRepository<LegislationNotice, Long> {

    boolean existsByBillId(String billId);

    Optional<LegislationNotice> findByBillId(String billId);

    List<LegislationNotice> findByDeadlineBetween(LocalDate from, LocalDate to);

    @Query(value = "SELECT bill_id FROM legislation_notice WHERE embedding_vector IS NULL", nativeQuery = true)
    List<String> findBillIdsWithNullEmbedding(Pageable pageable);

    @Modifying
    @Query(value = "UPDATE legislation_notice SET embedding_vector = CAST(:vector AS vector) WHERE bill_id = :billId", nativeQuery = true)
    void updateEmbeddingVector(@Param("billId") String billId, @Param("vector") String vector);

    @Query(value = "SELECT * FROM legislation_notice WHERE embedding_vector IS NOT NULL " +
            "AND (deadline IS NULL OR deadline >= CURRENT_DATE) " +
            "ORDER BY embedding_vector <=> CAST(:userVector AS vector) LIMIT :limit", nativeQuery = true)
    List<LegislationNotice> findByEmbeddingSimilarityAfterDeadline(@Param("userVector") String userVector,
                                                                   @Param("limit") int limit);

    @Query(value = "SELECT * FROM legislation_notice WHERE deadline >= CURRENT_DATE " +
            "AND deadline <= CURRENT_DATE + INTERVAL '30 days' ORDER BY deadline ASC LIMIT :limit", nativeQuery = true)
    List<LegislationNotice> findDeadlineUrgent(@Param("limit") int limit);
}
