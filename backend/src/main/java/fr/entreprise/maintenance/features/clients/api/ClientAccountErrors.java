package fr.entreprise.maintenance.features.clients.api;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.features.clients.domain.AccountFailure;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class ClientAccountErrors {
    private final ApiProblems problems;
    public ClientAccountErrors(ApiProblems problems) { this.problems=problems; }
    @ExceptionHandler(AccountFailure.class)
    public ResponseEntity<Object> rejected(AccountFailure failure,HttpServletRequest request) {
        int status=switch(failure.code()) { case INVALID_ACCOUNT_INPUT ->400; case ACCOUNT_NOT_FOUND ->404; case ACCOUNT_VERSION_STALE ->412; default ->409; };
        var problem=problems.create(status,request); problem.setProperty("code",failure.code().name());
        return ResponseEntity.status(status).headers(problems.headers(request)).body(problem);
    }
}
