package com.jobportal.dto;

import com.jobportal.model.InterviewStatus;
import jakarta.validation.constraints.NotNull;

public record InterviewResponseRequest(@NotNull InterviewStatus status) {}
