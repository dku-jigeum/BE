package com.dku.opensource.be.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    @Query("SELECT u FROM UserProfile u JOIN u.interestTags t WHERE t IN :tags AND u.fcmToken IS NOT NULL")
    List<UserProfile> findByInterestTagsContainingAndFcmTokenIsNotNull(@Param("tags") List<String> tags);
}
