package fr.entreprise.maintenance.bootstrap.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer.CookieValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityCookiePolicyTest {
    @Test void productionCookieIsHostOnlySecureHttpOnlyAndLaxAndInvalidHostPrefixIsRefused() {
        var config = new IdentityWebSecurityConfiguration();
        var serializer = config.identityCookieSerializer("__Host-maint-session", true);
        var request = new MockHttpServletRequest(); request.setServerName("service.example.invalid");
        var response = new MockHttpServletResponse();
        serializer.writeCookieValue(new CookieValue(request, response, java.util.UUID.randomUUID().toString()));
        assertThat(response.getHeader("Set-Cookie")).contains("__Host-maint-session=", "Secure", "HttpOnly", "SameSite=Lax", "Path=/").doesNotContain("Domain=");
        assertThatThrownBy(() -> config.identityCookieSerializer("__Host-maint-session", false)).isInstanceOf(IllegalArgumentException.class);
    }
}
