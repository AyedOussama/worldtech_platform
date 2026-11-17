package fr.entreprise.maintenance.bootstrap.security;

import java.net.URI;
import java.util.Map;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import fr.entreprise.maintenance.features.identity.api.GoogleBrowserSessions;
import fr.entreprise.maintenance.features.identity.api.WebAuthenticationSessions;
import fr.entreprise.maintenance.features.identity.application.GoogleIdentityService;
import fr.entreprise.maintenance.features.identity.application.GooglePolicy;
import fr.entreprise.maintenance.features.identity.application.WebIdentityService;
import fr.entreprise.maintenance.features.identity.domain.GoogleSubject;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.shared.crypto.VersionedSecretCipher;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "app.identity.google.enabled", havingValue = "true")
public class GoogleSecurityConfiguration {
    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    ClientRegistrationRepository googleRegistration(@Value("${MAINT_GOOGLE_WEB_CLIENT_ID}") String id,
            @Value("${google_client_secret}") String secret, @Value("${MAINT_GOOGLE_REDIRECT_URI}") URI callback) {
        if (!id.matches("[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com") || secret.isBlank()
                || !"https".equals(callback.getScheme()) || callback.getHost() == null || callback.getUserInfo() != null || callback.getPort() != -1
                || callback.getQuery() != null || callback.getFragment() != null || !"/login/oauth2/code/google".equals(callback.getPath())) {
            throw new IllegalArgumentException("Real Google client and exact controlled HTTPS callback required.");
        }
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("google").clientId(id).clientSecret(secret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(callback.toString()).scope("openid", "email").authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token").jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com").userNameAttributeName("sub").clientName("Google").build());
    }

    @Bean @Order(5)
    SecurityFilterChain googleChain(HttpSecurity http, ClientRegistrationRepository registrations, GoogleIdentityService google,
            GooglePolicy policy, GoogleBrowserSessions browsers, WebAuthenticationSessions sessions, WebIdentityService web,
            VersionedSecretCipher cipher, ApiProblems problems, JsonMapper mapper) throws Exception {
        var delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        delegate.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        var resolver = new OAuth2AuthorizationRequestResolver() {
            @Override public OAuth2AuthorizationRequest resolve(HttpServletRequest request) { return resolve(request, "google"); }
            @Override public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registration) {
                if (!"GET".equals(request.getMethod()) || !"/oauth2/authorization/google".equals(request.getRequestURI())) { return null; }
                if (request.getAttribute(GoogleAuthorizationRequests.TRANSACTION) == null) { return null; }
                return delegate.resolve(request, "google");
            }
        };
        var authorizedClients = new OAuth2AuthorizedClientRepository() {
            @Override public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String id, Authentication principal, HttpServletRequest request) { return null; }
            @Override public void saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication principal, HttpServletRequest request, HttpServletResponse response) { }
            @Override public void removeAuthorizedClient(String id, Authentication principal, HttpServletRequest request, HttpServletResponse response) { }
        };
        var oidc = new OidcUserService(); oidc.setRetrieveUserInfo(request -> false); // ID-token claims suffice; no profile API or token persistence.
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                            throws ServletException, IOException {
                        if ("GET".equals(request.getMethod()) && "/oauth2/authorization/google".equals(request.getRequestURI())) {
                            try {
                                var transaction = google.launch(request.getParameter("launch"), browsers.binding(request, true));
                                request.setAttribute(GoogleAuthorizationRequests.TRANSACTION, transaction.id());
                            } catch (IdentityFailure failure) { problems.write(401, request, response); return; }
                            catch (RuntimeException failure) { problems.write(503, request, response); return; }
                        }
                        chain.doFilter(request, response);
                    }
                }, OAuth2AuthorizationRequestRedirectFilter.class)
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()).requireExplicitSave(true))
                .sessionManagement(session -> session.sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .requestCache(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/oauth2/authorization/google", "/login/oauth2/code/google").permitAll().anyRequest().denyAll())
                .oauth2Login(oauth -> oauth.authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver)
                                .authorizationRequestRepository(new GoogleAuthorizationRequests(google, browsers, cipher, mapper)))
                        .authorizedClientRepository(authorizedClients).userInfoEndpoint(info -> info.oidcUserService(oidc))
                        .successHandler((request, response, authentication) -> {
                            try {
                                if (!(authentication.getPrincipal() instanceof OidcUser user) || !Boolean.TRUE.equals(user.getEmailVerified())) {
                                    problems.write(401, request, response); return;
                                }
                                var result = google.complete((java.util.UUID) request.getAttribute(GoogleAuthorizationRequests.TRANSACTION),
                                        new GoogleSubject(user.getIssuer().toString(), user.getSubject()),user.getEmail(), browsers.identity(request));
                                response.setHeader("Cache-Control", "no-store");
                                if (result.handoffCode() != null) {
                                    response.setStatus(303);
                                    response.setHeader("Location", policy.destination(result.destinationId()) + "#code=" + result.handoffCode() + "&transaction=" + result.transactionId());
                                    return;
                                }
                                sessions.rotate(result.webIdentity(), request, response);
                                var current = web.current(result.webIdentity()).orElseThrow();
                                var authorities = web.authorities(current);
                                String state = authorities.contains("MFA_PENDING") ? "MFA_PENDING"
                                        : authorities.contains("EMAIL_PENDING") ? "EMAIL_PENDING" : "AUTHENTICATED";
                                response.setContentType("application/json");
                                mapper.writeValue(response.getOutputStream(), Map.of("status", state));
                            } catch (IdentityFailure failure) { problems.write(failure.reason() == IdentityFailure.Reason.CONFLICT ? 409 : 401, request, response); }
                            catch (RuntimeException failure) { problems.write(503, request, response); }
                        })
                        .failureHandler((request, response, failure) -> problems.write(401, request, response)))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, failure) -> problems.write(401, request, response))
                        .accessDeniedHandler((request, response, failure) -> problems.write(403, request, response)))
                .headers(headers -> headers.referrerPolicy(policyHeaders -> policyHeaders.policy(ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'")));
        return http.build();
    }
}
