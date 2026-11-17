package fr.entreprise.maintenance.shared.http;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.shared.idempotency.CommandReceipts;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class CommandReceiptExceptionHandler {
    private final ApiProblems problems;
    public CommandReceiptExceptionHandler(ApiProblems problems) { this.problems = problems; }
    @ExceptionHandler(CommandReceipts.Rejected.class)
    public ResponseEntity<Object> rejected(CommandReceipts.Rejected failure, HttpServletRequest request) {
        int status = failure.malformed() ? 400 : 409;
        var problem = problems.create(status, request);
        problem.setProperty("code", failure.malformed() ? "INVALID_IDEMPOTENCY_KEY" : "IDEMPOTENCY_CONFLICT");
        return ResponseEntity.status(status).headers(problems.headers(request)).body(problem);
    }
}
