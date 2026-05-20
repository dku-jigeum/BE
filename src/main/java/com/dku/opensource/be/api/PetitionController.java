package com.dku.opensource.be.api;

import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/petitions")
@RequiredArgsConstructor
public class PetitionController {

    private final PetitionRepository petitionRepository;

    @GetMapping
    public ApiResponse<Page<PetitionSummary>> getPetitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("participantCount").descending());
        return ApiResponse.success(petitionRepository.findAll(pageable).map(PetitionSummary::from));
    }

    @GetMapping("/{petitionNo}")
    public ResponseEntity<ApiResponse<PetitionDetail>> getPetition(@PathVariable String petitionNo) {
        return petitionRepository.findByPetitionNo(petitionNo)
                .map(p -> ResponseEntity.ok(ApiResponse.success(PetitionDetail.from(p))))
                .orElse(ResponseEntity.notFound().build());
    }

    record PetitionSummary(String petitionNo, String title, int participantCount, String deadline) {
        static PetitionSummary from(Petition p) {
            return new PetitionSummary(p.getPetitionNo(), p.getTitle(), p.getParticipantCount(),
                    p.getDeadline() != null ? p.getDeadline().toString() : null);
        }
    }

    record PetitionDetail(String petitionNo, String title, String content,
                          int participantCount, String deadline) {
        static PetitionDetail from(Petition p) {
            return new PetitionDetail(p.getPetitionNo(), p.getTitle(), p.getContent(),
                    p.getParticipantCount(), p.getDeadline() != null ? p.getDeadline().toString() : null);
        }
    }
}
