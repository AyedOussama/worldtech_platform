package fr.entreprise.httpfixtures;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Outside the application's component scan; only used by standalone MVC tests. */
@RestController
public class ErrorProbeController {

    @GetMapping("/_test/error")
    public String error() {
        throw new IllegalStateException("internal-diagnostic-marker");
    }

    @GetMapping("/_test/denied")
    public String denied() {
        throw new AccessDeniedException("internal-diagnostic-marker");
    }

    @PostMapping("/_test/validation")
    public String validate(@Valid @RequestBody ProbeInput input) {
        return "not-used";
    }

    public record ProbeInput(@Size(max = 3, message = "unsafe-validation-message") String value) {
    }
}
