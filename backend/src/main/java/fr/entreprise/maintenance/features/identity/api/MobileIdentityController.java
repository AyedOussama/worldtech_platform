package fr.entreprise.maintenance.features.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.identity.application.IdentityActions;
import fr.entreprise.maintenance.features.identity.application.MobileIdentityService;
import fr.entreprise.maintenance.features.identity.domain.ActionTokens;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.identity.domain.MobileTokenStore;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MobileIdentityController {
    private final MobileIdentityService mobile;
    private final IdentityActions actions;
    public MobileIdentityController(MobileIdentityService mobile, IdentityActions actions) { this.mobile = mobile; this.actions = actions; }

    @PostMapping("/api/mobile/v1/auth/login")
    public ResponseEntity<TokensView> login(@Valid @RequestBody LoginBody body, HttpServletRequest request) {
        return pair(mobile.login(body.email(), body.password(), request.getRemoteAddr()));
    }
    @PostMapping("/api/mobile/v1/auth/refresh")
    public ResponseEntity<TokensView> refresh(@Valid @RequestBody RefreshBody body, HttpServletRequest request) {
        return pair(mobile.refresh(body.refreshToken(), request.getRemoteAddr()));
    }
    @PostMapping("/api/mobile/v1/auth/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshBody body, HttpServletRequest request) {
        mobile.logout(body.refreshToken(), request.getRemoteAddr());
        return done();
    }
    @PostMapping("/api/mobile/v1/auth/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal LoginIdentity identity) {
        mobile.logoutAll(identity); return done();
    }
    @PostMapping("/api/mobile/v1/auth/reauthenticate")
    public ResponseEntity<Void> reauthenticate(@AuthenticationPrincipal LoginIdentity identity, @Valid @RequestBody WebMfaController.PasswordBody body,
            HttpServletRequest request) {
        mobile.reauthenticate(identity, body.password(), request.getRemoteAddr()); return done();
    }
    @GetMapping("/api/mobile/v1/me")
    public ResponseEntity<WebIdentityController.MeView> me(@AuthenticationPrincipal LoginIdentity identity) {
        var account = mobile.current(identity).account();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new WebIdentityController.MeView(account.userId(), account.status().name(),
                account.roles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()), account.emailVerified()));
    }
    @PostMapping("/api/mobile/v1/auth/email-verification/requests")
    public ResponseEntity<Void> requestVerification(@Valid @RequestBody WebIdentityActionsController.EmailBody body, HttpServletRequest request) {
        actions.request(body.email(), ActionTokens.Purpose.EMAIL_VERIFICATION, request.getRemoteAddr()); return accepted();
    }
    @PostMapping("/api/mobile/v1/auth/password-reset/requests")
    public ResponseEntity<Void> requestReset(@Valid @RequestBody WebIdentityActionsController.EmailBody body, HttpServletRequest request) {
        actions.request(body.email(), ActionTokens.Purpose.PASSWORD_RESET, request.getRemoteAddr()); return accepted();
    }
    @PostMapping("/api/mobile/v1/auth/email-verification/confirm")
    public ResponseEntity<Void> verify(@Valid @RequestBody WebIdentityActionsController.TokenBody body, HttpServletRequest request) {
        actions.verify(body.token(), request.getRemoteAddr()); return done();
    }
    @PostMapping("/api/mobile/v1/auth/password-reset/confirm")
    public ResponseEntity<Void> reset(@Valid @RequestBody WebIdentityActionsController.ResetBody body, HttpServletRequest request) {
        actions.reset(body.token(), body.password(), request.getRemoteAddr()); return done();
    }

    public static ResponseEntity<TokensView> pair(MobileTokenStore.Pair pair) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new TokensView("Bearer", pair.accessToken(), pair.refreshToken(),
                pair.accessExpiresAt(), pair.refreshExpiresAt(), pair.familyExpiresAt()));
    }
    private static ResponseEntity<Void> done() { return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build(); }
    private static ResponseEntity<Void> accepted() { return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build(); }
    public record LoginBody(@NotNull @Size(max = 254) String email, @NotNull @Size(max = 256) String password) {
        @Override public String toString() { return "LoginBody[redacted]"; }
    }
    public record RefreshBody(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{43}") String refreshToken) {
        @Override public String toString() { return "RefreshBody[redacted]"; }
    }
    public record TokensView(String tokenType, String accessToken, String refreshToken, java.time.Instant accessExpiresAt,
            java.time.Instant refreshExpiresAt, java.time.Instant familyExpiresAt) {
        @Override public String toString() { return "TokensView[redacted]"; }
    }
}
