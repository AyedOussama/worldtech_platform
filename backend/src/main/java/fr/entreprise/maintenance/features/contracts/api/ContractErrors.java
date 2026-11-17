package fr.entreprise.maintenance.features.contracts.api;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.features.contracts.domain.ContractFailure;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class ContractErrors {
    private final ApiProblems problems;
    public ContractErrors(ApiProblems problems) { this.problems=problems; }
    @ExceptionHandler(ContractFailure.class)
    public ResponseEntity<Object> rejected(ContractFailure failure,HttpServletRequest request) {
        int status=switch(failure.code()) { case INVALID_CONTRACT_INPUT ->400; case CONTRACT_NOT_FOUND ->404; case CONTRACT_VERSION_STALE ->412; case CONTRACT_NOT_APPLICABLE ->409; };
        var problem=problems.create(status,request); problem.setProperty("code",failure.code().name()); return ResponseEntity.status(status).headers(problems.headers(request)).body(problem);
    }
}
