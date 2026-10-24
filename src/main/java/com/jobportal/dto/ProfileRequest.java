package com.jobportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProfileRequest(
        @Size(max = 120) String fullName,
        @Size(max = 2000) String bio,
        @Size(max = 500) String resumeUrl,
        @Size(max = 20) List<@NotBlank @Size(max = 80) String> skills,
        @Size(max = 20) List<@NotBlank @Size(max = 160) String> education,
        @Size(max = 160) String companyName
) {}
