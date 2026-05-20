package com.dku.opensource.be.api;

import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.domain.user.UserProfile;
import com.dku.opensource.be.domain.user.UserProfileRepository;
import com.dku.opensource.be.recommendation.RecommendationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileRepository userProfileRepository;
    private final RecommendationService recommendationService;

    @PostMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(@RequestBody CreateProfileRequest req) {
        if (userProfileRepository.existsByUserId(req.userId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("이미 존재하는 사용자입니다."));
        }
        UserProfile profile = UserProfile.of(req.userId());
        if (req.interestTags() != null && !req.interestTags().isEmpty()) {
            profile.updateInterestTags(req.interestTags());
        }
        profile.updateProfile(req.age(), req.occupation());
        userProfileRepository.save(profile);
        recommendationService.updateUserEmbedding(req.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ProfileResponse.from(profile)));
    }

    @PutMapping("/profile/{userId}")
    @Transactional
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @PathVariable String userId,
            @RequestBody UpdateProfileRequest req) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();

        if (req.interestTags() != null) {
            profile.updateInterestTags(req.interestTags());
        }
        profile.updateProfile(req.age(), req.occupation());
        userProfileRepository.save(profile);
        recommendationService.updateUserEmbedding(userId);
        return ResponseEntity.ok(ApiResponse.success(ProfileResponse.from(profile)));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@PathVariable String userId) {
        return userProfileRepository.findByUserId(userId)
                .map(p -> ResponseEntity.ok(ApiResponse.success(ProfileResponse.from(p))))
                .orElse(ResponseEntity.notFound().build());
    }

    record CreateProfileRequest(String userId, Integer age, String occupation, List<String> interestTags) {}
    record UpdateProfileRequest(Integer age, String occupation, List<String> interestTags) {}

    record ProfileResponse(String userId, Integer age, String occupation,
                           List<String> interestTags, boolean hasEmbedding) {
        static ProfileResponse from(UserProfile p) {
            return new ProfileResponse(p.getUserId(), p.getAge(), p.getOccupation(),
                    p.getInterestTags(), p.getEmbeddingVector() != null);
        }
    }
}
