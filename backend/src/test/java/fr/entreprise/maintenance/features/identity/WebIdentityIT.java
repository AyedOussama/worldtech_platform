package fr.entreprise.maintenance.features.identity;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.IdentityAccount;
import fr.entreprise.maintenance.features.identity.domain.IdentityRole;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.PasswordHasher;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebIdentityIT {
    @Container
    static final GenericContainer<?> DATABASE = PostgisFixture.container();
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) { PostgisFixture.properties(registry, DATABASE); }

    @LocalServerPort int port;
    @Autowired IdentityStore store;
    @Autowired PasswordHasher passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;

    private String email;
    private String password;
    private IdentityAccount account;

    @BeforeEach
    void provisionSyntheticAccount() {
        jdbc.update("DELETE FROM app.auth_rate_bucket");
        email = "fixture-" + UUID.randomUUID() + "@example.invalid";
        password = UUID.randomUUID() + " some spaces";
        var address = EmailAddress.parse(email);
        account = store.createPending(address.normalized(), address.display(), passwords.encode(password), IdentityRole.CLIENT, Instant.now());
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?", account.userId());
        jdbc.update("UPDATE app.local_credential SET email_verified_at=now() WHERE user_id=?", account.userId());
    }

    @Test
    void loginRotatesSessionPersistsContextAndLogoutRevokesIt() throws Exception {
        Browser browser = new Browser();
        String csrf = browser.csrf();
        String oldCookie = browser.cookie();
        HttpResponse<String> login = browser.login(email.toUpperCase(java.util.Locale.ROOT), password, csrf);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(!oldCookie.equals(browser.cookie())).isTrue();
        String sessionCookie = login.headers().allValues("Set-Cookie").stream().filter(x -> x.startsWith("MAINT_TEST_SESSION=")).findFirst().orElseThrow();
        assertThat(sessionCookie).contains("HttpOnly", "SameSite=Lax", "Path=/").doesNotContain("Domain=");
        HttpResponse<String> me = browser.get("/api/web/v1/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(me.body()).get("userId").asText()).isEqualTo(account.userId().toString());
        assertThat(me.body()).doesNotContain("password", "hash", "sessionId", "securityVersion", "token");
        assertThat(me.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.spring_session WHERE principal_name=?", Integer.class,
                account.userId().toString())).isEqualTo(1);
        // A pre-login token cannot be used for logout after fixation/CSRF rotation.
        assertThat(browser.post("/api/web/v1/auth/logout", "{}", csrf).statusCode()).isEqualTo(403);
        assertThat(browser.post("/api/web/v1/auth/logout", "{}", browser.csrf()).statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.auth_session WHERE user_id=? AND revoked_at IS NULL", Integer.class,
                account.userId())).isZero();
    }

    @Test
    void loginAndLogoutRequireCsrf() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login(email, password, null).statusCode()).isEqualTo(403);
        assertThat(browser.login(email, password, browser.csrf()).statusCode()).isEqualTo(200);
        assertThat(browser.post("/api/web/v1/auth/logout", "{}", null).statusCode()).isEqualTo(403);
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(200);
        assertThat(browser.get("/api/web/v1/auth/logout").statusCode()).isEqualTo(403);
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(200);
    }

    @Test
    void absentBadPasswordAndSuspendedAccountsHaveTheSameSafeResponse() throws Exception {
        Browser browser = new Browser();
        String csrf = browser.csrf();
        var absent = browser.login("absent@example.invalid", password, csrf);
        var bad = browser.login(email, "not-the-password", csrf);
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?", account.userId());
        var suspended = browser.login(email, password, csrf);
        for (var response : java.util.List.of(absent, bad, suspended)) {
            assertThat(response.statusCode()).isEqualTo(401);
            JsonNode body = mapper.readTree(response.body());
            assertThat(body.get("code").asText()).isEqualTo("AUTHENTICATION_REQUIRED");
            assertThat(response.body()).doesNotContain(email, password, "SUSPENDED", "absent@example.invalid");
            assertThat(response.headers().firstValue("Location")).isEmpty();
        }
    }

    @Test
    void bodyIsBoundedAndPrivilegedUnknownFieldsAreRejected() throws Exception {
        Browser browser = new Browser();
        String csrf = browser.csrf();
        assertThat(browser.post("/api/web/v1/auth/login", mapper.writeValueAsString(Map.of(
                "email", email, "password", password, "role", "ADMIN")), csrf).statusCode()).isEqualTo(400);
        assertThat(browser.post("/api/web/v1/auth/login", " ".repeat(4100), csrf).statusCode()).isEqualTo(413);
        String valid = mapper.writeValueAsString(Map.of("email", email, "password", password));
        assertThat(browser.post("/api/web/v1/auth/login", valid + " {}", csrf).statusCode()).isEqualTo(400);
        String duplicate = "{\"email\":\"absent@example.invalid\",\"email\":" + mapper.writeValueAsString(email)
                + ",\"password\":" + mapper.writeValueAsString(password) + "}";
        assertThat(browser.post("/api/web/v1/auth/login", duplicate, csrf).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.auth_session WHERE user_id=?", Integer.class, account.userId())).isZero();
        assertThat(jdbc.queryForList("SELECT role FROM app.user_role WHERE user_id=?", String.class, account.userId())).containsExactly("CLIENT");
    }

    @Test
    void absentAndPresentBadCredentialsExecuteComparableBoundedCostPathsWithoutClaimingTimingEquivalence() throws Exception {
        long[] present = new long[4]; long[] absent = new long[4];
        Browser browser = new Browser(); String csrf = browser.csrf();
        for (int i = 0; i < 4; i++) {
            long start = System.nanoTime();
            assertThat(browser.login(email, "incorrect passphrase", csrf).statusCode()).isEqualTo(401);
            present[i] = (System.nanoTime() - start) / 1_000_000;
            start = System.nanoTime();
            assertThat(browser.login("absent-" + UUID.randomUUID() + "@example.invalid", "incorrect passphrase", csrf).statusCode()).isEqualTo(401);
            absent[i] = (System.nanoTime() - start) / 1_000_000;
        }
        java.util.Arrays.sort(present); java.util.Arrays.sort(absent);
        System.out.println("Login timing smoke probe, 4 samples each: present_wrong_median_ms=" + present[2]
                + ", absent_median_ms=" + absent[2] + "; not a statistical timing or production load proof.");
    }

    @Test
    void suspensionRevocationAndAbsoluteDeadlineAreEffectiveOnNextRequest() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login(email, password, browser.csrf()).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?", account.userId());
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?", account.userId());
        assertThat(browser.login(email, password, browser.csrf()).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE app.auth_session SET revoked_at=now() WHERE user_id=?", account.userId());
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
        assertThat(browser.login(email, password, browser.csrf()).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE app.auth_session SET created_at=now()-interval '9 hours', expires_at=now()-interval '1 second' WHERE user_id=?",
                account.userId());
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
    }

    @Test
    void securityVersionAndJdbcIdleExpiryInvalidateOldCookies() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login(email, password, browser.csrf()).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE app.app_user SET security_version=security_version+1 WHERE user_id=?", account.userId());
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
        assertThat(browser.login(email, password, browser.csrf()).statusCode()).isEqualTo(200);
        long previous = Instant.now().minusSeconds(1900).toEpochMilli();
        jdbc.update("UPDATE app.spring_session SET last_access_time=?,expiry_time=? WHERE principal_name=?", previous,
                Instant.now().minusSeconds(100).toEpochMilli(), account.userId().toString());
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
    }

    @Test
    void adminIsMfaPendingAndNeverReceivesEffectiveAdminBeforeMfa() throws Exception {
        jdbc.update("INSERT INTO app.user_role(user_id,role) VALUES (?,'ADMIN')", account.userId());
        Browser browser = new Browser();
        var login = browser.login(email, password, browser.csrf());
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(login.body()).get("status").asText()).isEqualTo("MFA_PENDING");
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(403);
        assertThat(browser.get("/api/web/v1/admin/tickets").statusCode()).isEqualTo(403);
        assertThat(browser.get("/api/mobile/v1/technician/missions").statusCode()).isEqualTo(401);
    }

    @Test
    void storedSecurityContextNeverContainsPasswordOrPasswordHash() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.login(email, password, browser.csrf()).statusCode()).isEqualTo(200);
        String hash = jdbc.queryForObject("SELECT password_hash FROM app.local_credential WHERE user_id=?", String.class, account.userId());
        for (byte[] value : jdbc.queryForList("SELECT attribute_bytes FROM app.spring_session_attributes", byte[].class)) {
            String serialized = new String(value, java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(serialized.contains(password) || serialized.contains(hash)).isFalse();
        }
    }

    @Test
    void distributedAttemptLimitSurvivesDifferentSessionsAndCannotBeBypassedByForwardedHeaders() throws Exception {
        for (int i = 0; i < 10; i++) {
            Browser browser = new Browser();
            assertThat(browser.login(email, "incorrect", browser.csrf()).statusCode()).isEqualTo(401);
        }
        Browser browser = new Browser();
        var request = HttpRequest.newBuilder(uri("/api/web/v1/auth/login")).header("Content-Type", "application/json")
                .header("X-CSRF-TOKEN", browser.csrf()).header("X-Forwarded-For", "192.0.2.1")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("email", email, "password", password)))).build();
        var response = browser.client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(response.headers().firstValue("Retry-After")).isPresent();
    }

    @Test
    void rateReservationsAreAtomicAcrossRealConcurrentTransactions() throws Exception {
        String key = "a".repeat(64);
        Instant start = Instant.now();
        try (var executor = Executors.newFixedThreadPool(6)) {
            var work = IntStream.range(0, 18).mapToObj(i -> (Callable<Boolean>) () ->
                    store.reserveAttempt(key, start, start.plusSeconds(60), 3)).toList();
            int accepted = 0;
            for (var result : executor.invokeAll(work)) { if (result.get()) { accepted++; } }
            assertThat(accepted).isEqualTo(3);
        }
        assertThat(jdbc.queryForObject("SELECT hits FROM app.auth_rate_bucket WHERE bucket_key=?", Integer.class, key)).isEqualTo(3);
    }

    @Test
    void previousLoginInsertShapeStillWorksAfterAdditiveRecentAuthenticationMigration() {
        UUID session = UUID.randomUUID();
        jdbc.update("INSERT INTO app.auth_session(session_id,user_id,channel,security_version,created_at,expires_at) "
                + "VALUES (?,?,'WEB',0,now(),now()+interval '8 hours')", session, account.userId());
        assertThat(jdbc.queryForObject("SELECT authenticated_at IS NOT NULL FROM app.auth_session WHERE session_id=?", Boolean.class, session)).isTrue();
    }

    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }

    private final class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> post(String path, String body, String csrf) throws Exception {
            var builder = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json");
            if (csrf != null) { builder.header("X-CSRF-TOKEN", csrf); }
            return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
        String csrf() throws Exception {
            var response = get("/api/web/v1/auth/csrf");
            assertThat(response.statusCode()).isEqualTo(200);
            return mapper.readTree(response.body()).get("token").asText();
        }
        String cookie() { return cookies.getCookieStore().getCookies().stream().filter(c -> !c.hasExpired())
                .filter(c -> c.getName().equals("MAINT_TEST_SESSION")).map(HttpCookie::getValue).findFirst().orElse(""); }
        HttpResponse<String> login(String email, String password, String csrf) throws Exception {
            return post("/api/web/v1/auth/login", mapper.writeValueAsString(Map.of("email", email, "password", password)), csrf);
        }
    }
}
