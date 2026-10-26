package com.bttcode.onboarding.repository;

import com.bttcode.onboarding.domain.ApplicationDocument;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, UUID> {
}
