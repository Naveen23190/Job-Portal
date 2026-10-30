
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private static final Logger logger =
            LoggerFactory.getLogger(ApplicationController.class);

    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final ObjectProvider<EmailNotificationService> emailNotifications;
    private final LiveUpdateService liveUpdates;

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

    // ----------------------------------------------------
    // APPLY FOR A JOB
    // ----------------------------------------------------

    @PostMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApplicationResponse apply(
            @PathVariable Long jobId,
            @Valid @RequestBody ApplicationRequest request,
            @AuthenticationPrincipal UserAccount applicant) {

        if (applicant == null || applicant.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in as a job seeker"
            );
        }

        JobPosting job = findJob(jobId);

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This job is no longer accepting applications"
            );
        }

        if (applicationRepository.existsByJobIdAndApplicantId(
                jobId, applicant.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already applied for this job"
            );
        }

        JobApplication application = new JobApplication();

        application.setJob(job);
        application.setApplicant(applicant);
        application.setCoverLetter(request.coverLetter());

        JobApplication saved = applicationRepository.saveAndFlush(application);

        // Build response before triggering external notifications.
        ApplicationResponse response = toResponse(saved);

        // Publish live updates independently.
        try {
            liveUpdates.publishApplicationUpdate(applicant.getId());
            liveUpdates.publishApplicationUpdate(job.getEmployer().getId());

        } catch (RuntimeException exception) {
            logger.error(
                    "Failed to publish live updates for application {}",
                    saved.getId(),
                    exception
            );
        }

        // Send email notifications independently.
        try {
            emailNotifications.ifAvailable(notifications ->
                    notifications.applicationSubmitted(
                            applicant.getEmail(),
                            applicant.getFullName(),
                            job.getEmployer().getEmail(),
                            job.getEmployer().getCompanyName() == null
                                    ? job.getEmployer().getFullName()
                                    : job.getEmployer().getCompanyName(),
                            job.getTitle()
                    )
            );

        } catch (RuntimeException exception) {
            logger.error(
                    "Failed to trigger email notification for application {}",
                    saved.getId(),
                    exception
            );
        }

        return response;
    }

    // ----------------------------------------------------
    // GET MY APPLICATIONS
    // ----------------------------------------------------

    @GetMapping("/applications/me")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @Transactional(readOnly = true)
    public List<ApplicationResponse> myApplications(
            @AuthenticationPrincipal UserAccount applicant) {

        if (applicant == null || applicant.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return applicationRepository
                .findByApplicantIdOrderByAppliedAtDesc(applicant.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ----------------------------------------------------
    // GET APPLICATIONS FOR A JOB
    // ----------------------------------------------------

    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasAnyRole('EMPLOYER','ADMIN')")
    @Transactional(readOnly = true)
    public List<ApplicationResponse> applicationsForJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserAccount currentUser) {

        JobPosting job = findJob(jobId);

        verifyOwnerOrAdmin(job, currentUser);

        return applicationRepository
                .findByJobIdOrderByAppliedAtDesc(jobId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ----------------------------------------------------
    // UPDATE APPLICATION STATUS
    // ----------------------------------------------------

    @PatchMapping("/applications/{applicationId}/status")
    @PreAuthorize("hasAnyRole('EMPLOYER','ADMIN')")
    @Transactional
    public ApplicationResponse updateStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody StatusRequest request,
            @AuthenticationPrincipal UserAccount currentUser) {

        JobApplication application = findApplication(applicationId);

        verifyOwnerOrAdmin(application.getJob(), currentUser);

        ApplicationStatus newStatus;

        try {
            newStatus = ApplicationStatus.valueOf(
                    request.status().trim().toUpperCase()
            );

        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid application status"
            );
        }

        application.setStatus(newStatus);

        JobApplication saved = applicationRepository.saveAndFlush(application);

        ApplicationResponse response = toResponse(saved);

        try {
            UserAccount applicant = saved.getApplicant();

            emailNotifications.ifAvailable(notifications ->
                    notifications.applicationStatusChanged(
                            applicant.getEmail(),
                            applicant.getFullName(),
                            saved.getJob().getTitle(),
                            saved.getStatus().name()
                    )
            );

        } catch (RuntimeException exception) {
            logger.error(
                    "Failed to trigger status notification for application {}",
                    saved.getId(),
                    exception
            );
        }

        return response;
    }

    // ----------------------------------------------------
    // WITHDRAW APPLICATION
    // ----------------------------------------------------

    @PatchMapping("/applications/{applicationId}/withdraw")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @Transactional
    public ApplicationResponse withdraw(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserAccount applicant) {

        if (applicant == null || applicant.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        JobApplication application = findApplication(applicationId);

        if (!application.getApplicant().getId().equals(applicant.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot withdraw another user's application"
            );
        }

        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Application is already withdrawn"
            );
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);

        JobApplication saved = applicationRepository.saveAndFlush(application);

        ApplicationResponse response = toResponse(saved);

        try {
            UserAccount employer = saved.getJob().getEmployer();

            emailNotifications.ifAvailable(notifications ->
                    notifications.applicationWithdrawn(
                            employer.getEmail(),
                            employer.getCompanyName() == null
                                    ? employer.getFullName()
                                    : employer.getCompanyName(),
                            applicant.getFullName(),
                            saved.getJob().getTitle()
                    )
            );

            liveUpdates.publishApplicationUpdate(employer.getId());

        } catch (RuntimeException exception) {
            logger.error(
                    "Failed to trigger withdrawal notifications for application {}",
                    saved.getId(),
                    exception
            );
        }

        return response;
    }

    // ----------------------------------------------------
    // FIND JOB
    // ----------------------------------------------------

    private JobPosting findJob(Long id) {

        return jobRepository.findWithEmployerById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job not found"
                ));
    }

    // ----------------------------------------------------
    // FIND APPLICATION
    // ----------------------------------------------------

    private JobApplication findApplication(Long id) {

        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found"
                ));
    }

    // ----------------------------------------------------
    // VERIFY JOB OWNER OR ADMIN
    // ----------------------------------------------------

    private void verifyOwnerOrAdmin(
            JobPosting job,
            UserAccount currentUser) {

        if (currentUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (job.getEmployer() == null ||
                !job.getEmployer().getId().equals(currentUser.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to manage applications for this job"
            );
        }
    }

    // ----------------------------------------------------
    // CONVERT ENTITY TO RESPONSE DTO
    // ----------------------------------------------------

    private ApplicationResponse toResponse(JobApplication application) {

        UserAccount applicant = application.getApplicant();

        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                applicant.getId(),
                applicant.getFullName(),
                applicant.getEmail(),
                applicant.getResumeUrl(),
                application.getCoverLetter(),
                application.getStatus(),
                application.getAppliedAt()
        );
    }
}
