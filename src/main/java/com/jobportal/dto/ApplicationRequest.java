package com.jobportal.dto;

import jakarta.validation.constraints.Size;

public record ApplicationRequest(@Size(max = 5000) String coverLetter) {}
