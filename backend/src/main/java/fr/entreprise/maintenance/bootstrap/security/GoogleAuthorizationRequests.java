package fr.entreprise.maintenance.bootstrap.security;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import fr.entreprise.maintenance.features.identity.api.GoogleBrowserSessions;
import fr.entreprise.maintenance.features.identity.application.GoogleIdentityService;
import fr.entreprise.maintenance.shared.crypto.VersionedSecretCipher;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import tools.jackson.databind.json.JsonMapper;

/** No Google tokens or plaintext nonce/PKCE verifier in JDBC sessions. State is bound to the browser and consumed once. */
public final class GoogleAuthorizationRequests implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    static final String TRANSACTION = "maintenance.google.transaction";
    private record Envelope(String authorizationUri, String clientId, String redirectUri, Set<String> scopes, String state,
            Map<String, Object> parameters, Map<String, Object> attributes) {
        @Override public String toString() { return "Envelope[redacted]"; }
    }
    private final GoogleIdentityService google;
    private final GoogleBrowserSessions browsers;
    private final VersionedSecretCipher cipher;
    private final JsonMapper mapper;
    public GoogleAuthorizationRequests(GoogleIdentityService google, GoogleBrowserSessions browsers, VersionedSecretCipher cipher, JsonMapper mapper) {
        this.google = google; this.browsers = browsers; this.cipher = cipher; this.mapper = mapper;
    }
    @Override public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) { return load(request, false); }
    @Override public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) { return load(request, true); }
    private OAuth2AuthorizationRequest load(HttpServletRequest request, boolean consume) {
        var stored = google.authorization(request.getParameter("state"), browsers.binding(request, false), consume);
        if (stored == null) { return null; }
        UUID id = stored.transaction().id();
        Envelope envelope = mapper.readValue(cipher.decrypt(stored.encrypted(), "google-authorization/" + id), Envelope.class);
        if (consume) { request.setAttribute(TRANSACTION, id); }
        return OAuth2AuthorizationRequest.authorizationCode().authorizationUri(envelope.authorizationUri()).clientId(envelope.clientId())
                .redirectUri(envelope.redirectUri()).scopes(envelope.scopes()).state(envelope.state())
                .additionalParameters(envelope.parameters()).attributes(envelope.attributes()).build();
    }
    @Override public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorization, HttpServletRequest request, HttpServletResponse response) {
        if (authorization == null) { return; } // Removal uses the explicit consuming operation, never an unbound cleanup.
        UUID id = (UUID) request.getAttribute(TRANSACTION);
        if (id == null) { throw new IllegalStateException("Prepared Google transaction required."); }
        var envelope = new Envelope(authorization.getAuthorizationUri(), authorization.getClientId(), authorization.getRedirectUri(), authorization.getScopes(),
                authorization.getState(), authorization.getAdditionalParameters(), authorization.getAttributes());
        google.saveAuthorization(id, authorization.getState(), cipher.encrypt(mapper.writeValueAsString(envelope), "google-authorization/" + id));
    }
}
