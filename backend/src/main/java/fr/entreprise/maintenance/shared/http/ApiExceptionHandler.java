package fr.entreprise.maintenance.shared.http;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final int MAX_FIELD_ERRORS = 20;

    private final ApiProblems problems;

    public ApiExceptionHandler(ApiProblems problems) {
        this.problems = problems;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest webRequest) {
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        var problem = problems.create(status.value(), request);
        if (exception instanceof MethodArgumentNotValidException validation) {
            problem.setProperty("fieldErrors", validation.getBindingResult().getFieldErrors().stream()
                    .limit(MAX_FIELD_ERRORS)
                    .map(error -> new FieldError(error.getField(),
                            error.getCode() == null ? "INVALID" : error.getCode()))
                    .toList());
        }
        if (status.is5xxServerError()) {
            logFailure(exception, request);
        }
        HttpHeaders safeHeaders = new HttpHeaders();
        safeHeaders.putAll(headers);
        safeHeaders.putAll(problems.headers(request));
        return new ResponseEntity<>(problem, safeHeaders, status);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> accessDenied(HttpServletRequest request) {
        return problems.response(403, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> authenticationRequired(HttpServletRequest request) {
        return problems.response(401, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception exception, HttpServletRequest request) {
        logFailure(exception, request);
        return problems.response(500, request);
    }

    private static void logFailure(Exception exception, HttpServletRequest request) {
        // Exception messages/causes may contain credentials, SQL or private content.
        LOG.error("Request failed: traceId={}, exceptionType={}",
                RequestTrace.id(request), exception.getClass().getName());
    }

    public record FieldError(String field, String code) {
    }
}
