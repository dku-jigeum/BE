package com.dku.opensource.be.domain.petition;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PetitionRepository extends JpaRepository<Petition, Long> {

    Optional<Petition> findByPetitionNo(String petitionNo);

    boolean existsByPetitionNo(String petitionNo);

    List<Petition> findByDeadlineBetween(LocalDate from, LocalDate to);

    List<Petition> findTop20ByOrderByParticipantCountDesc();

    @Query(value = "SELECT petition_no FROM petition WHERE embedding_vector IS NULL", nativeQuery = true)
    List<String> findNosWithNullEmbedding(Pageable pageable);

    @Modifying
    @Query(value = "UPDATE petition SET embedding_vector = CAST(:vector AS vector) WHERE petition_no = :petitionNo", nativeQuery = true)
    void updateEmbeddingVector(@Param("petitionNo") String petitionNo, @Param("vector") String vector);

    @Query(value = "SELECT * FROM petition WHERE embedding_vector IS NOT NULL " +
            "AND (deadline IS NULL OR deadline >= CURRENT_DATE) " +
            "ORDER BY embedding_vector <=> CAST(:userVector AS vector) LIMIT :limit", nativeQuery = true)
    List<Petition> findByEmbeddingSimilarityAfterDeadline(@Param("userVector") String userVector,
                                                          @Param("limit") int limit);

    @Query(value = "SELECT * FROM petition WHERE deadline >= CURRENT_DATE " +
            "AND deadline <= CURRENT_DATE + INTERVAL '30 days' ORDER BY deadline ASC LIMIT :limit", nativeQuery = true)
    List<Petition> findDeadlineUrgent(@Param("limit") int limit);
}
