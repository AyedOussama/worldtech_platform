package fr.entreprise.maintenance.shared.http;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ApiProblems {

    private final JsonMapper mapper;

    public ApiProblems(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public ProblemDetail create(int status, HttpServletRequest request) {
        int safeStatus = status >= 400 && status <= 599 ? status : 500;
        ProblemCode code = ProblemCode.forStatus(safeStatus);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(safeStatus), code.detail());
        problem.setType(code.type());
        HttpStatus knownStatus = HttpStatus.resolve(safeStatus);
        problem.setTitle(knownStatus == null ? "Request failed" : knownStatus.getReasonPhrase());
        String traceId = RequestTrace.id(request);
        problem.setInstance(URI.create("urn:maintenance:request:" + traceId));
        problem.setProperty("code", code.name());
        problem.setProperty("traceId", traceId);
        return problem;
    }

    public ResponseEntity<Object> response(int status, HttpServletRequest request) {
        ProblemDetail problem = create(status, request);
        return new ResponseEntity<>(problem, headers(request), problem.getStatus());
    }

    public HttpHeaders headers(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.setCacheControl("no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set(RequestTrace.HEADER, RequestTrace.id(request));
        return headers;
    }

    public void write(int status, HttpServletRequest request, HttpServletResponse response) throws IOException {
        ProblemDetail problem = create(status, request);
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader(RequestTrace.HEADER, RequestTrace.id(request));
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
