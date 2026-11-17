package fr.entreprise.maintenance.bootstrap.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import fr.entreprise.maintenance.features.identity.application.MobileIdentityService;
import fr.entreprise.maintenance.features.identity.application.WebIdentityService;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration(proxyBeanMethods = false)
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
public class IdentityMobileSecurityConfiguration {
    @Bean
    @Order(20)
    SecurityFilterChain identityMobileChain(HttpSecurity http, ApiProblems problems, MobileIdentityService mobile, WebIdentityService web) throws Exception {
        var provider = new AuthenticationProvider() {
            @Override public Authentication authenticate(Authentication authentication) {
                var current = mobile.authenticate((String) authentication.getCredentials());
                return UsernamePasswordAuthenticationToken.authenticated(current.identity(), null,
                        web.authorities(current.session()).stream().map(SimpleGrantedAuthority::new).toList());
            }
            @Override public boolean supports(Class<?> type) { return type == OpaqueBearer.class; }
        };
        // A web cookie never authenticates this channel, including MFA_PENDING administrators.
        http.securityMatcher("/api/mobile/v1/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .csrf(AbstractHttpConfigurer::disable) // Only this cookie-free, stateless channel, never the web chain.
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(new BearerFilter(new ProviderManager(provider), problems), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/mobile/v1/auth/login", "/api/mobile/v1/auth/refresh", "/api/mobile/v1/auth/logout",
                                "/api/mobile/v1/auth/email-verification/requests", "/api/mobile/v1/auth/email-verification/confirm",
                                "/api/mobile/v1/auth/password-reset/requests", "/api/mobile/v1/auth/password-reset/confirm").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/mobile/v1/auth/google/transactions", "/api/mobile/v1/auth/google/exchange").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/mobile/v1/auth/google/technician-registration/transactions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/mobile/v1/auth/reauthenticate", "/api/mobile/v1/auth/identities/google/link",
                                "/api/mobile/v1/auth/identities/google/unlink").hasAuthority("IDENTITY_READ")
                        .requestMatchers(HttpMethod.GET, "/api/mobile/v1/me").hasAuthority("IDENTITY_READ")
                        .requestMatchers(HttpMethod.POST,"/api/mobile/v1/onboarding/technician/register").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/mobile/v1/onboarding/technician","/api/mobile/v1/onboarding/technician/history").hasAuthority("IDENTITY_READ")
                        .requestMatchers(HttpMethod.POST,"/api/mobile/v1/onboarding/technician/start","/api/mobile/v1/onboarding/technician/profile","/api/mobile/v1/onboarding/technician/submit").hasAuthority("IDENTITY_READ")
                        .requestMatchers(HttpMethod.POST, "/api/mobile/v1/auth/logout-all").hasAuthority("IDENTITY_READ")
                        .anyRequest().denyAll())
                .exceptionHandling(config -> config.authenticationEntryPoint((request, response, failure) -> problems.write(401, request, response))
                        .accessDeniedHandler((request, response, failure) -> problems.write(403, request, response)));
        return http.build();
    }

    private static final class OpaqueBearer extends AbstractAuthenticationToken {
        private String token;
        OpaqueBearer(String token) { super(Set.of()); this.token = token; }
        @Override public Object getPrincipal() { return "opaque-mobile-request"; }
        @Override public Object getCredentials() { return token; }
        @Override public void eraseCredentials() { super.eraseCredentials(); token = null; }
    }

    private static final class BearerFilter extends OncePerRequestFilter {
        private final ProviderManager manager;
        private final ApiProblems problems;
        BearerFilter(ProviderManager manager, ApiProblems problems) { this.manager = manager; this.problems = problems; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            var headers = Collections.list(request.getHeaders("Authorization"));
            if (!headers.isEmpty()) {
                OpaqueBearer input = null;
                try {
                    if (headers.size() != 1 || !headers.getFirst().matches("(?i:Bearer) [A-Za-z0-9_-]{43}")) {
                        problems.write(401, request, response); return;
                    }
                    input = new OpaqueBearer(headers.getFirst().substring(7));
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(manager.authenticate(input));
                    SecurityContextHolder.setContext(context);
                } catch (IdentityFailure failure) {
                    SecurityContextHolder.clearContext(); problems.write(401, request, response); return;
                } catch (RuntimeException failure) {
                    SecurityContextHolder.clearContext(); problems.write(503, request, response); return;
                } finally { if (input != null) { input.eraseCredentials(); } }
            }
            chain.doFilter(request, response);
        }
    }
}
