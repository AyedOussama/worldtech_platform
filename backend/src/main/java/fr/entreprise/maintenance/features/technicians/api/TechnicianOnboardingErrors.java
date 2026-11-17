package fr.entreprise.maintenance.features.technicians.api;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianFailure;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class TechnicianOnboardingErrors {
    private final ApiProblems problems;
    public TechnicianOnboardingErrors(ApiProblems problems) { this.problems=problems; }
    @ExceptionHandler(TechnicianFailure.class)
    public ResponseEntity<Object> rejected(TechnicianFailure failure,HttpServletRequest request) {
        int status=switch(failure.code()) {
            case INVALID_TECHNICIAN_INPUT -> 400;
            case TECHNICIAN_DOSSIER_NOT_FOUND -> 404;
            case TECHNICIAN_VERSION_STALE -> 412;
            default -> 409;
        };
        var problem=problems.create(status,request); problem.setProperty("code",failure.code().name());
        return ResponseEntity.status(status).headers(problems.headers(request)).body(problem);
    }
}
