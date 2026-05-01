package com.dku.opensource.be.domain.legislation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LegislationNoticeRepository extends JpaRepository<LegislationNotice, Long> {

    Optional<LegislationNotice> findByNoticeNo(String noticeNo);

    boolean existsByNoticeNo(String noticeNo);

    List<LegislationNotice> findByDeadlineBetween(LocalDate from, LocalDate to);
}
