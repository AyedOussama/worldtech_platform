package fr.entreprise.maintenance.features.assets.api;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.features.assets.domain.AssetFailure;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class AssetErrors {
    private final ApiProblems problems;
    public AssetErrors(ApiProblems problems) { this.problems=problems; }
    @ExceptionHandler(AssetFailure.class)
    public ResponseEntity<Object> rejected(AssetFailure failure,HttpServletRequest request) {
        int status=switch(failure.code()) { case INVALID_ASSET_INPUT ->400; case ASSET_NOT_FOUND ->404; case ASSET_VERSION_STALE ->412; };
        var problem=problems.create(status,request); problem.setProperty("code",failure.code().name());
        return ResponseEntity.status(status).headers(problems.headers(request)).body(problem);
    }
}
