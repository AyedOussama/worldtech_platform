package fr.entreprise.maintenance.bootstrap.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import fr.entreprise.maintenance.features.identity.application.WebIdentityService;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
public class IdentityWebSecurityConfiguration {
    @Bean
    DefaultCookieSerializer identityCookieSerializer(
            @Value("${app.identity.cookie-name:__Host-maint-session}") String name,
            @Value("${app.identity.secure-cookie:true}") boolean secure) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,80}") || (name.startsWith("__Host-") && !secure)) {
            throw new IllegalArgumentException("Invalid session cookie policy.");
        }
        var serializer = new DefaultCookieSerializer();
        serializer.setCookieName(name);
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite("Lax");
        serializer.setUseBase64Encoding(false);
        return serializer;
    }

    @Bean
    @Order(10)
    SecurityFilterChain identityWebChain(HttpSecurity http, WebIdentityService identity, ApiProblems problems,
            JsonMapper mapper, @Value("${app.identity.cookie-name:__Host-maint-session}") String cookieName) throws Exception {
        var contexts = new HttpSessionSecurityContextRepository();
        var csrf = new HttpSessionCsrfTokenRepository();
        var sessions = new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
        var provider = new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) {
                try {
                    var principal = identity.login(authentication.getName(), (String) authentication.getCredentials(),
                            (String) authentication.getDetails());
                    var current = identity.current(principal).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS));
                    return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                            identity.authorities(current).stream().map(SimpleGrantedAuthority::new).toList());
                } catch (IdentityFailure failure) {
                    throw new LoginException(failure.reason() == IdentityFailure.Reason.RATE_LIMITED ? 429 : 401, failure.retryAfterSeconds());
                } catch (RuntimeException failure) {
                    throw new LoginException(503);
                }
            }

            @Override
            public boolean supports(Class<?> type) { return type == UsernamePasswordAuthenticationToken.class; }
        };
        var login = new JsonLoginFilter(new ProviderManager(provider), mapper);
        login.setSecurityContextRepository(contexts);
        login.setSessionAuthenticationStrategy(sessions);
        login.setAuthenticationFailureHandler((request, response, failure) -> {
            int status = failure instanceof LoginException known ? known.status : 401;
            if (status == 429) { response.setHeader("Retry-After", Integer.toString(((LoginException) failure).retryAfter)); }
            problems.write(status, request, response);
        });
        login.setAuthenticationSuccessHandler((request, response, authentication) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            String level = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("MFA_PENDING"))
                    ? "MFA_PENDING" : authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("EMAIL_PENDING"))
                    ? "EMAIL_PENDING" : "AUTHENTICATED";
            mapper.writeValue(response.getOutputStream(), Map.of("status", level));
        });
        String contracts=fr.entreprise.maintenance.features.contracts.api.ContractController.ADMIN;
        String coverage=fr.entreprise.maintenance.features.contracts.api.ContractController.CLIENT;
        String adminTickets=fr.entreprise.maintenance.features.tickets.api.TicketController.ADMIN;
        String clientTickets=fr.entreprise.maintenance.features.tickets.api.TicketController.CLIENT;
        String adminMissions=fr.entreprise.maintenance.features.missions.api.MissionController.ADMIN;
        http.securityMatcher("/api/web/v1/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/web/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/web/v1/auth/login", "/api/web/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/web/v1/auth/email-verification/requests", "/api/web/v1/auth/email-verification/confirm",
                                "/api/web/v1/auth/password-reset/requests", "/api/web/v1/auth/password-reset/confirm").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/web/v1/auth/google/transactions").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/web/v1/auth/google/client-registration/transactions","/api/web/v1/auth/google/technician-registration/transactions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/web/v1/auth/identities/google/link", "/api/web/v1/auth/identities/google/unlink")
                                .hasAuthority("IDENTITY_READ")
                        .requestMatchers(HttpMethod.GET, "/api/web/v1/me").hasAuthority("IDENTITY_READ")
                        .requestMatchers(HttpMethod.GET,coverage,coverage+"/{revisionId}",clientTickets,clientTickets+"/{ticketId}",
                                clientTickets+"/{ticketId}/proposals",clientTickets+"/{ticketId}/history",clientTickets+"/{ticketId}/messages").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.POST,clientTickets,clientTickets+"/{ticketId}/proposals",clientTickets+"/{ticketId}/confirm-resolution",
                                clientTickets+"/{ticketId}/report-persistence",clientTickets+"/{ticketId}/messages").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET,contracts,contracts+"/{contractId}",contracts+"/{contractId}/revisions",contracts+"/{contractId}/revisions/{revisionId}",
                                adminTickets,adminTickets+"/{ticketId}",adminTickets+"/{ticketId}/proposals",adminTickets+"/{ticketId}/history",adminTickets+"/{ticketId}/messages",adminTickets+"/{ticketId}/internal-notes",
                                adminMissions,adminMissions+"/{missionId}",adminMissions+"/{missionId}/financial-revisions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,contracts,contracts+"/{contractId}/revisions",adminTickets+"/{ticketId}/qualify",adminTickets+"/{ticketId}/start",
                                adminTickets+"/{ticketId}/wait-client",adminTickets+"/{ticketId}/resume",adminTickets+"/{ticketId}/resolve",adminTickets+"/{ticketId}/cancel",
                                adminTickets+"/{ticketId}/messages",adminTickets+"/{ticketId}/internal-notes",
                                adminMissions,adminMissions+"/{missionId}/publish",adminMissions+"/{missionId}/financial-revisions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/web/v1/client/accounts","/api/web/v1/client/accounts/{accountId}",
                                "/api/web/v1/client/accounts/{accountId}/sites","/api/web/v1/client/accounts/{accountId}/sites/{siteId}",
                                "/api/web/v1/client/accounts/{accountId}/equipment","/api/web/v1/client/accounts/{accountId}/equipment/{equipmentId}",
                                "/api/web/v1/client/accounts/{accountId}/summary").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET,"/api/web/v1/admin/client-accounts","/api/web/v1/admin/client-accounts/{accountId}",
                                "/api/web/v1/admin/client-accounts/{accountId}/members","/api/web/v1/admin/client-accounts/{accountId}/members/{memberId}","/api/web/v1/admin/client-accounts/{accountId}/sites",
                                "/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}","/api/web/v1/admin/client-accounts/{accountId}/equipment",
                                "/api/web/v1/admin/client-accounts/{accountId}/equipment/{equipmentId}","/api/web/v1/admin/client-accounts/{accountId}/summary",
                                "/api/web/v1/admin/client-accounts/{accountId}/members/{userId}/site-access").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,"/api/web/v1/admin/client-accounts","/api/web/v1/admin/client-accounts/{accountId}/members",
                                "/api/web/v1/admin/client-accounts/{accountId}/members/{memberId}/remove","/api/web/v1/admin/client-accounts/{accountId}/sites",
                                "/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/archive","/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/equipment",
                                "/api/web/v1/admin/client-accounts/{accountId}/equipment/{equipmentId}/archive").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,"/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}",
                                "/api/web/v1/admin/client-accounts/{accountId}/equipment/{equipmentId}","/api/web/v1/admin/client-accounts/{accountId}/members/{userId}/site-access").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,"/api/web/v1/admin/client-accounts/{accountId}","/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}",
                                "/api/web/v1/admin/client-accounts/{accountId}/equipment/{equipmentId}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,"/api/web/v1/onboarding/client/register","/api/web/v1/onboarding/technician/register").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/web/v1/onboarding/client","/api/web/v1/onboarding/client/history",
                                "/api/web/v1/onboarding/technician","/api/web/v1/onboarding/technician/history").hasAuthority("IDENTITY_READ")
                        .requestMatchers(HttpMethod.POST,"/api/web/v1/onboarding/client/accept-invitation","/api/web/v1/onboarding/client/profile","/api/web/v1/onboarding/client/submit",
                                "/api/web/v1/onboarding/technician/start","/api/web/v1/onboarding/technician/profile","/api/web/v1/onboarding/technician/submit").hasAuthority("IDENTITY_READ")
                        .requestMatchers(HttpMethod.GET,"/api/web/v1/admin/client-invitations/{id}","/api/web/v1/admin/onboardings/clients",
                                "/api/web/v1/admin/onboardings/clients/{id}","/api/web/v1/admin/onboardings/clients/{id}/history","/api/web/v1/admin/onboardings/technicians",
                                "/api/web/v1/admin/onboardings/technicians/{id}","/api/web/v1/admin/onboardings/technicians/{id}/history").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,"/api/web/v1/admin/client-invitations","/api/web/v1/admin/client-invitations/{id}/resend",
                                "/api/web/v1/admin/onboardings/clients/{id}/approve","/api/web/v1/admin/onboardings/clients/{id}/reject","/api/web/v1/admin/onboardings/clients/{id}/request-information",
                                "/api/web/v1/admin/onboardings/technicians/{id}/approve","/api/web/v1/admin/onboardings/technicians/{id}/reject","/api/web/v1/admin/onboardings/technicians/{id}/request-information").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/web/v1/auth/reauthenticate", "/api/web/v1/auth/mfa/enrollment/start",
                                "/api/web/v1/auth/mfa/enrollment/confirm", "/api/web/v1/auth/mfa/challenge", "/api/web/v1/auth/mfa/recovery",
                                "/api/web/v1/auth/mfa/replacement/start", "/api/web/v1/auth/mfa/recovery-codes/replace").authenticated()
                        .anyRequest().denyAll())
                .securityContext(context -> context.securityContextRepository(contexts).requireExplicitSave(true))
                .sessionManagement(session -> session.sessionAuthenticationStrategy(sessions))
                .csrf(config -> config.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CurrentIdentityFilter(identity, problems), SecurityContextHolderFilter.class)
                .addFilterAt(login, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(config -> config.authenticationEntryPoint((request, response, failure) -> problems.write(401, request, response))
                        .accessDeniedHandler((request, response, failure) -> problems.write(403, request, response)))
                .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.logoutUrl("/api/web/v1/auth/logout")
                        .addLogoutHandler((request, response, authentication) -> {
                            if (authentication != null && authentication.getPrincipal() instanceof LoginIdentity principal) {
                                identity.logout(principal);
                            }
                        }).invalidateHttpSession(true).clearAuthentication(true).deleteCookies(cookieName)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setHeader("Cache-Control", "no-store");
                            response.setStatus(204);
                        }))
                .headers(headers -> headers.referrerPolicy(policy -> policy.policy(ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(policy -> policy.policyDirectives(
                                "default-src 'none'; object-src 'none'; frame-ancestors 'none'; base-uri 'none'")));
        return http.build();
    }

    private static final class LoginException extends AuthenticationException {
        private final int status;
        private final int retryAfter;
        LoginException(int status) { this(status, 1); }
        LoginException(int status, int retryAfter) { super("Authentication request refused."); this.status = status; this.retryAfter = retryAfter; }
    }

    private record LoginBody(String email, String password) {
        @Override public String toString() { return "LoginBody[redacted]"; }
    }

    private static final class JsonLoginFilter extends AbstractAuthenticationProcessingFilter {
        private final JsonMapper mapper;
        JsonLoginFilter(ProviderManager manager, JsonMapper mapper) {
            super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/web/v1/auth/login"), manager);
            this.mapper = mapper;
        }

        @Override
        public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
            try {
                if (request.getContentType() == null || !MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(request.getContentType()))) {
                    throw new LoginException(415);
                }
                if (request.getContentLengthLong() > 4096) { throw new LoginException(413); }
                byte[] body = request.getInputStream().readNBytes(4097);
                if (body.length > 4096) { throw new LoginException(413); }
                LoginBody credentials = mapper.readValue(body, LoginBody.class);
                if (credentials == null || credentials.email() == null || credentials.password() == null) { throw new LoginException(400); }
                var token = UsernamePasswordAuthenticationToken.unauthenticated(credentials.email(), credentials.password());
                token.setDetails(request.getRemoteAddr());
                return getAuthenticationManager().authenticate(token);
            } catch (AuthenticationException failure) { throw failure; }
            catch (RuntimeException failure) { throw new LoginException(400); }
        }
    }

    private static final class CurrentIdentityFilter extends OncePerRequestFilter {
        private final WebIdentityService identity;
        private final ApiProblems problems;
        CurrentIdentityFilter(WebIdentityService identity, ApiProblems problems) { this.identity = identity; this.problems = problems; }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginIdentity principal) {
                try {
                    var current = identity.current(principal);
                    if (current.isEmpty()) {
                        SecurityContextHolder.clearContext();
                        var session = request.getSession(false);
                        if (session != null) { session.invalidate(); }
                    } else {
                        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                                principal, null, identity.authorities(current.get()).stream().map(SimpleGrantedAuthority::new).toList()));
                    }
                } catch (RuntimeException failure) {
                    SecurityContextHolder.clearContext();
                    problems.write(503, request, response);
                    return;
                }
            }
            chain.doFilter(request, response);
        }
    }
}
