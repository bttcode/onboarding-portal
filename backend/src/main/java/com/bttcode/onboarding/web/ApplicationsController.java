package com.bttcode.onboarding.web;

import com.bttcode.onboarding.api.ApplicationsApi;
import com.bttcode.onboarding.api.model.ApplicantData;
import com.bttcode.onboarding.api.model.ApplicationDetail;
import com.bttcode.onboarding.api.model.ApplicationPage;
import com.bttcode.onboarding.api.model.ApplicationStatus;
import com.bttcode.onboarding.api.model.ApplicationSummary;
import com.bttcode.onboarding.api.model.DecisionRequest;
import com.bttcode.onboarding.domain.ApplicationDocument;
import com.bttcode.onboarding.service.ApplicationMapper;
import com.bttcode.onboarding.service.ApplicationService;
import com.bttcode.onboarding.service.CurrentUserProvider;
import com.bttcode.onboarding.service.InvalidRequestException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Implements the generated {@link ApplicationsApi}. Routing, parameter binding and bean
 * validation come from the contract; this class only adds authorization and delegates.
 */
@RestController
public class ApplicationsController implements ApplicationsApi {

    private final ApplicationService service;
    private final ApplicationMapper mapper;
    private final CurrentUserProvider currentUser;

    public ApplicationsController(ApplicationService service, ApplicationMapper mapper, CurrentUserProvider currentUser) {
        this.service = service;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Override
    @PreAuthorize("hasRole('applicant')")
    public ResponseEntity<ApplicationDetail> createApplication(ApplicantData applicantData) {
        ApplicationDetail created = service.submit(currentUser.get(), mapper.toDetails(applicantData));
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Override
    @PreAuthorize("hasRole('applicant')")
    public ResponseEntity<List<ApplicationSummary>> listMyApplications() {
        return ResponseEntity.ok(service.mine(currentUser.get()));
    }

    @Override
    @PreAuthorize("hasRole('caseworker')")
    public ResponseEntity<ApplicationPage> searchApplications(ApplicationStatus status, String q, Integer page,
                                                              Integer size, String sort) {
        return ResponseEntity.ok(service.search(mapper.toDomain(status), q, page, size, sort));
    }

    @Override
    @PreAuthorize("hasAnyRole('applicant', 'caseworker')")
    public ResponseEntity<ApplicationDetail> getApplication(UUID id) {
        return ResponseEntity.ok(service.get(currentUser.get(), id));
    }

    @Override
    @PreAuthorize("hasRole('caseworker')")
    public ResponseEntity<ApplicationDetail> decideApplication(UUID id, DecisionRequest decisionRequest) {
        return ResponseEntity.ok(service.decide(currentUser.get(), id,
                mapper.toDomain(decisionRequest.getDecision()), decisionRequest.getComment()));
    }

    @Override
    @PreAuthorize("hasRole('caseworker')")
    public ResponseEntity<ApplicationDetail> retryCheck(UUID id) {
        return ResponseEntity.ok(service.retryCheck(id));
    }

    @Override
    @PreAuthorize("hasRole('applicant')")
    public ResponseEntity<Void> uploadDocument(UUID id, MultipartFile file) {
        if (file == null) {
            throw new InvalidRequestException("A file is required.");
        }
        try {
            service.uploadDocument(currentUser.get(), id, file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAnyRole('applicant', 'caseworker')")
    public ResponseEntity<Resource> downloadDocument(UUID id) {
        ApplicationDocument document = service.document(currentUser.get(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .contentLength(document.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new ByteArrayResource(document.getData()));
    }
}
