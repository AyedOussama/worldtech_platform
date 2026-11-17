package fr.entreprise.maintenance.features.clients.api;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.features.clients.domain.ClientFailure;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class ClientOnboardingErrors {
    private final ApiProblems problems;
    public ClientOnboardingErrors(ApiProblems problems) { this.problems=problems; }
    @ExceptionHandler(ClientFailure.class)
    public ResponseEntity<Object> rejected(ClientFailure failure,HttpServletRequest request) {
        int status=switch(failure.code()) {
            case INVALID_CLIENT_INPUT,INVALID_INVITATION -> 400;
            case CLIENT_DOSSIER_NOT_FOUND -> 404;
            case CLIENT_VERSION_STALE -> 412;
            default -> 409;
        };
        var problem=problems.create(status,request); problem.setProperty("code",failure.code().name());
        return ResponseEntity.status(status).headers(problems.headers(request)).body(problem);
    }
}
