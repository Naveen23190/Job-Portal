package com.jobportal.dto;

import com.jobportal.model.JobStatus;
import com.jobportal.model.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;

public record JobResponse(Long id, String title, String location, String employmentType,
                          String description, String requirements, BigDecimal salaryMinLpa,
                          BigDecimal salaryMaxLpa, BigDecimal stipendMinMonthly,
                          BigDecimal stipendMaxMonthly, WorkMode workMode,
                          Integer minExperienceYears, Integer maxExperienceYears, JobStatus status,
                          Long employerId, String companyName, Instant createdAt) {}
