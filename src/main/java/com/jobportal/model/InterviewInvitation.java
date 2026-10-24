package com.jobportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "interview_invitations")
@Getter
@Setter
@NoArgsConstructor
public class InterviewInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Column(nullable = false)
    private Instant proposedAt;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(length = 500)
    private String meetingUrl;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InterviewStatus status = InterviewStatus.PROPOSED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant respondedAt;
}
