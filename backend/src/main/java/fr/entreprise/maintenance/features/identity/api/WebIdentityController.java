package fr.entreprise.maintenance.features.identity.api;

import java.util.Set;
import java.util.UUID;

import fr.entreprise.maintenance.features.identity.application.WebIdentityService;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebIdentityController {
    private final WebIdentityService identity;
    public WebIdentityController(WebIdentityService identity) { this.identity = identity; }

    @GetMapping("/api/web/v1/auth/csrf")
    public ResponseEntity<CsrfView> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CsrfView(token.getHeaderName(), token.getToken()));
    }

    @GetMapping("/api/web/v1/me")
    public ResponseEntity<MeView> me(@AuthenticationPrincipal LoginIdentity principal) {
        var current = identity.current(principal).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS));
        var account = current.account();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new MeView(account.userId(), account.status().name(),
                account.roles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()), account.emailVerified()));
    }

    public record CsrfView(String headerName, String token) { }
    public record MeView(UUID userId, String status, Set<String> roles, boolean emailVerified) { }
}
