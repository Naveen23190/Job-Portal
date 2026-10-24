package com.jobportal.controller;

import com.jobportal.dto.JobRequest;
import com.jobportal.dto.JobResponse;
import com.jobportal.model.JobPosting;
import com.jobportal.model.JobStatus;
import com.jobportal.model.Role;
import com.jobportal.model.UserAccount;
import com.jobportal.model.WorkMode;
import com.jobportal.repository.JobRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobRepository jobRepository;

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public Page<JobResponse> listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) Integer experienceYears,
            @RequestParam(required = false) BigDecimal minSalaryLpa,
            @RequestParam(required = false) BigDecimal maxSalaryLpa,
            @RequestParam(required = false) BigDecimal minStipendMonthly,
            @RequestParam(required = false) BigDecimal maxStipendMonthly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        Pageable pageable = validatePage(page, size);
        if (experienceYears != null && (experienceYears < 0 || experienceYears > 50)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Experience must be between 0 and 50 years");
        }
        if (minSalaryLpa != null && minSalaryLpa.signum() < 0
                || maxSalaryLpa != null && maxSalaryLpa.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary filters cannot be negative");
        }
        if (minSalaryLpa != null && minSalaryLpa.compareTo(new BigDecimal("1000")) > 0
                || maxSalaryLpa != null && maxSalaryLpa.compareTo(new BigDecimal("1000")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary filters cannot exceed 1000 LPA");
        }
        if (minSalaryLpa != null && maxSalaryLpa != null
                && minSalaryLpa.compareTo(maxSalaryLpa) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum salary filter cannot exceed maximum salary filter");
        }
        if (minStipendMonthly != null && minStipendMonthly.signum() < 0
                || maxStipendMonthly != null && maxStipendMonthly.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stipend filters cannot be negative");
        }
        if (minStipendMonthly != null && maxStipendMonthly != null
                && minStipendMonthly.compareTo(maxStipendMonthly) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum stipend filter cannot exceed maximum stipend filter");
        }
        return jobRepository.searchOpenJobs(JobStatus.OPEN, normalize(keyword), normalize(location),
                        workMode, normalize(employmentType), experienceYears, minSalaryLpa, maxSalaryLpa,
                        minStipendMonthly, maxStipendMonthly, pageable)
                .map(this::toResponse);
    }

    @GetMapping("/saved")
    public Page<JobResponse> listSavedJobs(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        Pageable pageable = validatePage(page, size);
        if (ids == null || ids.isEmpty()) return Page.empty(pageable);
        if (ids.size() > 100 || ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saved job IDs are invalid");
        }
        return jobRepository.findByStatusAndIdInOrderByCreatedAtDescIdDesc(JobStatus.OPEN, ids, pageable)
                .map(this::toResponse);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<JobResponse> myJobs(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserAccount user) {
        List<JobPosting> postings = user.getRole() == Role.ADMIN
                ? jobRepository.findAll()
                : jobRepository.findByEmployer_IdOrderByCreatedAtDesc(user.getId());
        return postings.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable Long id) {
        return toResponse(findJob(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse createJob(@Valid @RequestBody JobRequest request,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal UserAccount user) {
        JobPosting job = new JobPosting();
        applyRequest(job, request);
        job.setEmployer(user);
        job.setStatus(JobStatus.OPEN);
        return toResponse(jobRepository.save(job));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public JobResponse updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest request,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal UserAccount user) {
        JobPosting job = findJob(id);
        verifyOwnerOrAdmin(job, user);
        applyRequest(job, request);
        return toResponse(jobRepository.save(job));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public JobResponse updateJobStatus(@PathVariable Long id, @RequestParam JobStatus status,
                                       @org.springframework.security.core.annotation.AuthenticationPrincipal UserAccount user) {
        JobPosting job = findJob(id);
        verifyOwnerOrAdmin(job, user);
        job.setStatus(status);
        return toResponse(jobRepository.save(job));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeJob(@PathVariable Long id,
                         @org.springframework.security.core.annotation.AuthenticationPrincipal UserAccount user) {
        JobPosting job = findJob(id);
        verifyOwnerOrAdmin(job, user);
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);
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

    private void applyRequest(JobPosting job, JobRequest request) {
        boolean internship = "Internship".equalsIgnoreCase(request.employmentType().trim());
        if (request.minExperienceYears() != null && request.maxExperienceYears() != null
                && request.minExperienceYears() > request.maxExperienceYears()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum experience cannot exceed maximum experience");
        }
        if (request.salaryMinLpa() != null && request.salaryMaxLpa() != null
                && request.salaryMinLpa().compareTo(request.salaryMaxLpa()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum annual CTC cannot be greater than maximum annual CTC");
        }
        if (request.stipendMinMonthly() != null && request.stipendMaxMonthly() != null
                && request.stipendMinMonthly().compareTo(request.stipendMaxMonthly()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum monthly stipend cannot be greater than maximum monthly stipend");
        }
        if (internship && (request.salaryMinLpa() != null || request.salaryMaxLpa() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Internships must use a monthly stipend instead of annual CTC");
        }
        if (!internship && (request.stipendMinMonthly() != null || request.stipendMaxMonthly() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Monthly stipend fields are only valid for internship postings");
        }
        job.setTitle(request.title().trim());
        job.setLocation(request.location().trim());
        job.setEmploymentType(request.employmentType().trim());
        job.setDescription(request.description().trim());
        job.setRequirements(request.requirements());
        job.setSalaryMinLpa(request.salaryMinLpa());
        job.setSalaryMaxLpa(request.salaryMaxLpa());
        job.setStipendMinMonthly(request.stipendMinMonthly());
        job.setStipendMaxMonthly(request.stipendMaxMonthly());
        job.setWorkMode(request.workMode() == null ? inferWorkMode(request.location()) : request.workMode());
        job.setMinExperienceYears(request.minExperienceYears());
        job.setMaxExperienceYears(request.maxExperienceYears());
    }

    private WorkMode inferWorkMode(String location) {
        String normalized = location.toLowerCase(Locale.ROOT);
        if (normalized.contains("remote")) return WorkMode.REMOTE;
        if (normalized.contains("hybrid")) return WorkMode.HYBRID;
        return WorkMode.ONSITE;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private Pageable validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and page size must be between 1 and 24");
        }
        return PageRequest.of(page, size);
    }

    private JobResponse toResponse(JobPosting job) {
        return new JobResponse(job.getId(), job.getTitle(), job.getLocation(), job.getEmploymentType(),
                job.getDescription(), job.getRequirements(), job.getSalaryMinLpa(), job.getSalaryMaxLpa(),
                job.getStipendMinMonthly(), job.getStipendMaxMonthly(), job.getWorkMode(),
                job.getMinExperienceYears(), job.getMaxExperienceYears(), job.getStatus(), job.getEmployer().getId(),
                job.getEmployer().getCompanyName(), job.getCreatedAt());
    }
}
