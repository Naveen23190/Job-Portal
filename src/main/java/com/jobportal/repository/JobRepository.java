package com.jobportal.repository;

import com.jobportal.model.JobPosting;
import com.jobportal.model.JobStatus;
import com.jobportal.model.WorkMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<JobPosting, Long> {
    @EntityGraph(attributePaths = "employer")
    @Query(value = """
            select j from JobPosting j
            where j.status = :status
              and (:keyword = ''
                   or lower(j.title) like lower(concat('%', :keyword, '%'))
                   or lower(j.description) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(j.requirements, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(j.employer.companyName, '')) like lower(concat('%', :keyword, '%')))
              and (:location = '' or lower(j.location) like lower(concat('%', :location, '%')))
              and (:workMode is null or j.workMode = :workMode)
              and (:employmentType = '' or lower(j.employmentType) = lower(:employmentType))
              and (:experienceYears is null
                   or (j.minExperienceYears is null or j.minExperienceYears <= :experienceYears)
                      and (j.maxExperienceYears is null or j.maxExperienceYears >= :experienceYears))
              and (:minSalaryLpa is null
                   or coalesce(j.salaryMaxLpa, j.salaryMinLpa) >= :minSalaryLpa)
              and (:maxSalaryLpa is null
                   or coalesce(j.salaryMinLpa, j.salaryMaxLpa) <= :maxSalaryLpa)
              and (:minStipendMonthly is null
                   or coalesce(j.stipendMaxMonthly, j.stipendMinMonthly) >= :minStipendMonthly)
              and (:maxStipendMonthly is null
                   or coalesce(j.stipendMinMonthly, j.stipendMaxMonthly) <= :maxStipendMonthly)
            order by j.createdAt desc, j.id desc
            """,
            countQuery = """
                    select count(j) from JobPosting j
                    where j.status = :status
                      and (:keyword = ''
                           or lower(j.title) like lower(concat('%', :keyword, '%'))
                           or lower(j.description) like lower(concat('%', :keyword, '%'))
                           or lower(coalesce(j.requirements, '')) like lower(concat('%', :keyword, '%'))
                           or lower(coalesce(j.employer.companyName, '')) like lower(concat('%', :keyword, '%')))
                      and (:location = '' or lower(j.location) like lower(concat('%', :location, '%')))
                      and (:workMode is null or j.workMode = :workMode)
                      and (:employmentType = '' or lower(j.employmentType) = lower(:employmentType))
                      and (:experienceYears is null
                           or (j.minExperienceYears is null or j.minExperienceYears <= :experienceYears)
                              and (j.maxExperienceYears is null or j.maxExperienceYears >= :experienceYears))
                      and (:minSalaryLpa is null
                           or coalesce(j.salaryMaxLpa, j.salaryMinLpa) >= :minSalaryLpa)
                      and (:maxSalaryLpa is null
                           or coalesce(j.salaryMinLpa, j.salaryMaxLpa) <= :maxSalaryLpa)
                      and (:minStipendMonthly is null
                           or coalesce(j.stipendMaxMonthly, j.stipendMinMonthly) >= :minStipendMonthly)
                      and (:maxStipendMonthly is null
                           or coalesce(j.stipendMinMonthly, j.stipendMaxMonthly) <= :maxStipendMonthly)
                    """)
    Page<JobPosting> searchOpenJobs(
            @Param("status") JobStatus status,
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("workMode") WorkMode workMode,
            @Param("employmentType") String employmentType,
            @Param("experienceYears") Integer experienceYears,
            @Param("minSalaryLpa") BigDecimal minSalaryLpa,
            @Param("maxSalaryLpa") BigDecimal maxSalaryLpa,
            @Param("minStipendMonthly") BigDecimal minStipendMonthly,
            @Param("maxStipendMonthly") BigDecimal maxStipendMonthly,
            Pageable pageable);

    @EntityGraph(attributePaths = "employer")
    Page<JobPosting> findByStatusAndIdInOrderByCreatedAtDescIdDesc(
            JobStatus status, List<Long> ids, Pageable pageable);

    @EntityGraph(attributePaths = "employer")
    List<JobPosting> findByEmployer_IdOrderByCreatedAtDesc(Long employerId);

    Optional<JobPosting> findByTitleAndEmployer_Id(String title, Long employerId);

    @Override
    @EntityGraph(attributePaths = "employer")
    Optional<JobPosting> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "employer")
    List<JobPosting> findAll();

    @EntityGraph(attributePaths = {"employer"})
    Optional<JobPosting> findWithEmployerById(Long id);
}
