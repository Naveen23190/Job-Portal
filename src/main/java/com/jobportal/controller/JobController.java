
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
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRepository jobRepository;

    private static final int MAX_PAGE_SIZE = 24;
    private static final int MAX_EXPERIENCE = 50;

    private static final BigDecimal MAX_SALARY =
            new BigDecimal("1000");

    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "createdAt",
            "title",
            "location",
            "salaryMinLpa",
            "salaryMaxLpa",
            "minExperienceYears"
    );

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // --------------------------------------------------
    // GET ALL OPEN JOBS WITH SEARCH, FILTER AND PAGINATION
    // --------------------------------------------------

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

            @RequestParam(defaultValue = "9") int size,

            @RequestParam(defaultValue = "createdAt") String sortBy,

            @RequestParam(defaultValue = "desc") String direction
    ) {

        Pageable pageable = validatePage(
                page, size, sortBy, direction
        );

        validateExperience(experienceYears);

        validateSalaryFilters(
                minSalaryLpa, maxSalaryLpa
        );

        validateStipendFilters(
                minStipendMonthly, maxStipendMonthly
        );

        return jobRepository.searchOpenJobs(

                JobStatus.OPEN,

                normalize(keyword),

                normalize(location),

                workMode,

                normalize(employmentType),

                experienceYears,

                minSalaryLpa,

                maxSalaryLpa,

                minStipendMonthly,

                maxStipendMonthly,

                pageable

        ).map(this::toResponse);
    }

    // --------------------------------------------------
    // GET SAVED JOBS
    // --------------------------------------------------

    @GetMapping("/saved")
    public Page<JobResponse> listSavedJobs(

            @RequestParam(required = false) List<Long> ids,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "9") int size,

            @RequestParam(defaultValue = "createdAt") String sortBy,

            @RequestParam(defaultValue = "desc") String direction
    ) {

        Pageable pageable = validatePage(
                page, size, sortBy, direction
        );

        if (ids == null || ids.isEmpty()) {
            return Page.empty(pageable);
        }

        if (ids.size() > 100 ||
                ids.stream().anyMatch(id -> id == null || id <= 0)) {

            throw badRequest("Saved job IDs are invalid");
        }

        return jobRepository
                .findByStatusAndIdInOrderByCreatedAtDescIdDesc(
                        JobStatus.OPEN,
                        ids,
                        pageable
                )
                .map(this::toResponse);
    }

    // --------------------------------------------------
    // GET EMPLOYER'S JOBS
    // --------------------------------------------------

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<JobResponse> myJobs(
            @AuthenticationPrincipal UserAccount user
    ) {

        validateAuthenticatedUser(user);

        List<JobPosting> postings;

        if (user.getRole() == Role.ADMIN) {

            postings = jobRepository.findAll(
                    Sort.by(Sort.Direction.DESC, "createdAt")
                            .and(Sort.by(Sort.Direction.DESC, "id"))
            );

        } else {

            postings = jobRepository
                    .findByEmployer_IdOrderByCreatedAtDesc(
                            user.getId()
                    );
        }

        return postings.stream()
                .map(this::toResponse)
                .toList();
    }

    // --------------------------------------------------
    // GET JOB BY ID
    // --------------------------------------------------

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable Long id) {

        validateJobId(id);

        JobPosting job = findJob(id);

        return toResponse(job);
    }

    // --------------------------------------------------
    // CREATE JOB
    // --------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse createJob(

            @Valid @RequestBody JobRequest request,

            @AuthenticationPrincipal UserAccount user
    ) {

        validateAuthenticatedUser(user);

        JobPosting job = new JobPosting();

        applyRequest(job, request);

        job.setEmployer(user);

        job.setStatus(JobStatus.OPEN);

        JobPosting savedJob = jobRepository.save(job);

        return toResponse(savedJob);
    }

    // --------------------------------------------------
    // UPDATE JOB
    // --------------------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public JobResponse updateJob(

            @PathVariable Long id,

            @Valid @RequestBody JobRequest request,

            @AuthenticationPrincipal UserAccount user
    ) {

        validateJobId(id);

        validateAuthenticatedUser(user);

        JobPosting job = findJob(id);

        verifyOwnerOrAdmin(job, user);

        applyRequest(job, request);

        JobPosting updatedJob = jobRepository.save(job);

        return toResponse(updatedJob);
    }

    // --------------------------------------------------
    // UPDATE JOB STATUS
    // --------------------------------------------------

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public JobResponse updateJobStatus(

            @PathVariable Long id,

            @RequestParam JobStatus status,

            @AuthenticationPrincipal UserAccount user
    ) {

        validateJobId(id);

        validateAuthenticatedUser(user);

        if (status == null) {
            throw badRequest("Job status is required");
        }

        JobPosting job = findJob(id);

        verifyOwnerOrAdmin(job, user);

        job.setStatus(status);

        return toResponse(jobRepository.save(job));
    }

    // --------------------------------------------------
    // CLOSE JOB
    // --------------------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeJob(

            @PathVariable Long id,

            @AuthenticationPrincipal UserAccount user
    ) {

        validateJobId(id);

        validateAuthenticatedUser(user);

        JobPosting job = findJob(id);

        verifyOwnerOrAdmin(job, user);

        job.setStatus(JobStatus.CLOSED);

        jobRepository.save(job);
    }

    // ==================================================
    // PRIVATE HELPER METHODS
    // ==================================================

    // Find job by ID

    private JobPosting findJob(Long id) {

        return jobRepository.findById(id)

                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Job not found"
                        )
                );
    }

    // Validate job ID

    private void validateJobId(Long id) {

        if (id == null || id <= 0) {
            throw badRequest("Invalid job ID");
        }
    }

    // Validate authenticated user

    private void validateAuthenticatedUser(UserAccount user) {

        if (user == null || user.getId() == null ||
                user.getRole() == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }
    }

    // Verify employer ownership or admin access

    private void verifyOwnerOrAdmin(
            JobPosting job,
            UserAccount user
    ) {

        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (job.getEmployer() == null ||
                job.getEmployer().getId() == null ||
                !job.getEmployer().getId().equals(user.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not manage this job"
            );
        }
    }

    // Apply request data to job entity

    private void applyRequest(
            JobPosting job,
            JobRequest request
    ) {

        if (request == null) {
            throw badRequest("Job request cannot be empty");
        }

        String title = requireText(
                request.title(), "Job title"
        );

        String location = requireText(
                request.location(), "Job location"
        );

        String employmentType = requireText(
                request.employmentType(), "Employment type"
        );

        String description = requireText(
                request.description(), "Job description"
        );

        boolean internship =
                "Internship".equalsIgnoreCase(employmentType);

        validateExperienceRange(
                request.minExperienceYears(),
                request.maxExperienceYears()
        );

        validateSalaryFilters(
                request.salaryMinLpa(),
                request.salaryMaxLpa()
        );

        validateStipendFilters(
                request.stipendMinMonthly(),
                request.stipendMaxMonthly()
        );

        // Internship must use monthly stipend

        if (internship &&
                (request.salaryMinLpa() != null ||
                 request.salaryMaxLpa() != null)) {

            throw badRequest(
                    "Internships must use a monthly stipend instead of annual CTC"
            );
        }

        // Regular jobs must use annual salary

        if (!internship &&
                (request.stipendMinMonthly() != null ||
                 request.stipendMaxMonthly() != null)) {

            throw badRequest(
                    "Monthly stipend fields are only valid for internship postings"
            );
        }

        job.setTitle(title);

        job.setLocation(location);

        job.setEmploymentType(employmentType);

        job.setDescription(description);

        job.setRequirements(
                request.requirements() == null
                        ? null
                        : request.requirements().trim()
        );

        job.setSalaryMinLpa(request.salaryMinLpa());

        job.setSalaryMaxLpa(request.salaryMaxLpa());

        job.setStipendMinMonthly(
                request.stipendMinMonthly()
        );

        job.setStipendMaxMonthly(
                request.stipendMaxMonthly()
        );

        job.setWorkMode(
                request.workMode() != null
                        ? request.workMode()
                        : inferWorkMode(location)
        );

        job.setMinExperienceYears(
                request.minExperienceYears()
        );

        job.setMaxExperienceYears(
                request.maxExperienceYears()
        );
    }

    // Validate mandatory text fields

    private String requireText(String value, String fieldName) {

        if (value == null || value.isBlank()) {

            throw badRequest(
                    fieldName + " cannot be empty"
            );
        }

        return value.trim();
    }

    // Validate experience range

    private void validateExperience(Integer experienceYears) {

        if (experienceYears != null &&
                (experienceYears < 0 ||
                 experienceYears > MAX_EXPERIENCE)) {

            throw badRequest(
                    "Experience must be between 0 and 50 years"
            );
        }
    }

    // Validate minimum and maximum experience

    private void validateExperienceRange(
            Integer minExperience,
            Integer maxExperience
    ) {

        validateExperience(minExperience);

        validateExperience(maxExperience);

        if (minExperience != null &&
                maxExperience != null &&
                minExperience > maxExperience) {

            throw badRequest(
                    "Minimum experience cannot exceed maximum experience"
            );
        }
    }

    // Validate annual salary filters

    private void validateSalaryFilters(
            BigDecimal minSalary,
            BigDecimal maxSalary
    ) {

        if (isNegative(minSalary) || isNegative(maxSalary)) {

            throw badRequest(
                    "Salary filters cannot be negative"
            );
        }

        if (exceedsSalaryLimit(minSalary) ||
                exceedsSalaryLimit(maxSalary)) {

            throw badRequest(
                    "Salary filters cannot exceed 1000 LPA"
            );
        }

        if (minSalary != null &&
                maxSalary != null &&
                minSalary.compareTo(maxSalary) > 0) {

            throw badRequest(
                    "Minimum salary cannot exceed maximum salary"
            );
        }
    }

    // Validate monthly stipend filters

    private void validateStipendFilters(
            BigDecimal minStipend,
            BigDecimal maxStipend
    ) {

        if (isNegative(minStipend) ||
                isNegative(maxStipend)) {

            throw badRequest(
                    "Stipend filters cannot be negative"
            );
        }

        if (minStipend != null &&
                maxStipend != null &&
                minStipend.compareTo(maxStipend) > 0) {

            throw badRequest(
                    "Minimum stipend cannot exceed maximum stipend"
            );
        }
    }

    // Check negative monetary values

    private boolean isNegative(BigDecimal value) {

        return value != null && value.signum() < 0;
    }

    // Check maximum salary

    private boolean exceedsSalaryLimit(BigDecimal value) {

        return value != null &&
                value.compareTo(MAX_SALARY) > 0;
    }

    // Infer work mode from location

    private WorkMode inferWorkMode(String location) {

        String normalized = location.toLowerCase(Locale.ROOT);

        if (normalized.contains("remote")) {
            return WorkMode.REMOTE;
        }

        if (normalized.contains("hybrid")) {
            return WorkMode.HYBRID;
        }

        return WorkMode.ONSITE;
    }

    // Normalize optional search parameters

    private String normalize(String value) {

        return value == null ? "" : value.trim();
    }

    // Validate pagination and sorting

    private Pageable validatePage(
            int page,
            int size,
            String sortBy,
            String direction
    ) {

        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {

            throw badRequest(
                    "Page must be non-negative and page size must be between 1 and 24"
            );
        }

        String normalizedSort = normalize(sortBy);

        if (!ALLOWED_SORT_FIELDS.contains(normalizedSort)) {

            throw badRequest(
                    "Invalid sort field. Allowed fields: " +
                            String.join(", ", ALLOWED_SORT_FIELDS)
            );
        }

        Sort.Direction sortDirection;

        try {

            sortDirection = Sort.Direction.fromString(
                    normalize(direction)
            );

        } catch (IllegalArgumentException exception) {

            throw badRequest(
                    "Sort direction must be ASC or DESC"
            );
        }

        Sort sort = Sort.by(
                sortDirection,
                normalizedSort
        );

        if ("createdAt".equals(normalizedSort)) {

            sort = sort.and(
                    Sort.by(sortDirection, "id")
            );
        }

        return PageRequest.of(page, size, sort);
    }

    // Create BAD_REQUEST exception

    private ResponseStatusException badRequest(String message) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    // Convert entity into response DTO

    private JobResponse toResponse(JobPosting job) {

        UserAccount employer = job.getEmployer();

        return new JobResponse(

                job.getId(),

                job.getTitle(),

                job.getLocation(),

                job.getEmploymentType(),

                job.getDescription(),

                job.getRequirements(),

                job.getSalaryMinLpa(),

                job.getSalaryMaxLpa(),

                job.getStipendMinMonthly(),

                job.getStipendMaxMonthly(),

                job.getWorkMode(),

                job.getMinExperienceYears(),

                job.getMaxExperienceYears(),

                job.getStatus(),

                employer != null ? employer.getId() : null,

                employer != null ? employer.getCompanyName() : null,

                job.getCreatedAt()
        );
    }
}
