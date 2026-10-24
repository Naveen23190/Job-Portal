package com.jobportal.controller;

import com.jobportal.dto.InterviewRequest;
import com.jobportal.dto.InterviewResponse;
import com.jobportal.dto.InterviewResponseRequest;
import com.jobportal.model.*;
import com.jobportal.repository.InterviewInvitationRepository;
import com.jobportal.repository.JobApplicationRepository;
import com.jobportal.service.LiveUpdateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class InterviewController {
    private final JobApplicationRepository applicationRepository;
    private final InterviewInvitationRepository interviewRepository;
    private final LiveUpdateService liveUpdates;

    public InterviewController(
            JobApplicationRepository applicationRepository,
            InterviewInvitationRepository interviewRepository,
            LiveUpdateService liveUpdates) {
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.liveUpdates = liveUpdates;
    }

    @GetMapping("/interviews/me")
    @PreAuthorize("hasAnyRole('JOB_SEEKER', 'EMPLOYER')")
    public List<InterviewResponse> myInterviews(@AuthenticationPrincipal UserAccount user) {
        List<InterviewInvitation> interviews = user.getRole() == Role.JOB_SEEKER
                ? interviewRepository.findByApplication_Applicant_IdOrderByProposedAtDesc(user.getId())
                : interviewRepository.findByApplication_Job_Employer_IdOrderByProposedAtDesc(user.getId());
        return interviews.stream().map(this::toResponse).toList();
    }

    @PostMapping("/applications/{applicationId}/interviews")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InterviewResponse propose(
            @PathVariable Long applicationId,
            @Valid @RequestBody InterviewRequest request,
            @AuthenticationPrincipal UserAccount employer) {
        JobApplication application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        verifyOwnerOrAdmin(application.getJob(), employer);
        if (application.getStatus() == ApplicationStatus.WITHDRAWN
                || application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.HIRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An interview cannot be proposed for this application");
        }
        if (interviewRepository.existsByApplication_IdAndStatus(applicationId, InterviewStatus.PROPOSED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This application already has a pending interview proposal");
        }
        validateMeetingUrl(request.meetingUrl());

        InterviewInvitation invitation = new InterviewInvitation();
        invitation.setApplication(application);
        invitation.setProposedAt(request.proposedAt());
        invitation.setDurationMinutes(request.durationMinutes());
        invitation.setMeetingUrl(blankToNull(request.meetingUrl()));
        invitation.setNotes(blankToNull(request.notes()));
        InterviewInvitation saved = interviewRepository.save(invitation);

        if (application.getStatus() != ApplicationStatus.INTERVIEW) {
            application.setStatus(ApplicationStatus.INTERVIEW);
            applicationRepository.save(application);
        }
        publishAfterCommit(() -> {
            liveUpdates.publishInterviewUpdate(application.getApplicant().getId());
            liveUpdates.publishApplicationUpdate(application.getApplicant().getId());
            liveUpdates.publishApplicationUpdate(application.getJob().getEmployer().getId());
        });
        return toResponse(saved);
    }

    @PatchMapping("/interviews/{interviewId}/response")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @Transactional
    public InterviewResponse respond(
            @PathVariable Long interviewId,
            @Valid @RequestBody InterviewResponseRequest request,
            @AuthenticationPrincipal UserAccount applicant) {
        InterviewInvitation invitation = interviewRepository.findByIdForUpdate(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview invitation not found"));
        if (!invitation.getApplication().getApplicant().getId().equals(applicant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only respond to your own interview invitations");
        }
        if (invitation.getStatus() != InterviewStatus.PROPOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This interview invitation has already been answered");
        }
        if (request.status() != InterviewStatus.ACCEPTED && request.status() != InterviewStatus.DECLINED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Interview response must be ACCEPTED or DECLINED");
        }

        invitation.setStatus(request.status());
        invitation.setRespondedAt(Instant.now());
        InterviewInvitation saved = interviewRepository.save(invitation);
        publishAfterCommit(() -> {
            liveUpdates.publishInterviewUpdate(invitation.getApplication().getJob().getEmployer().getId());
            liveUpdates.publishApplicationUpdate(invitation.getApplication().getJob().getEmployer().getId());
            liveUpdates.publishInterviewUpdate(applicant.getId());
        });
        return toResponse(saved);
    }

    private void verifyOwnerOrAdmin(JobPosting job, UserAccount user) {
        if (user.getRole() != Role.ADMIN && !job.getEmployer().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not manage this job");
        }
    }

    private void validateMeetingUrl(String value) {
        if (value == null || value.isBlank()) return;
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.toLowerCase(Locale.ROOT).equals("http")
                    && !scheme.toLowerCase(Locale.ROOT).equals("https")) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Meeting link must be a valid HTTP or HTTPS URL");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private InterviewResponse toResponse(InterviewInvitation invitation) {
        JobApplication application = invitation.getApplication();
        return new InterviewResponse(
                invitation.getId(),
                application.getId(),
                application.getJob().getTitle(),
                application.getApplicant().getFullName(),
                invitation.getProposedAt(),
                invitation.getDurationMinutes(),
                invitation.getMeetingUrl(),
                invitation.getNotes(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getRespondedAt());
    }

    private void publishAfterCommit(Runnable publication) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publication.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publication.run();
            }
        });
    }
}
