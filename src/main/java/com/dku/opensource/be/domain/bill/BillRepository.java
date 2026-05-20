package com.dku.opensource.be.domain.bill;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillNo(String billNo);

    boolean existsByBillNo(String billNo);

    @Query("SELECT b.billNo FROM Bill b WHERE b.content IS NULL")
    List<String> findBillNosWithNullContent(Pageable pageable);

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.content IS NOT NULL AND b.content <> ''")
    long countBillsWithContent();

    @Query("SELECT b FROM Bill b WHERE b.content IS NOT NULL AND b.content <> ''")
    Page<Bill> findWithContent(Pageable pageable);

    @Query("SELECT b FROM Bill b WHERE b.content IS NULL OR b.content = ''")
    Page<Bill> findWithoutContent(Pageable pageable);

    List<Bill> findByDeadlineBetween(LocalDate from, LocalDate to);

    List<Bill> findTop20ByOrderByViewCountDesc();

    @Query(value = "SELECT bill_no FROM bill WHERE embedding_vector IS NULL",
            nativeQuery = true)
    List<String> findBillNosWithNullEmbedding(Pageable pageable);

    @Modifying
    @Query(value = "UPDATE bill SET embedding_vector = CAST(:vector AS vector) WHERE bill_no = :billNo",
            nativeQuery = true)
    void updateEmbeddingVector(@Param("billNo") String billNo, @Param("vector") String vector);

    // pgvector 코사인 유사도 — 전체
    @Query(value = "SELECT * FROM bill WHERE embedding_vector IS NOT NULL " +
            "ORDER BY embedding_vector <=> CAST(:userVector AS vector) LIMIT :limit",
            nativeQuery = true)
    List<Bill> findByEmbeddingSimilarity(@Param("userVector") String userVector,
                                         @Param("limit") int limit);

    // pgvector 코사인 유사도 — 마감일 필터 (마감 전 또는 마감일 없음)
    @Query(value = "SELECT * FROM bill WHERE embedding_vector IS NOT NULL " +
            "AND (deadline IS NULL OR deadline >= CURRENT_DATE) " +
            "ORDER BY embedding_vector <=> CAST(:userVector AS vector) LIMIT :limit",
            nativeQuery = true)
    List<Bill> findByEmbeddingSimilarityAfterDeadline(@Param("userVector") String userVector,
                                                      @Param("limit") int limit);
}
