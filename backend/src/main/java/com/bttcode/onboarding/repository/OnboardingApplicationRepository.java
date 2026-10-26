package com.bttcode.onboarding.repository;

import com.bttcode.onboarding.domain.OnboardingApplication;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OnboardingApplicationRepository
        extends JpaRepository<OnboardingApplication, UUID>, JpaSpecificationExecutor<OnboardingApplication> {

    List<OnboardingApplication> findByApplicantSubOrderBySubmittedAtDesc(String applicantSub);
}
