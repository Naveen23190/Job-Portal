package com.jobportal.dto;

import com.jobportal.model.Role;
import com.jobportal.model.UserAccount;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record UserResponse(Long id, String email, String fullName, Role role, String bio,
                           String resumeUrl, List<String> skills, List<String> education,
                           String companyName, Instant createdAt) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getBio(), user.getResumeUrl(), splitLines(user.getSkills()),
                splitLines(user.getEducation()), user.getCompanyName(), user.getCreatedAt());
    }

    private static List<String> splitLines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}
