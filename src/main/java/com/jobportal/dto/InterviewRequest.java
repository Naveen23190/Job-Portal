package com.jobportal.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record InterviewRequest(
        @NotNull @Future Instant proposedAt,
        @Min(15) @Max(240) int durationMinutes,
        @Size(max = 500) String meetingUrl,
        @Size(max = 2000) String notes) {}
