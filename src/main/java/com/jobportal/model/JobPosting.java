package com.jobportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class JobPosting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 120)
    private String location;

    @Column(nullable = false, length = 40)
    private String employmentType;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private WorkMode workMode;

    private Integer minExperienceYears;

    private Integer maxExperienceYears;

    @Column(precision = 7, scale = 2)
    private BigDecimal salaryMinLpa;

    @Column(precision = 7, scale = 2)
    private BigDecimal salaryMaxLpa;

    @Column(precision = 9, scale = 2)
    private BigDecimal stipendMinMonthly;

    @Column(precision = 9, scale = 2)
    private BigDecimal stipendMaxMonthly;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status = JobStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private UserAccount employer;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
