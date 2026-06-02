package com.dku.opensource.be.api;

import lombok.extern.slf4j.Slf4j;
import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.domain.user.UserProfile;
import com.dku.opensource.be.domain.user.UserProfileRepository;
import com.dku.opensource.be.recommendation.RecommendationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileRepository userProfileRepository;
    private final RecommendationService recommendationService;

    @PostMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody CreateProfileRequest req) {
        if (userProfileRepository.existsByUserId(userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("이미 프로필이 존재합니다."));
        }
        UserProfile profile = UserProfile.of(userId);
        if (req.interestTags() != null && !req.interestTags().isEmpty()) {
            profile.updateInterestTags(req.interestTags());
        }
        profile.updateProfile(req.age(), req.occupation());
        userProfileRepository.save(profile);
        try { recommendationService.updateUserEmbedding(userId); } catch (Exception e) {
            log.warn("임베딩 업데이트 실패 (프로필은 저장됨): {}", e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ProfileResponse.from(profile)));
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody UpdateProfileRequest req) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();

        if (req.interestTags() != null) {
            profile.updateInterestTags(req.interestTags());
        }
        profile.updateProfile(req.age(), req.occupation());
        userProfileRepository.save(profile);
        try { recommendationService.updateUserEmbedding(userId); } catch (Exception e) {
            log.warn("임베딩 업데이트 실패 (프로필은 저장됨): {}", e.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.success(ProfileResponse.from(profile)));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal String userId) {
        return userProfileRepository.findByUserId(userId)
                .map(p -> ResponseEntity.ok(ApiResponse.success(ProfileResponse.from(p))))
                .orElse(ResponseEntity.notFound().build());
    }

    record CreateProfileRequest(Integer age, String occupation, List<String> interestTags) {}
    record UpdateProfileRequest(Integer age, String occupation, List<String> interestTags) {}

    record ProfileResponse(String userId, Integer age, String occupation,
                           List<String> interestTags, boolean hasEmbedding) {
        static ProfileResponse from(UserProfile p) {
            return new ProfileResponse(p.getUserId(), p.getAge(), p.getOccupation(),
                    p.getInterestTags(), p.getEmbeddingVector() != null);
        }
    }
}
