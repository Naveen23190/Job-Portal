package com.jobportal.dto;

import com.jobportal.model.ApplicationStatus;

import java.time.Instant;

public record ApplicationResponse(Long id, Long jobId, String jobTitle, Long applicantId,
                                 String applicantName, String applicantEmail, String resumeUrl,
                                 String coverLetter, ApplicationStatus status, Instant appliedAt) {}
