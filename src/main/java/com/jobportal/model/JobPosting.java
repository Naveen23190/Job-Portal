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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public WorkMode getWorkMode() {
        return workMode;
    }

    public void setWorkMode(WorkMode workMode) {
        this.workMode = workMode;
    }

    public Integer getMinExperienceYears() {
        return minExperienceYears;
    }

    public void setMinExperienceYears(Integer minExperienceYears) {
        this.minExperienceYears = minExperienceYears;
    }

    public Integer getMaxExperienceYears() {
        return maxExperienceYears;
    }

    public void setMaxExperienceYears(Integer maxExperienceYears) {
        this.maxExperienceYears = maxExperienceYears;
    }

    public BigDecimal getSalaryMinLpa() {
        return salaryMinLpa;
    }

    public void setSalaryMinLpa(BigDecimal salaryMinLpa) {
        this.salaryMinLpa = salaryMinLpa;
    }

    public BigDecimal getSalaryMaxLpa() {
        return salaryMaxLpa;
    }

    public void setSalaryMaxLpa(BigDecimal salaryMaxLpa) {
        this.salaryMaxLpa = salaryMaxLpa;
    }

    public BigDecimal getStipendMinMonthly() {
        return stipendMinMonthly;
    }

    public void setStipendMinMonthly(BigDecimal stipendMinMonthly) {
        this.stipendMinMonthly = stipendMinMonthly;
    }

    public BigDecimal getStipendMaxMonthly() {
        return stipendMaxMonthly;
    }

    public void setStipendMaxMonthly(BigDecimal stipendMaxMonthly) {
        this.stipendMaxMonthly = stipendMaxMonthly;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public UserAccount getEmployer() {
        return employer;
    }

    public void setEmployer(UserAccount employer) {
        this.employer = employer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
