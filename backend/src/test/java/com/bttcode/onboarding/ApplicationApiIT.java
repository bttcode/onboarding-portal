package com.bttcode.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bttcode.onboarding.domain.CheckStatus;
import com.bttcode.onboarding.domain.OutboxEvent;
import com.bttcode.onboarding.repository.OnboardingApplicationRepository;
import com.bttcode.onboarding.repository.OutboxEventRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfigureMockMvc
class ApplicationApiIT extends IntegrationTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Autowired
    private MockMvc mvc;
    @Autowired
    private OnboardingApplicationRepository applications;
    @Autowired
    private OutboxEventRepository outbox;
    @Autowired
    private TransactionTemplate tx;

    @Test
    void anApplicantSubmitsAndTheCheckIsQueued() throws Exception {
        String id = submit(applicant("alice"), "Queued");

        assertThat(outbox.findByAggregateIdOrderByCreatedAt(UUID.fromString(id)))
                .singleElement()
                .extracting(OutboxEvent::getType)
                .isEqualTo(OutboxEvent.SANCTIONS_CHECK_REQUESTED);

        mvc.perform(get("/api/applications/mine").with(applicant("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].status", id).value("SUBMITTED"));
    }

    @Test
    void invalidInputIsRejectedByTheContract() throws Exception {
        String badPostalCode = TestData.applicantJson("Invalid").replace("\"8001\"", "\"80015\"");

        mvc.perform(post("/api/applications").with(applicant("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPostalCode))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousCallersAreRejected() throws Exception {
        mvc.perform(get("/api/applications/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    void applicantsCannotUseTheReviewQueue() throws Exception {
        mvc.perform(get("/api/applications").with(applicant("alice"))).andExpect(status().isForbidden());
    }

    @Test
    void applicantsCannotSeeOtherApplicants() throws Exception {
        String id = submit(applicant("alice"), "Private");

        mvc.perform(get("/api/applications/{id}", id).with(applicant("bob")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/applications/{id}", id).with(caseworker("carol")))
                .andExpect(status().isOk());
    }

    @Test
    void theReviewQueueIsPagedAndSortedOnTheServer() throws Exception {
        String token = "Pg" + System.nanoTime();
        submit(applicant("alice"), token + "Charlie");
        submit(applicant("alice"), token + "Alpha");
        submit(applicant("alice"), token + "Bravo");

        mvc.perform(get("/api/applications").with(caseworker("carol"))
                        .param("q", token).param("size", "2").param("page", "0").param("sort", "lastName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].applicantName").value("Anna " + token + "Alpha"))
                .andExpect(jsonPath("$.content[1].applicantName").value("Anna " + token + "Bravo"));

        mvc.perform(get("/api/applications").with(caseworker("carol")).param("sort", "applicantSub,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approvalFollowsTheFourEyesPrinciple() throws Exception {
        String id = submit(applicant("alice"), "FourEyes");
        completeCheck(id, CheckStatus.CLEAR);

        decide(caseworker("carol"), id, "APPROVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.decisions.length()").value(1));

        decide(caseworker("carol"), id, "APPROVE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_DECIDED"));

        decide(caseworker("dave"), id, "APPROVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void documentsAreTypeCheckedAndDownloadable() throws Exception {
        String id = submit(applicant("alice"), "Document");

        mvc.perform(multipart(HttpMethod.PUT, "/api/applications/{id}/document", id)
                        .file(new MockMultipartFile("file", "fake.pdf", "application/pdf", "MZ...".getBytes()))
                        .with(applicant("alice")))
                .andExpect(status().isBadRequest());

        mvc.perform(multipart(HttpMethod.PUT, "/api/applications/{id}/document", id)
                        .file(new MockMultipartFile("file", "id card.png", "application/octet-stream", PNG))
                        .with(applicant("alice")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/applications/{id}/document", id).with(caseworker("carol")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("id_card.png")));
    }

    private String submit(RequestPostProcessor user, String lastName) throws Exception {
        String body = mvc.perform(post("/api/applications").with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestData.applicantJson(lastName)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private org.springframework.test.web.servlet.ResultActions decide(RequestPostProcessor user, String id,
                                                                      String decision) throws Exception {
        return mvc.perform(post("/api/applications/{id}/decisions", id).with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\": \"" + decision + "\"}"));
    }

    private void completeCheck(String id, CheckStatus result) {
        tx.executeWithoutResult(status -> applications.findById(UUID.fromString(id)).orElseThrow()
                .recordCheckResult(result, "CHK-TEST", Instant.now()));
    }
}
