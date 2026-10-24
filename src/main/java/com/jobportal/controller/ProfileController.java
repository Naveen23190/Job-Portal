package com.jobportal.controller;

import com.jobportal.dto.ProfileRequest;
import com.jobportal.dto.UserResponse;
import com.jobportal.model.UserAccount;
import com.jobportal.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse getProfile(@AuthenticationPrincipal UserAccount user) {
        return toResponse(userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")));
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@Valid @RequestBody ProfileRequest request,
                                      @AuthenticationPrincipal UserAccount user) {
        UserAccount profile = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (request.fullName() != null) profile.setFullName(request.fullName().trim());
        if (request.bio() != null) profile.setBio(request.bio().trim());
        if (request.resumeUrl() != null) profile.setResumeUrl(validateResumeUrl(request.resumeUrl()));
        if (request.skills() != null) profile.setSkills(String.join("\n", request.skills()));
        if (request.education() != null) profile.setEducation(String.join("\n", request.education()));
        if (request.companyName() != null) profile.setCompanyName(request.companyName().trim());
        return toResponse(userRepository.save(profile));
    }

    private String validateResumeUrl(String value) {
        String resumeUrl = value.trim();
        if (resumeUrl.isEmpty()) return resumeUrl;
        try {
            URI uri = URI.create(resumeUrl);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || scheme == null
                    || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Resume URL must be a valid HTTP or HTTPS link");
        }
        return resumeUrl;
    }

    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private UserResponse toResponse(UserAccount user) {
        return UserResponse.from(user);
    }
}
