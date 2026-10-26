package com.bttcode.sanctionsmock;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckController {

    private final CheckService service;

    public CheckController(CheckService service) {
        this.service = service;
    }

    @PostMapping("/checks")
    public ResponseEntity<CheckResponse> check(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                               @Valid @RequestBody CheckRequest request) throws InterruptedException {
        CheckResponse response = service.check(idempotencyKey, request);
        return response == null
                ? ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
                : ResponseEntity.ok(response);
    }

    @GetMapping("/admin/chaos")
    public ChaosSettings chaos() {
        return service.chaos();
    }

    @PutMapping("/admin/chaos")
    public ChaosSettings setChaos(@Valid @RequestBody ChaosSettings settings) {
        service.setChaos(settings);
        return settings;
    }

    @GetMapping("/admin/stats")
    public CheckService.Stats stats() {
        return service.stats();
    }
}
