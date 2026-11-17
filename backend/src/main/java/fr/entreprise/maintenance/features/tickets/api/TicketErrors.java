package fr.entreprise.maintenance.features.tickets.api;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.features.tickets.domain.TicketFailure;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class TicketErrors {
    private final ApiProblems problems;
    public TicketErrors(ApiProblems problems) { this.problems=problems; }
    @ExceptionHandler(TicketFailure.class)
    public ResponseEntity<Object> rejected(TicketFailure failure,HttpServletRequest request) {
        int status=switch(failure.code()) { case INVALID_TICKET_INPUT ->400; case TICKET_NOT_FOUND ->404; case TICKET_VERSION_STALE ->412; case TICKET_STATE_CONFLICT ->409; };
        var problem=problems.create(status,request); problem.setProperty("code",failure.code().name()); return ResponseEntity.status(status).headers(problems.headers(request)).body(problem);
    }
}
