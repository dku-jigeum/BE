package com.dku.opensource.be.domain.legislation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LegislationNoticeRepository extends JpaRepository<LegislationNotice, Long> {

    boolean existsByBillId(String billId);

    java.util.Optional<LegislationNotice> findByBillId(String billId);

    List<LegislationNotice> findByDeadlineBetween(LocalDate from, LocalDate to);
}
