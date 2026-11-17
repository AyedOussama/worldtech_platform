package fr.entreprise.maintenance.bootstrap.http;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {

    private final ApiProblems problems;

    public ApiErrorController(ApiProblems problems) {
        this.problems = problems;
    }

    @RequestMapping("/error")
    public ResponseEntity<Object> error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        // Never serialize servlet error messages, exceptions, attributes or request URLs.
        return problems.response(status instanceof Integer value ? value : 500, request);
    }
}
