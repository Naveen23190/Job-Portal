package com.jobportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import com.jobportal.model.WorkMode;

public record JobRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 120) String location,
        @NotBlank @Size(max = 40) String employmentType,
        @NotBlank String description,
        String requirements,
        @DecimalMin("0.1") @DecimalMax("1000.0") @Digits(integer = 4, fraction = 2) BigDecimal salaryMinLpa,
        @DecimalMin("0.1") @DecimalMax("1000.0") @Digits(integer = 4, fraction = 2) BigDecimal salaryMaxLpa,
        @DecimalMin("1000") @DecimalMax("1000000") @Digits(integer = 7, fraction = 2) BigDecimal stipendMinMonthly,
        @DecimalMin("1000") @DecimalMax("1000000") @Digits(integer = 7, fraction = 2) BigDecimal stipendMaxMonthly,
        WorkMode workMode,
        @Min(0) @Max(50) Integer minExperienceYears,
        @Min(0) @Max(50) Integer maxExperienceYears
) {}
