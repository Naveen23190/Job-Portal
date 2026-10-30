package com.jobportal.controller;

import com.jobportal.dto.ApplicationRequest;
import com.jobportal.dto.ApplicationResponse;
import com.jobportal.dto.StatusRequest;
import com.jobportal.model.*;
import com.jobportal.repository.JobApplicationRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.service.EmailNotificationService;
import com.jobportal.service.LiveUpdateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApplicationController {
    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final ObjectProvider<EmailNotificationService> emailNotifications;
    private final LiveUpdateService liveUpdates;
    private static final Logger log =
        LoggerFactory.getLogger(ApplicationController.class);

    public ApplicationController(
            JobRepository jobRepository,
            JobApplicationRepository applicationRepository,
            ObjectProvider<EmailNotificationService> emailNotifications,
            LiveUpdateService liveUpdates) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.emailNotifications = emailNotifications;
        this.liveUpdates = liveUpdates;
    }

    @PostMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@PathVariable Long jobId,
                                     @Valid @RequestBody ApplicationRequest request,
                                     @AuthenticationPrincipal UserAccount applicant) {
        JobPosting job = findJob(jobId);
        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This job is no longer accepting applications");
        }
        if (applicationRepository.existsByJobIdAndApplicantId(jobId, applicant.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied to this job");
        }
        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setApplicant(applicant);
        application.setCoverLetter(request.coverLetter());
        JobApplication saved = applicationRepository.save(application);
        liveUpdates.publishApplicationUpdate(applicant.getId());
        liveUpdates.publishApplicationUpdate(job.getEmployer().getId());
        emailNotifications.ifAvailable(notifications -> notifications.applicationSubmitted(
                applicant.getEmail(), applicant.getFullName(),
                job.getEmployer().getEmail(),
                job.getEmployer().getCompanyName() == null
                        ? job.getEmployer().getFullName() : job.getEmployer().getCompanyName(),
                job.getTitle()));
        return toResponse(saved);
    }

    @GetMapping("/applications/me")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public List<ApplicationResponse> myApplications(@AuthenticationPrincipal UserAccount applicant) {
        return applicationRepository.findByApplicantIdOrderByAppliedAtDesc(applicant.getId())
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<ApplicationResponse> applicationsForJob(
            @PathVariable Long jobId, @AuthenticationPrincipal UserAccount user) {
        JobPosting job = findJob(jobId);
        verifyOwnerOrAdmin(job, user);
        return applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId)
                .stream().map(this::toResponse).toList();
    }

    @PatchMapping("/applications/{applicationId}/status")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public ApplicationResponse updateStatus(@PathVariable Long applicationId,
                                            @Valid @RequestBody StatusRequest request,
                                            @AuthenticationPrincipal UserAccount user) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        verifyOwnerOrAdmin(application.getJob(), user);
        if (request.status() == ApplicationStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Applicants must withdraw their own applications");
        }
        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A withdrawn application cannot be updated");
        }
        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(request.status());
        JobApplication saved = applicationRepository.save(application);
        if (previousStatus != saved.getStatus()) {
            liveUpdates.publishApplicationUpdate(application.getApplicant().getId());
            liveUpdates.publishApplicationUpdate(application.getJob().getEmployer().getId());
            emailNotifications.ifAvailable(notifications -> notifications.applicationStatusChanged(
                    application.getApplicant().getEmail(),
                    application.getApplicant().getFullName(),
                    application.getJob().getTitle(),
                    saved.getStatus().name().replace('_', ' ').toLowerCase()));
        }
        return toResponse(saved);
    }

    @PatchMapping("/applications/{applicationId}/withdraw")
    public ApplicationResponse withdraw(@PathVariable Long applicationId,
                                        @AuthenticationPrincipal UserAccount applicant) {
        if (applicant == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Sign in to withdraw your application");
        }
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        if (!application.getApplicant().getId().equals(applicant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only withdraw your own application");
        }
        if (application.getStatus() != ApplicationStatus.SUBMITTED
                && application.getStatus() != ApplicationStatus.REVIEWING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only submitted or reviewing applications can be withdrawn");
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        JobApplication saved = applicationRepository.save(application);
        UserAccount employer = application.getJob().getEmployer();
        liveUpdates.publishApplicationUpdate(applicant.getId());
        liveUpdates.publishApplicationUpdate(employer.getId());
        emailNotifications.ifAvailable(notifications -> notifications.applicationWithdrawn(
                employer.getEmail(),
                employer.getCompanyName() == null ? employer.getFullName() : employer.getCompanyName(),
                applicant.getFullName(),
                application.getJob().getTitle()));
        return toResponse(saved);
    }

    private JobPosting findJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    private void verifyOwnerOrAdmin(JobPosting job, UserAccount user) {
        if (user.getRole() != Role.ADMIN && !job.getEmployer().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not manage this job");
        }
    }

    private ApplicationResponse toResponse(JobApplication application) {
        UserAccount applicant = application.getApplicant();
        return new ApplicationResponse(application.getId(), application.getJob().getId(),
                application.getJob().getTitle(), applicant.getId(), applicant.getFullName(),
                applicant.getEmail(), applicant.getResumeUrl(), application.getCoverLetter(), application.getStatus(),
                application.getAppliedAt());
    }
}
