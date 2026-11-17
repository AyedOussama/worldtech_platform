package fr.entreprise.maintenance.bootstrap.security;

import java.net.URI;
import java.util.Map;
import fr.entreprise.maintenance.features.identity.application.GooglePolicy;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleSecurityConfigurationTest {
    @Test void productionRegistrationUsesFixedIssuerEndpointsScopesAndExactHttpsCallback() {
        var config = new GoogleSecurityConfiguration();
        String syntheticId = "unit-test-only.apps.googleusercontent.com";
        var registration = config.googleRegistration(syntheticId, OpaqueTokens.generate(), URI.create("https://service.example.invalid/login/oauth2/code/google"))
                .findByRegistrationId("google");
        assertThat(registration.getAuthorizationGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(registration.getScopes()).containsExactlyInAnyOrder("openid", "email");
        assertThat(registration.getProviderDetails().getIssuerUri()).isEqualTo("https://accounts.google.com");
        assertThat(registration.getProviderDetails().getJwkSetUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
        assertThat(registration.getRedirectUri()).doesNotContain("{baseUrl}");
        for (String invalid : java.util.List.of("http://localhost/login/oauth2/code/google", "https://service.example.invalid/other",
                "https://service.example.invalid/login/oauth2/code/google?return=elsewhere", "https://user@service.example.invalid/login/oauth2/code/google")) {
            assertThatThrownBy(() -> config.googleRegistration(syntheticId, OpaqueTokens.generate(), URI.create(invalid))).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void mobileDestinationsAreOperatorControlledHttpsLinksNotCustomSchemesOrFreeRedirects() {
        for (String invalid : java.util.List.of("myapp://return", "http://localhost/return", "https://app.example.invalid/return?next=x", "https://app.example.invalid/return#code=x")) {
            assertThatThrownBy(() -> new GooglePolicy(true, Map.of("app", URI.create(invalid)))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(new GooglePolicy(false, Map.of()).mobileDestinations()).isEmpty();
    }
}
