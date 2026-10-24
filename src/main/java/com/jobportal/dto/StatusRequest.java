package com.jobportal.dto;

import com.jobportal.model.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull ApplicationStatus status) {}
