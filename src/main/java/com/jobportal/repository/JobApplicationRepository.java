package com.jobportal.repository;

import com.jobportal.model.JobApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    boolean existsByJobIdAndApplicantId(Long jobId, Long applicantId);

    @EntityGraph(attributePaths = {"job", "applicant"})
    List<JobApplication> findByApplicantIdOrderByAppliedAtDesc(Long applicantId);

    @EntityGraph(attributePaths = {"job", "applicant"})
    List<JobApplication> findByJobIdOrderByAppliedAtDesc(Long jobId);

    @Override
    @EntityGraph(attributePaths = {"job.employer", "applicant"})
    Optional<JobApplication> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from JobApplication a where a.id = :id")
    Optional<JobApplication> findByIdForUpdate(@Param("id") Long id);
}
