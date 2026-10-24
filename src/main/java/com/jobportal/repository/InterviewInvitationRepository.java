package com.jobportal.repository;

import com.jobportal.model.InterviewInvitation;
import com.jobportal.model.InterviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterviewInvitationRepository extends JpaRepository<InterviewInvitation, Long> {
    @EntityGraph(attributePaths = {"application", "application.job", "application.applicant"})
    List<InterviewInvitation> findByApplication_Applicant_IdOrderByProposedAtDesc(Long applicantId);

    @EntityGraph(attributePaths = {"application", "application.job", "application.applicant"})
    List<InterviewInvitation> findByApplication_Job_Employer_IdOrderByProposedAtDesc(Long employerId);

    boolean existsByApplication_IdAndStatus(Long applicationId, InterviewStatus status);

    @Override
    @EntityGraph(attributePaths = {"application", "application.job", "application.applicant"})
    Optional<InterviewInvitation> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InterviewInvitation i where i.id = :id")
    Optional<InterviewInvitation> findByIdForUpdate(@Param("id") Long id);
}
