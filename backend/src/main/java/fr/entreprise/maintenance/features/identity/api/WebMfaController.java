package fr.entreprise.maintenance.features.identity.api;

import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.identity.application.MfaService;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebMfaController {
    private final MfaService mfa;
    private final WebAuthenticationSessions sessions;
    public WebMfaController(MfaService mfa, WebAuthenticationSessions sessions) { this.mfa = mfa; this.sessions = sessions; }

    @PostMapping("/api/web/v1/auth/mfa/enrollment/start")
    public ResponseEntity<MfaService.Enrollment> start(@AuthenticationPrincipal LoginIdentity principal, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mfa.start(principal, request.getRemoteAddr(), false));
    }
    @PostMapping("/api/web/v1/auth/mfa/replacement/start")
    public ResponseEntity<MfaService.Enrollment> replace(@AuthenticationPrincipal LoginIdentity principal, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mfa.start(principal, request.getRemoteAddr(), true));
    }
    @PostMapping("/api/web/v1/auth/mfa/enrollment/confirm")
    public ResponseEntity<ActivationView> activate(@AuthenticationPrincipal LoginIdentity principal, @Valid @RequestBody EnrollmentBody body,
            HttpServletRequest request, HttpServletResponse response) {
        var result = mfa.activate(principal, body.enrollmentId(), body.code(), request.getRemoteAddr());
        if (result.requiresLogin()) { sessions.clear(request); } else { sessions.rotate(principal, request, response); }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ActivationView(
                result.requiresLogin() ? "REAUTHENTICATION_REQUIRED" : "AUTHENTICATED", result.recoveryCodes()));
    }
    @PostMapping("/api/web/v1/auth/mfa/challenge")
    public ResponseEntity<Void> prove(@AuthenticationPrincipal LoginIdentity principal, @Valid @RequestBody TotpBody body,
            HttpServletRequest request, HttpServletResponse response) {
        mfa.prove(principal, body.code(), request.getRemoteAddr(), false);
        sessions.rotate(principal, request, response);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping("/api/web/v1/auth/mfa/recovery")
    public ResponseEntity<Void> recover(@AuthenticationPrincipal LoginIdentity principal, @Valid @RequestBody RecoveryBody body,
            HttpServletRequest request, HttpServletResponse response) {
        mfa.prove(principal, body.code(), request.getRemoteAddr(), true);
        sessions.rotate(principal, request, response);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping("/api/web/v1/auth/mfa/recovery-codes/replace")
    public ResponseEntity<ActivationView> codes(@AuthenticationPrincipal LoginIdentity principal, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ActivationView("AUTHENTICATED",
                mfa.recoveryCodes(principal, request.getRemoteAddr())));
    }
    @PostMapping("/api/web/v1/auth/reauthenticate")
    public ResponseEntity<Void> reauthenticate(@AuthenticationPrincipal LoginIdentity principal, @Valid @RequestBody PasswordBody body,
            HttpServletRequest request, HttpServletResponse response) {
        var renewed = mfa.reauthenticate(principal, body.password(), request.getRemoteAddr());
        sessions.rotate(renewed, request, response);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    public record EnrollmentBody(@NotNull UUID enrollmentId, @NotNull @Pattern(regexp = "[0-9]{6}") String code) {
        @Override public String toString() { return "EnrollmentBody[redacted]"; }
    }
    public record TotpBody(@NotNull @Pattern(regexp = "[0-9]{6}") String code) {
        @Override public String toString() { return "TotpBody[redacted]"; }
    }
    public record RecoveryBody(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{43}") String code) {
        @Override public String toString() { return "RecoveryBody[redacted]"; }
    }
    public record PasswordBody(@NotNull @Size(max = 256) String password) {
        @Override public String toString() { return "PasswordBody[redacted]"; }
    }
    public record ActivationView(String status, List<String> recoveryCodes) {
        @Override public String toString() { return "ActivationView[redacted]"; }
    }
}
