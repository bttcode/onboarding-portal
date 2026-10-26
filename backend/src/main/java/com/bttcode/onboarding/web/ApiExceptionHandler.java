package com.bttcode.onboarding.web;

import com.bttcode.onboarding.domain.BusinessRuleException;
import com.bttcode.onboarding.service.InvalidRequestException;
import com.bttcode.onboarding.service.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 problem details. Spring's base class already covers binding and
 * validation errors; this adds the application's own exceptions.
 *
 * <p>There is deliberately no catch-all handler: access-denied errors must reach Spring Security
 * to become 403, and unexpected errors should stay 500 without leaking internals.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail invalid(InvalidRequestException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    /** Constraint violations on query parameters, raised by method validation on the generated interface. */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraintViolation(ConstraintViolationException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail businessRule(BusinessRuleException e) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Business rule violated", e.getMessage());
        problem.setProperty("code", e.code());
        return problem;
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ProblemDetail concurrentUpdate(RuntimeException e) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Concurrent update",
                "Someone else changed this application at the same time. Reload it and try again.");
        problem.setProperty("code", "CONCURRENT_UPDATE");
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
