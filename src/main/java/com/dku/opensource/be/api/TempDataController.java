package com.dku.opensource.be.api;

import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/temp")
@RequiredArgsConstructor
public class TempDataController {

    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;
    private final LegislationNoticeRepository legislationNoticeRepository;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> getStats() {
        return ApiResponse.success(Map.of(
                "bills", billRepository.count(),
                "billsWithContent", billRepository.countBillsWithContent(),
                "petitions", petitionRepository.count(),
                "legislation", legislationNoticeRepository.count()
        ));
    }

    @GetMapping("/bills")
    public ApiResponse<Page<BillRow>> getBills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Bill> bills = billRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
        return ApiResponse.success(bills.map(b -> new BillRow(
                b.getBillNo(), b.getTitle(),
                b.getContent(),
                b.getDeadline() != null ? b.getDeadline().toString() : null,
                b.getViewCount()
        )));
    }

    @GetMapping("/petitions")
    public ApiResponse<Page<PetitionRow>> getPetitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Petition> petitions = petitionRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
        return ApiResponse.success(petitions.map(p -> new PetitionRow(
                p.getPetitionNo(), p.getTitle(),
                p.getParticipantCount(),
                p.getDeadline() != null ? p.getDeadline().toString() : null
        )));
    }

    @GetMapping("/legislation")
    public ApiResponse<Page<LegislationRow>> getLegislation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LegislationNotice> notices = legislationNoticeRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
        return ApiResponse.success(notices.map(l -> new LegislationRow(
                l.getBillNo(), l.getTitle(),
                l.getDeadline() != null ? l.getDeadline().toString() : null
        )));
    }

    record BillRow(String billNo, String title, String content, String deadline, int viewCount) {}
    record PetitionRow(String petitionNo, String title, int participantCount, String deadline) {}
    record LegislationRow(String billNo, String title, String deadline) {}
}
