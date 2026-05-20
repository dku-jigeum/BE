package com.dku.opensource.be.api;

import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillRepository billRepository;

    @GetMapping
    public ApiResponse<Page<BillSummary>> getBills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String committee) {
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Bill> bills = committee != null && !committee.isBlank()
                ? billRepository.findByCommittee(committee, pageable)
                : billRepository.findAll(pageable);
        return ApiResponse.success(bills.map(BillSummary::from));
    }

    @GetMapping("/{billNo}")
    @Transactional
    public ResponseEntity<ApiResponse<BillDetail>> getBill(@PathVariable String billNo) {
        return billRepository.findByBillNo(billNo)
                .map(bill -> {
                    bill.incrementViewCount();
                    return ResponseEntity.ok(ApiResponse.success(BillDetail.from(bill)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    record BillSummary(String billNo, String title, String committee, String deadline, int viewCount) {
        static BillSummary from(Bill b) {
            return new BillSummary(b.getBillNo(), b.getTitle(), b.getCommittee(),
                    b.getDeadline() != null ? b.getDeadline().toString() : null, b.getViewCount());
        }
    }

    record BillDetail(String billNo, String title, String committee, String content,
                      String deadline, int viewCount) {
        static BillDetail from(Bill b) {
            return new BillDetail(b.getBillNo(), b.getTitle(), b.getCommittee(), b.getContent(),
                    b.getDeadline() != null ? b.getDeadline().toString() : null, b.getViewCount());
        }
    }
}
