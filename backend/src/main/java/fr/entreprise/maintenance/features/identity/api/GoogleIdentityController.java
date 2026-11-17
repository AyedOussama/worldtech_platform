package fr.entreprise.maintenance.features.identity.api;

import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import fr.entreprise.maintenance.features.identity.application.GoogleIdentityService;
import fr.entreprise.maintenance.features.identity.domain.GoogleFlows;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "app.identity.google.enabled", havingValue = "true")
public class GoogleIdentityController {
    private final GoogleIdentityService google;
    private final GoogleBrowserSessions browsers;
    private final WebAuthenticationSessions sessions;
    public GoogleIdentityController(GoogleIdentityService google, GoogleBrowserSessions browsers, WebAuthenticationSessions sessions) {
        this.google = google; this.browsers = browsers; this.sessions = sessions;
    }
    @PostMapping("/api/web/v1/auth/google/transactions")
    public ResponseEntity<GoogleIdentityService.Start> webLogin(HttpServletRequest request) {
        return started(google.start(GoogleFlows.Channel.WEB, GoogleFlows.Intention.LOGIN, null, browsers.binding(request, true), null, null, request.getRemoteAddr()));
    }
    @PostMapping("/api/web/v1/auth/google/client-registration/transactions")
    public ResponseEntity<GoogleIdentityService.Start> clientRegistration(@Valid @RequestBody ClientRegistrationBody body,HttpServletRequest request) {
        return started(google.registerClient(browsers.binding(request,true),body.token(),body.email(),request.getRemoteAddr()));
    }
    @PostMapping("/api/web/v1/auth/google/technician-registration/transactions")
    public ResponseEntity<GoogleIdentityService.Start> webTechnicianRegistration(HttpServletRequest request) {
        return started(google.registerTechnician(GoogleFlows.Channel.WEB,browsers.binding(request,true),null,null,request.getRemoteAddr()));
    }
    @PostMapping("/api/mobile/v1/auth/google/technician-registration/transactions")
    public ResponseEntity<GoogleIdentityService.Start> mobileTechnicianRegistration(@Valid @RequestBody MobileStart body,HttpServletRequest request) {
        return started(google.registerTechnician(GoogleFlows.Channel.MOBILE,null,body.challenge(),body.destinationId(),request.getRemoteAddr()));
    }
    @PostMapping("/api/web/v1/auth/identities/google/link")
    public ResponseEntity<GoogleIdentityService.Start> webLink(@AuthenticationPrincipal LoginIdentity identity, HttpServletRequest request) {
        return started(google.start(GoogleFlows.Channel.WEB, GoogleFlows.Intention.LINK, identity, browsers.binding(request, true), null, null, request.getRemoteAddr()));
    }
    @PostMapping("/api/web/v1/auth/identities/google/unlink")
    public ResponseEntity<Void> webUnlink(@AuthenticationPrincipal LoginIdentity identity, HttpServletRequest request) {
        google.unlink(identity, GoogleFlows.Channel.WEB); sessions.clear(request);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping("/api/mobile/v1/auth/google/transactions")
    public ResponseEntity<GoogleIdentityService.Start> mobileLogin(@Valid @RequestBody MobileStart body, HttpServletRequest request) {
        return started(google.start(GoogleFlows.Channel.MOBILE, GoogleFlows.Intention.LOGIN, null, null, body.challenge(), body.destinationId(), request.getRemoteAddr()));
    }
    @PostMapping("/api/mobile/v1/auth/identities/google/link")
    public ResponseEntity<GoogleIdentityService.Start> mobileLink(@AuthenticationPrincipal LoginIdentity identity, @Valid @RequestBody MobileStart body,
            HttpServletRequest request) {
        return started(google.start(GoogleFlows.Channel.MOBILE, GoogleFlows.Intention.LINK, identity, null, body.challenge(), body.destinationId(), request.getRemoteAddr()));
    }
    @PostMapping("/api/mobile/v1/auth/identities/google/unlink")
    public ResponseEntity<Void> mobileUnlink(@AuthenticationPrincipal LoginIdentity identity) {
        google.unlink(identity, GoogleFlows.Channel.MOBILE);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping("/api/mobile/v1/auth/google/exchange")
    public ResponseEntity<?> exchange(@AuthenticationPrincipal LoginIdentity identity, @Valid @RequestBody ExchangeBody body, HttpServletRequest request) {
        var result = google.exchange(body.transactionId(), body.code(), body.verifier(), identity, request.getRemoteAddr());
        return result.linked() ? ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("status", "LINKED"))
                : MobileIdentityController.pair(result.tokens());
    }
    private static ResponseEntity<GoogleIdentityService.Start> started(GoogleIdentityService.Start start) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(start);
    }
    public record MobileStart(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{43}") String challenge,
            @NotNull @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String destinationId) { }
    public record ClientRegistrationBody(@NotNull @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,@NotNull @jakarta.validation.constraints.Size(max=254) String email) {
        @Override public String toString() { return "ClientRegistrationBody[redacted]"; }
    }
    public record ExchangeBody(@NotNull UUID transactionId, @NotNull @Pattern(regexp = "[A-Za-z0-9_-]{43}") String code,
            @NotNull @Pattern(regexp = "[A-Za-z0-9._~-]{43,128}") String verifier) {
        @Override public String toString() { return "ExchangeBody[redacted]"; }
    }
}
