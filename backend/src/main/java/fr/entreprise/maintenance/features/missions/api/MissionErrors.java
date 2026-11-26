package fr.entreprise.maintenance.features.missions.api;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.features.missions.domain.MissionFailure;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class MissionErrors {
    private final ApiProblems problems;
    public MissionErrors(ApiProblems problems) { this.problems=problems; }
    @ExceptionHandler(MissionFailure.class)
    public ResponseEntity<Object> rejected(MissionFailure failure,HttpServletRequest request) {
        int status=switch(failure.code()) {
            case INVALID_MISSION_INPUT -> 400;
            case MISSION_NOT_FOUND -> 404;
            case MISSION_VERSION_STALE -> 412;
            case MISSION_STATE_CONFLICT -> 409;
        };
        var problem=problems.create(status,request); problem.setProperty("code",failure.code().name()); return ResponseEntity.status(status).headers(problems.headers(request)).body(problem);
    }
}
