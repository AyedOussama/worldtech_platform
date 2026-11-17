package fr.entreprise.maintenance.features.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class IdentityExceptionHandler {
    private final ApiProblems problems;
    public IdentityExceptionHandler(ApiProblems problems) { this.problems = problems; }

    @ExceptionHandler(fr.entreprise.maintenance.features.identity.publicapi.RegistrationConflict.class)
    public ResponseEntity<Object> registrationConflict(HttpServletRequest request) { return problems.response(409,request); }

    @ExceptionHandler(IdentityFailure.class)
    public ResponseEntity<Object> rejected(IdentityFailure failure, HttpServletRequest request) {
        if (failure.reason() == IdentityFailure.Reason.RATE_LIMITED) {
            var result = problems.response(429, request);
            return ResponseEntity.status(429).headers(result.getHeaders()).header("Retry-After", Integer.toString(failure.retryAfterSeconds()))
                    .body(result.getBody());
        }
        return problems.response(switch (failure.reason()) {
            case INVALID_INPUT -> 400;
            case BAD_CREDENTIALS -> 401;
            case FORBIDDEN -> 403;
            case RATE_LIMITED -> 429;
            case CONFLICT -> 409;
        }, request);
    }
}
