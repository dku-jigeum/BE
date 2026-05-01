package com.dku.opensource.be.domain.petition;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "petition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Petition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String petitionNo;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDate deadline;

    @Column(nullable = false)
    private int participantCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static Petition of(String petitionNo, String title, String content,
                               LocalDate deadline, int participantCount) {
        Petition petition = new Petition();
        petition.petitionNo = petitionNo;
        petition.title = title;
        petition.content = content;
        petition.deadline = deadline;
        petition.participantCount = participantCount;
        return petition;
    }

    public void updateParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }
}
