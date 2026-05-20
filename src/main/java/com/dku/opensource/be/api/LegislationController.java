package com.dku.opensource.be.api;

import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/legislation")
@RequiredArgsConstructor
public class LegislationController {

    private final LegislationNoticeRepository legislationNoticeRepository;

    @GetMapping
    public ApiResponse<Page<LegislationSummary>> getLegislation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ApiResponse.success(legislationNoticeRepository.findAll(pageable).map(LegislationSummary::from));
    }

    @GetMapping("/{billId}")
    public ResponseEntity<ApiResponse<LegislationDetail>> getLegislation(@PathVariable String billId) {
        return legislationNoticeRepository.findByBillId(billId)
                .map(l -> ResponseEntity.ok(ApiResponse.success(LegislationDetail.from(l))))
                .orElse(ResponseEntity.notFound().build());
    }

    record LegislationSummary(String billId, String billNo, String title, String deadline) {
        static LegislationSummary from(LegislationNotice l) {
            return new LegislationSummary(l.getBillId(), l.getBillNo(), l.getTitle(),
                    l.getDeadline() != null ? l.getDeadline().toString() : null);
        }
    }

    record LegislationDetail(String billId, String billNo, String title, String deadline) {
        static LegislationDetail from(LegislationNotice l) {
            return new LegislationDetail(l.getBillId(), l.getBillNo(), l.getTitle(),
                    l.getDeadline() != null ? l.getDeadline().toString() : null);
        }
    }
}
