package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserUsernameOrderBySubmittedAtDesc(String username);
}
