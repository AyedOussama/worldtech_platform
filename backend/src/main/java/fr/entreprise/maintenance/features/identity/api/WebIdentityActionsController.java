package fr.entreprise.maintenance.features.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.identity.application.IdentityActions;
import fr.entreprise.maintenance.features.identity.domain.ActionTokens;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebIdentityActionsController {
    private final IdentityActions actions;
    public WebIdentityActionsController(IdentityActions actions) { this.actions = actions; }

    @PostMapping("/api/web/v1/auth/email-verification/requests")
    public ResponseEntity<Void> requestVerification(@Valid @RequestBody EmailBody body, HttpServletRequest request) {
        actions.request(body.email(), ActionTokens.Purpose.EMAIL_VERIFICATION, request.getRemoteAddr());
        return accepted();
    }
    @PostMapping("/api/web/v1/auth/password-reset/requests")
    public ResponseEntity<Void> requestReset(@Valid @RequestBody EmailBody body, HttpServletRequest request) {
        actions.request(body.email(), ActionTokens.Purpose.PASSWORD_RESET, request.getRemoteAddr());
        return accepted();
    }
    @PostMapping("/api/web/v1/auth/email-verification/confirm")
    public ResponseEntity<Void> verify(@Valid @RequestBody TokenBody body, HttpServletRequest request) {
        actions.verify(body.token(), request.getRemoteAddr());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping("/api/web/v1/auth/password-reset/confirm")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetBody body, HttpServletRequest request) {
        actions.reset(body.token(), body.password(), request.getRemoteAddr());
        var session = request.getSession(false);
        if (session != null) { session.invalidate(); }
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private static ResponseEntity<Void> accepted() { return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build(); }
    public record EmailBody(@NotBlank @Size(max = 254) String email) {
        @Override public String toString() { return "EmailBody[redacted]"; }
    }
    public record TokenBody(@NotBlank @Size(max = 43) String token) {
        @Override public String toString() { return "TokenBody[redacted]"; }
    }
    public record ResetBody(@NotBlank @Size(max = 43) String token, @NotNull @Size(max = 256) String password) {
        @Override public String toString() { return "ResetBody[redacted]"; }
    }
}
