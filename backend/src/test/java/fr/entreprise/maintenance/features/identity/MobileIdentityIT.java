package fr.entreprise.maintenance.features.identity;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import fr.entreprise.maintenance.features.identity.application.AdministratorBootstrapService;
import fr.entreprise.maintenance.features.identity.application.IdentityActions;
import fr.entreprise.maintenance.features.identity.application.MobileIdentityService;
import fr.entreprise.maintenance.features.identity.application.WebIdentityService;
import fr.entreprise.maintenance.features.identity.domain.ActionTokens;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import fr.entreprise.maintenance.features.identity.publicapi.AccountProvisioning;
import fr.entreprise.maintenance.testinfra.MutableClock;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(MobileIdentityIT.TimeConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MobileIdentityIT {
    @TestConfiguration(proxyBeanMethods = false)
    static class TimeConfiguration { @Bean @Primary MutableClock testClock() { return new MutableClock(); } }
    @Container static final GenericContainer<?> DATABASE = PostgisFixture.container();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { PostgisFixture.properties(registry, DATABASE); }
    @LocalServerPort int port;
    @Autowired AccountProvisioning provisioning;
    @Autowired AdministratorBootstrapService bootstrap;
    @Autowired IdentityActions actions;
    @Autowired ActionTokens actionTokens;
    @Autowired MobileIdentityService mobile;
    @Autowired WebIdentityService web;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    @Autowired MutableClock clock;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private UUID user;
    private String email;
    private String password;

    @BeforeEach void setup() {
        clock.reset(); jdbc.update("DELETE FROM app.auth_rate_bucket");
        email = "mobile-" + UUID.randomUUID() + "@example.invalid";
        password = OpaqueTokens.generate() + " long passphrase";
        user = provisioning.createPendingTechnician(email, password);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?", user);
        jdbc.update("UPDATE app.local_credential SET email_verified_at=now() WHERE user_id=?", user);
    }

    @Test void opaqueCredentialsAreHashedBoundedNoStoreAndNeverAWebSession() throws Exception {
        var response = post("login", Map.of("email", email, "password", password));
        JsonNode pair = ok(response);
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        String access = text(pair, "accessToken"); String refresh = text(pair, "refreshToken");
        assertThat(access.matches("[A-Za-z0-9_-]{43}") && refresh.matches("[A-Za-z0-9_-]{43}") && !access.equals(refresh)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mobile_access_token WHERE fingerprint=?", Integer.class, OpaqueTokens.fingerprint(access))).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mobile_refresh_token WHERE fingerprint=?", Integer.class, OpaqueTokens.fingerprint(refresh))).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.spring_session WHERE principal_name=?", Integer.class, user.toString())).isZero();
        var me = get("/api/mobile/v1/me", access);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(me.body()).get("userId").asText()).isEqualTo(user.toString());
        assertThat(me.body()).doesNotContain("hash", "password", "securityVersion", "sessionId", "Token");
        assertThat(get("/api/web/v1/me", access).statusCode()).isEqualTo(401);
        assertThat(get("/api/mobile/v1/me", refresh).statusCode()).isEqualTo(401);
        assertThat(get("/api/mobile/v1/me?access_token=" + access, null).statusCode()).isEqualTo(401);
    }

    @Test void rotationAndKnownReuseRevokeOnlyTheMatchingFamilyIncludingItsNewAccess() throws Exception {
        JsonNode first = login(); JsonNode unrelated = login();
        JsonNode rotated = ok(post("refresh", Map.of("refreshToken", text(first, "refreshToken"))));
        assertThat(!text(first, "refreshToken").equals(text(rotated, "refreshToken"))).isTrue();
        assertThat(get("/api/mobile/v1/me", text(rotated, "accessToken")).statusCode()).isEqualTo(200);
        assertThat(post("refresh", Map.of("refreshToken", text(first, "refreshToken"))).statusCode()).isEqualTo(401);
        assertThat(get("/api/mobile/v1/me", text(rotated, "accessToken")).statusCode()).isEqualTo(401);
        assertThat(post("refresh", Map.of("refreshToken", text(rotated, "refreshToken"))).statusCode()).isEqualTo(401);
        assertThat(get("/api/mobile/v1/me", text(unrelated, "accessToken")).statusCode()).isEqualTo(200);
        assertThat(post("refresh", Map.of("refreshToken", OpaqueTokens.generate())).statusCode()).isEqualTo(401);
        assertThat(get("/api/mobile/v1/me", text(unrelated, "accessToken")).statusCode()).isEqualTo(200);
    }

    @Test void concurrentRefreshHasOneResponseAndNoSurvivingActiveBranchAfterReplay() throws Exception {
        JsonNode pair = login();
        String old = text(pair, "refreshToken");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(java.util.List.of((Callable<HttpResponse<String>>) () -> post("refresh", Map.of("refreshToken", old)),
                    () -> post("refresh", Map.of("refreshToken", old))));
            var first = results.get(0).get(); var second = results.get(1).get();
            assertThat(java.util.List.of(first.statusCode(), second.statusCode())).containsExactlyInAnyOrder(200, 401);
            JsonNode winner = mapper.readTree((first.statusCode() == 200 ? first : second).body());
            assertThat(get("/api/mobile/v1/me", text(winner, "accessToken")).statusCode()).isEqualTo(401);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mobile_refresh_token t JOIN app.auth_session s USING(session_id) "
                + "WHERE s.user_id=? AND s.revoked_at IS NULL AND t.consumed_at IS NULL AND t.revoked_at IS NULL", Integer.class, user)).isZero();
    }

    @Test void lostCommittedResponseDoesNotPermitSilentGraceOrReconstruction() throws Exception {
        JsonNode pair = login(); String old = text(pair, "refreshToken");
        // The client deliberately discards the first successful response, as after network loss.
        assertThat(post("refresh", Map.of("refreshToken", old)).statusCode()).isEqualTo(200);
        assertThat(post("refresh", Map.of("refreshToken", old)).statusCode()).isEqualTo(401);
        assertThat(get("/api/mobile/v1/me", text(pair, "accessToken")).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.auth_session WHERE user_id=? AND revoked_at IS NULL", Integer.class, user)).isZero();
    }

    @Test void accessIdleAndAbsoluteFamilyDeadlinesAreEnforcedInCommands() throws Exception {
        JsonNode first = login(); clock.advance(301);
        assertThat(get("/api/mobile/v1/me", text(first, "accessToken")).statusCode()).isEqualTo(401);
        JsonNode next = ok(post("refresh", Map.of("refreshToken", text(first, "refreshToken"))));
        clock.advance(7 * 86400 + 1);
        assertThat(post("refresh", Map.of("refreshToken", text(next, "refreshToken"))).statusCode()).isEqualTo(401);
        clock.reset(); jdbc.update("DELETE FROM app.auth_rate_bucket");
        JsonNode absolute = login();
        for (int i = 0; i < 5; i++) {
            clock.advance(6 * 86400 - 1);
            absolute = ok(post("refresh", Map.of("refreshToken", text(absolute, "refreshToken"))));
        }
        assertThat(Instant.parse(text(absolute, "refreshExpiresAt"))).isEqualTo(Instant.parse(text(absolute, "familyExpiresAt")));
        clock.advance(6);
        assertThat(post("refresh", Map.of("refreshToken", text(absolute, "refreshToken"))).statusCode()).isEqualTo(401);
    }

    @Test void currentSuspensionSecurityVersionAndRolesAreCheckedBeforeEachAccessAndRefresh() throws Exception {
        JsonNode pair = login();
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?", user);
        assertThat(get("/api/mobile/v1/me", text(pair, "accessToken")).statusCode()).isEqualTo(401);
        assertThat(post("refresh", Map.of("refreshToken", text(pair, "refreshToken"))).statusCode()).isEqualTo(401);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE',security_version=security_version+1 WHERE user_id=?", user);
        assertThat(get("/api/mobile/v1/me", text(pair, "accessToken")).statusCode()).isEqualTo(401);
        JsonNode changed = login();
        jdbc.update("INSERT INTO app.user_role(user_id,role) VALUES (?,'ADMIN')", user);
        assertThat(get("/api/mobile/v1/me", text(changed, "accessToken")).statusCode()).isEqualTo(401);
        assertThat(post("refresh", Map.of("refreshToken", text(changed, "refreshToken"))).statusCode()).isEqualTo(401);
    }

    @Test void pendingAccountsHaveNoBusinessAuthorityAndClientOrAdminCannotUseMobileLogin() throws Exception {
        jdbc.update("UPDATE app.app_user SET status='PENDING' WHERE user_id=?", user);
        JsonNode pending = login();
        var current = mobile.authenticate(text(pending, "accessToken"));
        assertThat(web.authorities(current.session())).containsExactly("IDENTITY_READ");
        assertThat(get("/api/mobile/v1/technician/missions", text(pending, "accessToken")).statusCode()).isEqualTo(403);
        String clientEmail = "client-" + UUID.randomUUID() + "@example.invalid";
        provisioning.createPendingClient(clientEmail, password);
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.invalid";
        bootstrap.create(adminEmail, password);
        assertThat(post("login", Map.of("email", clientEmail, "password", password)).statusCode()).isEqualTo(401);
        assertThat(post("login", Map.of("email", adminEmail, "password", password)).statusCode()).isEqualTo(401);
        assertThat(post("login", Map.of("email", email, "password", password, "role", "ADMIN")).statusCode()).isEqualTo(400);
    }

    @Test void webCookiesNeverAuthenticateMobileEvenAfterSuccessfulWebLogin() throws Exception {
        var browser = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        String csrf = mapper.readTree(browser.send(HttpRequest.newBuilder(uri("/api/web/v1/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString()).body()).get("token").asText();
        assertThat(browser.send(HttpRequest.newBuilder(uri("/api/web/v1/auth/login")).header("Content-Type", "application/json").header("X-CSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("email", email, "password", password)))).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        assertThat(browser.send(HttpRequest.newBuilder(uri("/api/mobile/v1/me")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }

    @Test void logoutAndResetRevokeTokensWithoutChoosingAFamilyFromThePayload() throws Exception {
        JsonNode first = login(); JsonNode other = login();
        assertThat(post("logout", Map.of("refreshToken", text(first, "refreshToken"), "sessionId", UUID.randomUUID().toString())).statusCode()).isEqualTo(400);
        assertThat(post("logout", Map.of("refreshToken", text(first, "refreshToken"))).statusCode()).isEqualTo(204);
        assertThat(get("/api/mobile/v1/me", text(first, "accessToken")).statusCode()).isEqualTo(401);
        assertThat(get("/api/mobile/v1/me", text(other, "accessToken")).statusCode()).isEqualTo(200);
        String raw = OpaqueTokens.generate();
        actionTokens.issue(email, ActionTokens.Purpose.PASSWORD_RESET, OpaqueTokens.fingerprint(raw), raw, clock.instant(), clock.instant().plusSeconds(1800));
        assertThat(post("password-reset/confirm", Map.of("token", raw, "password", OpaqueTokens.generate() + " changed password")).statusCode()).isEqualTo(204);
        assertThat(get("/api/mobile/v1/me", text(other, "accessToken")).statusCode()).isEqualTo(401);
        assertThat(post("refresh", Map.of("refreshToken", text(other, "refreshToken"))).statusCode()).isEqualTo(401);
    }

    @Test void globalLogoutRevokesEveryDeviceAndPreexistingWebSessions() throws Exception {
        var webSession = web.login(email, password, "192.0.2.1");
        JsonNode first = login(); JsonNode second = login();
        var response = client.send(HttpRequest.newBuilder(uri("/api/mobile/v1/auth/logout-all"))
                .header("Authorization", "Bearer " + text(first, "accessToken")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(get("/api/mobile/v1/me", text(second, "accessToken")).statusCode()).isEqualTo(401);
        assertThat(web.current(webSession)).isEmpty();
    }

    @Test void authorizationStorageFailureRefusesAccessInsteadOfTrustingAPreviousDecision() throws Exception {
        JsonNode pair = login(); String access = text(pair, "accessToken");
        assertThat(get("/api/mobile/v1/me", access).statusCode()).isEqualTo(200);
        try (var connection = PostgisFixture.migrator(DATABASE); var statement = connection.createStatement()) {
            statement.execute("REVOKE SELECT ON app.mobile_access_token FROM maint_app");
            try {
                var failed = get("/api/mobile/v1/me", access);
                assertThat(failed.statusCode()).isEqualTo(503);
                assertThat(failed.body().contains(access) || failed.body().contains("SELECT") || failed.body().contains("Exception")).isFalse();
            } finally { statement.execute("GRANT SELECT ON app.mobile_access_token TO maint_app"); }
        }
        assertThat(get("/api/mobile/v1/me", access).statusCode()).isEqualTo(200);
    }

    private JsonNode login() throws Exception { return ok(post("login", Map.of("email", email, "password", password))); }
    private JsonNode ok(HttpResponse<String> response) throws Exception { assertThat(response.statusCode()).isEqualTo(200); return mapper.readTree(response.body()); }
    private static String text(JsonNode node, String field) { return node.get(field).asText(); }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    private HttpResponse<String> get(String path, String bearer) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)); if (bearer != null) { request.header("Authorization", "Bearer " + bearer); }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> post(String path, Map<String, String> body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri("/api/mobile/v1/auth/" + path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
}
