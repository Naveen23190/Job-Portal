package com.jobportal.dto;

import com.jobportal.model.InterviewStatus;

import java.time.Instant;

public record InterviewResponse(
        Long id,
        Long applicationId,
        String jobTitle,
        String applicantName,
        Instant proposedAt,
        Integer durationMinutes,
        String meetingUrl,
        String notes,
        InterviewStatus status,
        Instant createdAt,
        Instant respondedAt) {}
