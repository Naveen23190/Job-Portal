package com.jobportal.dto;

import com.jobportal.model.Role;

public record AuthResponse(String token, Long id, String email, String fullName, Role role,
                           String companyName) {}
