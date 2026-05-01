package com.dku.opensource.be.domain.petition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PetitionRepository extends JpaRepository<Petition, Long> {

    Optional<Petition> findByPetitionNo(String petitionNo);

    boolean existsByPetitionNo(String petitionNo);

    List<Petition> findByDeadlineBetween(LocalDate from, LocalDate to);

    List<Petition> findTop20ByOrderByParticipantCountDesc();
}
