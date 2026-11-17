package fr.entreprise.maintenance.bootstrap.security;

import jakarta.servlet.DispatcherType;

import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration(proxyBeanMethods = false)
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
public class HttpSecurityConfiguration {

    @Bean
    @Order(99)
    SecurityFilterChain securityFilterChain(HttpSecurity http, ApiProblems problems) throws Exception {
        var internalError = new AndRequestMatcher(
                new DispatcherTypeRequestMatcher(DispatcherType.ERROR),
                PathPatternRequestMatcher.withDefaults().matcher("/error"));

        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(internalError).permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                problems.write(401, request, response))
                        .accessDeniedHandler((request, response, exception) ->
                                problems.write(403, request, response)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .referrerPolicy(policy -> policy.policy(ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(policy -> policy.policyDirectives(
                                "default-src 'none'; object-src 'none'; frame-ancestors 'none'; base-uri 'none'")));
        // CSRF remains enabled. No authentication endpoint exists before lot 03.
        return http.build();
    }

    @Bean
    WebSecurityCustomizer rejectedRequestHandler(ApiProblems problems) {
        return web -> web.requestRejectedHandler((request, response, exception) ->
                problems.write(400, request, response));
    }
}
