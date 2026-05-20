package com.dku.opensource.be.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String userId;

    @ElementCollection
    @CollectionTable(name = "user_interest_tag",
            joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "tag", length = 100)
    private List<String> interestTags = new ArrayList<>();

    @Column(columnDefinition = "vector(1536)", insertable = false, updatable = false)
    private String embeddingVector;

    @Column(length = 500)
    private String fcmToken;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static UserProfile of(String userId) {
        UserProfile profile = new UserProfile();
        profile.userId = userId;
        return profile;
    }

    public void updateInterestTags(List<String> interestTags) {
        this.interestTags.clear();
        this.interestTags.addAll(interestTags);
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
