package com.jobportal.service;

import com.jobportal.dto.AuthResponse;
import com.jobportal.dto.LoginRequest;
import com.jobportal.dto.RegisterRequest;
import com.jobportal.model.Role;
import com.jobportal.model.UserAccount;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        Role role;
        try {
            role = Role.valueOf(request.role().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be JOB_SEEKER or EMPLOYER");
        }
        if (role == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrator accounts cannot be self-registered");
        }
        if (role == Role.EMPLOYER && (request.companyName() == null || request.companyName().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company name is required for employers");
        }

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setCompanyName(request.companyName() == null ? null : request.companyName().trim());
        return toResponse(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(UserAccount user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getEmail(),
                user.getFullName(), user.getRole(), user.getCompanyName());
    }
}
