package com.bttcode.sanctionsmock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CheckControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CheckService service;

    @AfterEach
    void heal() {
        service.setChaos(ChaosSettings.HEALTHY);
    }

    @Test
    void theSameKeyAlwaysGetsTheSameResult() throws Exception {
        String first = check("key-1", "Muster").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = check("key-1", "Muster").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String firstReference = JsonPath.read(first, "$.reference");
        String secondReference = JsonPath.read(second, "$.reference");
        assertThat(secondReference).isEqualTo(firstReference);
    }

    @Test
    void statsCountRetriesSeparatelyFromChecks() throws Exception {
        CheckService.Stats before = service.stats();

        check("key-5", "Muster");
        check("key-5", "Muster");

        CheckService.Stats after = service.stats();
        assertThat(after.totalCalls() - before.totalCalls()).isEqualTo(2);
        assertThat(after.distinctChecks() - before.distinctChecks()).isEqualTo(1);
        mvc.perform(get("/admin/stats")).andExpect(jsonPath("$.distinctChecks").value(after.distinctChecks()));
    }

    @Test
    void aListedNameIsAHit() throws Exception {
        check("key-2", "blocked").andExpect(jsonPath("$.result").value("HIT"));
        check("key-3", "Muster").andExpect(jsonPath("$.result").value("CLEAR"));
    }

    @Test
    void whenDownEveryCallFails() throws Exception {
        mvc.perform(put("/admin/chaos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"down\": true, \"failureRate\": 0, \"latencyMs\": 0}"))
                .andExpect(status().isOk());

        check("key-4", "Muster").andExpect(status().isServiceUnavailable());
    }

    @Test
    void invalidChaosSettingsAreRejected() throws Exception {
        mvc.perform(put("/admin/chaos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"down\": false, \"failureRate\": 1.5, \"latencyMs\": 0}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions check(String key, String lastName) throws Exception {
        return mvc.perform(post("/checks")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName": "Anna", "lastName": "%s", "birthDate": "1990-04-12", "nationality": "CH"}
                        """.formatted(lastName)));
    }
}
