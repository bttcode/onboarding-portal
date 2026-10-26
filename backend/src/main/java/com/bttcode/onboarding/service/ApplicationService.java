package com.bttcode.onboarding.service;

import com.bttcode.onboarding.api.model.ApplicationDetail;
import com.bttcode.onboarding.api.model.ApplicationPage;
import com.bttcode.onboarding.api.model.ApplicationSummary;
import com.bttcode.onboarding.config.OnboardingProperties;
import com.bttcode.onboarding.domain.ApplicantDetails;
import com.bttcode.onboarding.domain.ApplicationDocument;
import com.bttcode.onboarding.domain.ApplicationStatus;
import com.bttcode.onboarding.domain.BusinessRuleException;
import com.bttcode.onboarding.domain.DecisionType;
import com.bttcode.onboarding.domain.OnboardingApplication;
import com.bttcode.onboarding.domain.OutboxEvent;
import com.bttcode.onboarding.repository.ApplicationDocumentRepository;
import com.bttcode.onboarding.repository.OnboardingApplicationRepository;
import com.bttcode.onboarding.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases of the portal. Each public method is one transaction and returns API models, so lazy
 * associations are mapped while the persistence context is still open.
 */
@Service
@Transactional
public class ApplicationService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "submittedAt", "submittedAt",
            "lastName", "lastName",
            "status", "status");
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MINIMUM_AGE = 18;

    private final OnboardingApplicationRepository applications;
    private final ApplicationDocumentRepository documents;
    private final OutboxEventRepository outbox;
    private final ApplicationMapper mapper;
    private final OnboardingProperties properties;
    private final Clock clock;

    public ApplicationService(OnboardingApplicationRepository applications, ApplicationDocumentRepository documents,
                              OutboxEventRepository outbox, ApplicationMapper mapper,
                              OnboardingProperties properties, Clock clock) {
        this.applications = applications;
        this.documents = documents;
        this.outbox = outbox;
        this.mapper = mapper;
        this.properties = properties;
        this.clock = clock;
    }

    /** Saves the application and its outbox event in one transaction: both are stored, or neither. */
    public ApplicationDetail submit(CurrentUser user, ApplicantDetails details) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        if (details.birthDate().isAfter(today.minusYears(MINIMUM_AGE))) {
            throw new InvalidRequestException("The applicant must be at least " + MINIMUM_AGE + " years old.");
        }
        if (details.startDate().isBefore(today)) {
            throw new InvalidRequestException("The insurance start date cannot be in the past.");
        }
        Instant now = clock.instant();
        OnboardingApplication app = OnboardingApplication.submit(user.subject(), details, now);
        applications.save(app);
        outbox.save(OutboxEvent.sanctionsCheckRequested(app.getId(), now));
        return mapper.toDetail(app, properties.requiredApprovals());
    }

    @Transactional(readOnly = true)
    public List<ApplicationSummary> mine(CurrentUser user) {
        return applications.findByApplicantSubOrderBySubmittedAtDesc(user.subject()).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationPage search(ApplicationStatus status, String query, Integer page, Integer size, String sort) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE + ".");
        }
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, parseSort(sort));
        Page<OnboardingApplication> result = applications.findAll(matching(status, query), pageRequest);
        return mapper.toPage(result);
    }

    @Transactional(readOnly = true)
    public ApplicationDetail get(CurrentUser user, UUID id) {
        return mapper.toDetail(loadVisible(user, id), properties.requiredApprovals());
    }

    public ApplicationDetail decide(CurrentUser user, UUID id, DecisionType type, String comment) {
        OnboardingApplication app = load(id);
        if (app.isOwnedBy(user.subject())) {
            throw new BusinessRuleException("OWN_APPLICATION", "You cannot decide on your own application.");
        }
        app.decide(user.username(), type, comment, properties.requiredApprovals(), clock.instant());
        // flush now so a concurrent decision surfaces as a conflict inside this request
        applications.flush();
        return mapper.toDetail(app, properties.requiredApprovals());
    }

    public ApplicationDetail retryCheck(UUID id) {
        OnboardingApplication app = load(id);
        app.requestRecheck();
        outbox.save(OutboxEvent.sanctionsCheckRequested(app.getId(), clock.instant()));
        return mapper.toDetail(app, properties.requiredApprovals());
    }

    public void uploadDocument(CurrentUser user, UUID id, String originalName, byte[] content) {
        OnboardingApplication app = loadVisible(user, id);
        if (!app.isOwnedBy(user.subject())) {
            throw new NotFoundException("Application", id);
        }
        if (!app.acceptsDocuments()) {
            throw new BusinessRuleException("DECIDED",
                    "The application is already decided; documents can no longer be changed.");
        }
        if (content.length == 0) {
            throw new InvalidRequestException("The file is empty.");
        }
        if (content.length > properties.upload().maxBytes()) {
            throw new InvalidRequestException("The file is larger than " + properties.upload().maxBytes() + " bytes.");
        }
        String mediaType = FileSignatures.detectMediaType(content)
                .orElseThrow(() -> new InvalidRequestException("Only PDF, PNG and JPEG files are accepted."));
        String fileName = FileSignatures.sanitizeFileName(originalName);
        Instant now = clock.instant();

        documents.findById(id).ifPresentOrElse(
                existing -> existing.replace(fileName, mediaType, content, now),
                () -> documents.save(new ApplicationDocument(id, fileName, mediaType, content, now)));
        app.attachDocument(fileName);
    }

    @Transactional(readOnly = true)
    public ApplicationDocument document(CurrentUser user, UUID id) {
        loadVisible(user, id);
        return documents.findById(id).orElseThrow(() -> new NotFoundException("Document of application", id));
    }

    private OnboardingApplication load(UUID id) {
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application", id));
    }

    /** Caseworkers see every application; applicants only their own. */
    private OnboardingApplication loadVisible(CurrentUser user, UUID id) {
        OnboardingApplication app = load(id);
        if (!user.isCaseworker() && !app.isOwnedBy(user.subject())) {
            throw new NotFoundException("Application", id);
        }
        return app;
    }

    static Sort parseSort(String sort) {
        String value = sort == null || sort.isBlank() ? "submittedAt,asc" : sort;
        String[] parts = value.split(",");
        String field = SORTABLE_FIELDS.get(parts[0]);
        if (field == null || parts.length != 2) {
            throw new InvalidRequestException("Unsupported sort '" + value + "'. Use one of "
                    + SORTABLE_FIELDS.keySet() + " followed by ,asc or ,desc.");
        }
        Sort.Direction direction = switch (parts[1].toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new InvalidRequestException("Sort direction must be asc or desc.");
        };
        // the id tie-breaker keeps paging stable when many rows share the same sort value
        return Sort.by(direction, field).and(Sort.by("id"));
    }

    private static Specification<OnboardingApplication> matching(ApplicationStatus status, String query) {
        return (root, criteriaQuery, cb) -> {
            var predicate = cb.conjunction();
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (query != null && !query.isBlank()) {
                String pattern = "%" + escapeLike(query.strip().toLowerCase(Locale.ROOT)) + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("lastName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("email")), pattern, '\\')));
            }
            return predicate;
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
