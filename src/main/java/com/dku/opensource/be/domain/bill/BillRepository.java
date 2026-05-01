package com.dku.opensource.be.domain.bill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillNo(String billNo);

    boolean existsByBillNo(String billNo);

    List<Bill> findByDeadlineBetween(LocalDate from, LocalDate to);

    List<Bill> findTop20ByOrderByViewCountDesc();

    // pgvector 코사인 유사도 쿼리 — KAN-9에서 구현
    @Query(value = "SELECT * FROM bill WHERE embedding_vector IS NOT NULL " +
            "ORDER BY embedding_vector <=> CAST(:userVector AS vector) LIMIT :limit",
            nativeQuery = true)
    List<Bill> findByEmbeddingSimilarity(@Param("userVector") String userVector,
                                         @Param("limit") int limit);
}
