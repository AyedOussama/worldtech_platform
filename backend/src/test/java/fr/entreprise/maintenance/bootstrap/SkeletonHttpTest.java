package fr.entreprise.maintenance.bootstrap;

import java.util.UUID;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import fr.entreprise.maintenance.bootstrap.health.HealthController;
import fr.entreprise.maintenance.bootstrap.http.ApiErrorController;
import fr.entreprise.maintenance.shared.http.RequestTrace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import fr.entreprise.maintenance.bootstrap.security.HttpSecurityConfiguration;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import fr.entreprise.maintenance.shared.http.RequestTraceFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {HealthController.class, ApiErrorController.class},
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({HttpSecurityConfiguration.class, ApiProblems.class, RequestTraceFilter.class})
@ActiveProfiles("test")
class SkeletonHttpTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext context;

    @Test
    void onlyHealthAndInternalErrorControllersAreInstalled() {
        var mappings = context.getBean(RequestMappingHandlerMapping.class);
        assertThat(mappings.getHandlerMethods().values().stream().map(HandlerMethod::getBeanType).distinct())
                .containsExactlyInAnyOrder(HealthController.class, ApiErrorController.class);
    }

    @Test
    void healthExposesOnlyProcessStatus() throws Exception {
        MvcResult result = mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();
        assertThat(UUID.fromString(result.getResponse().getHeader(RequestTrace.HEADER))).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/unknown", "/health/", "/health/private", "/login", "/logout", "/error",
            "/actuator", "/actuator/health", "/actuator/env", "/actuator/heapdump", "/v3/api-docs",
            "/swagger-ui/index.html", "/api/web/v1/auth/login", "/api/mobile/v1/auth/login",
            "/api/web/v1/client/tickets", "/api/mobile/v1/technician/missions"})
    void everyUnlistedReadIsRefusedWithoutRedirectOrInternalDetails(String path) throws Exception {
        MvcResult result = mvc.perform(get(path).accept(MediaType.TEXT_HTML)
                        .queryParam("diagnostic", "caller-controlled-marker"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.type").value("urn:maintenance:problem:authentication-required"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.properties").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("caller-controlled-marker", "java.lang", "stackTrace", "<html");
    }

    @ParameterizedTest
    @ValueSource(strings = {"HEAD", "OPTIONS"})
    void getHealthPermissionDoesNotImplicitlyPermitOtherReadMethods(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/health"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void mutationsWithoutCsrfAreRefusedEvenOnHealth(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/health"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void validCsrfDoesNotGrantPermissionToAnUnlistedRoute() throws Exception {
        mvc.perform(post("/api/web/v1/client/tickets").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void invalidCsrfIsRefused() throws Exception {
        mvc.perform(post("/health").with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "CLIENT", "TECHNICIAN"})
    void evenASyntheticAuthenticatedRoleCannotAccessUnlistedOperations(String role) throws Exception {
        mvc.perform(get("/api/web/v1/admin/tickets").with(user("synthetic-actor").roles(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void noDefaultUserOrPasswordAuthenticationIsInstalled() throws Exception {
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
        mvc.perform(get("/api/web/v1/private").with(httpBasic("synthetic-actor", "synthetic-value")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mobile/v1/private").header(HttpHeaders.AUTHORIZATION, "Bearer synthetic-value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void traceIsServerGeneratedAndMatchesBodyAndHeader() throws Exception {
        MvcResult result = mvc.perform(get("/unknown").header(RequestTrace.HEADER, "caller-controlled-marker"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        String trace = result.getResponse().getHeader(RequestTrace.HEADER);
        assertThat(UUID.fromString(trace)).isNotNull();
        assertThat(result.getResponse().getContentAsString())
                .contains("\"traceId\":\"" + trace + "\"", "urn:maintenance:request:" + trace)
                .doesNotContain("caller-controlled-marker");
    }

    @Test
    void internalErrorDispatchIsSanitizedButDirectErrorRouteRemainsProtected() throws Exception {
        mvc.perform(get("/error").with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        })
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "internal-diagnostic-marker")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("internal-marker")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("internal-diagnostic-marker"))));
        mvc.perform(get("/error")).andExpect(status().isUnauthorized());
    }

    @Test
    void crossOriginPreflightDoesNotOpenPrivateRoutes() throws Exception {
        mvc.perform(options("/api/web/v1/private")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.invalid")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
