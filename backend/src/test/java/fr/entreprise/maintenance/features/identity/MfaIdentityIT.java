package fr.entreprise.maintenance.features.identity;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.crypto.spec.SecretKeySpec;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import fr.entreprise.maintenance.features.identity.application.AdministratorBootstrapService;
import fr.entreprise.maintenance.features.identity.domain.ActionTokens;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.publicapi.AccountProvisioning;
import fr.entreprise.maintenance.shared.crypto.VersionedSecretCipher;
import fr.entreprise.maintenance.testinfra.MutableClock;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.bouncycastle.util.encoders.Base32;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(MfaIdentityIT.TimeConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MfaIdentityIT {
    @TestConfiguration(proxyBeanMethods = false)
    static class TimeConfiguration {
        @Bean @Primary MutableClock testClock() { return new MutableClock(); }
    }
    @Container static final GenericContainer<?> DATABASE = PostgisFixture.container();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { PostgisFixture.properties(registry, DATABASE); }
    @LocalServerPort int port;
    @Autowired MutableClock clock;
    @Autowired AdministratorBootstrapService bootstrap;
    @Autowired AccountProvisioning provisioning;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    @Autowired VersionedSecretCipher cipher;
    @Autowired ActionTokens actions;
    private UUID user;
    private String email;
    private String password;

    @BeforeEach
    void setup() {
        clock.reset(); jdbc.update("DELETE FROM app.auth_rate_bucket");
        email = "operator-" + UUID.randomUUID() + "@example.invalid";
        password = UUID.randomUUID() + " operator passphrase";
        user = bootstrap.create(email, password);
        jdbc.update("UPDATE app.local_credential SET email_verified_at=? WHERE user_id=?", java.sql.Timestamp.from(clock.instant()), user);
    }

    @Test
    void enrollmentRequiresCsrfAndProofThenRotatesCookieAndGrantsOnlyTheCurrentAccount() throws Exception {
        Browser browser = login();
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(403);
        assertThat(browser.post("mfa/enrollment/start", Map.of(), null).statusCode()).isEqualTo(403);
        JsonNode enrollment = body(browser.post("mfa/enrollment/start", Map.of()));
        String before = browser.cookie();
        var activation = browser.post("mfa/enrollment/confirm", Map.of("enrollmentId", enrollment.get("enrollmentId").asText(),
                "code", otp(enrollment.get("secret").asText())));
        assertThat(activation.statusCode()).isEqualTo(200);
        assertThat(before.equals(browser.cookie())).isFalse();
        JsonNode codes = mapper.readTree(activation.body()).get("recoveryCodes");
        assertThat(codes.size()).isEqualTo(10);
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.recovery_code WHERE user_id=? AND consumed_at IS NULL", Integer.class, user)).isEqualTo(10);
        String secret = enrollment.get("secret").asText();
        String encrypted = jdbc.queryForObject("SELECT secret_encrypted FROM app.mfa_credential WHERE user_id=?", String.class, user);
        assertThat(encrypted.contains(secret)).isFalse();
        assertThat(cipher.decrypt(encrypted, "identity-totp/" + user).equals(secret)).isTrue();
        assertThat(activation.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    }

    @Test
    void aConsumedTotpStepCannotBeReplayedAfterANewPasswordLogin() throws Exception {
        Browser browser = login();
        JsonNode enrollment = enroll(browser);
        String secret = enrollment.get("secret").asText();
        String used = otp(secret);
        logout(browser);
        Browser next = login();
        assertThat(next.post("mfa/challenge", Map.of("code", used)).statusCode()).isEqualTo(403);
        clock.advance(30);
        assertThat(next.post("mfa/challenge", Map.of("code", otp(secret))).statusCode()).isEqualTo(204);
        assertThat(next.get("/api/web/v1/me").statusCode()).isEqualTo(200);
    }

    @Test
    void recoveryIsSingleUseEvenAcrossConcurrentSessions() throws Exception {
        Browser browser = login();
        JsonNode enrollment = enroll(browser);
        String code = enrollment.get("recoveryCodes").get(0).asText();
        logout(browser);
        Browser first = login(); Browser second = login();
        String csrf1 = first.csrf(); String csrf2 = second.csrf();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(java.util.List.of(
                    (Callable<Integer>) () -> first.post("mfa/recovery", Map.of("code", code), csrf1).statusCode(),
                    () -> second.post("mfa/recovery", Map.of("code", code), csrf2).statusCode()));
            assertThat(java.util.List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(204, 403);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.recovery_code WHERE user_id=? AND consumed_at IS NOT NULL", Integer.class, user)).isEqualTo(1);
    }

    @Test
    void expiredEnrollmentAndStalePasswordProofAreRefusedUntilReauthentication() throws Exception {
        Browser browser = login();
        JsonNode pending = body(browser.post("mfa/enrollment/start", Map.of()));
        clock.advance(301);
        assertThat(browser.post("mfa/enrollment/confirm", Map.of("enrollmentId", pending.get("enrollmentId").asText(),
                "code", otp(pending.get("secret").asText()))).statusCode()).isEqualTo(403);
        assertThat(browser.post("reauthenticate", Map.of("password", password)).statusCode()).isEqualTo(204);
        JsonNode next = body(browser.post("mfa/enrollment/start", Map.of()));
        assertThat(browser.post("mfa/enrollment/confirm", Map.of("enrollmentId", next.get("enrollmentId").asText(),
                "code", otp(next.get("secret").asText()))).statusCode()).isEqualTo(200);
    }

    @Test
    void pendingEnrollmentIsBoundToItsSessionAndAnotherAccountCannotConfirmIt() throws Exception {
        Browser owner = login();
        JsonNode pending = body(owner.post("mfa/enrollment/start", Map.of()));
        Browser anotherSession = login();
        assertThat(anotherSession.post("mfa/enrollment/confirm", Map.of("enrollmentId", pending.get("enrollmentId").asText(),
                "code", otp(pending.get("secret").asText()))).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT secret_encrypted IS NULL FROM app.mfa_credential WHERE user_id=?", Boolean.class, user)).isTrue();
    }

    @Test
    void passwordResetDoesNotRemoveMfaOrProduceAFullAdminSession() throws Exception {
        Browser browser = login(); JsonNode enrollment = enroll(browser);
        String encrypted = jdbc.queryForObject("SELECT secret_encrypted FROM app.mfa_credential WHERE user_id=?", String.class, user);
        String raw = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]); // Synthetic test token, not a credential default.
        actions.issue(email, ActionTokens.Purpose.PASSWORD_RESET, fr.entreprise.maintenance.features.identity.application.LoginAttempts.digest(raw),
                raw, clock.instant(), clock.instant().plusSeconds(1800));
        String next = UUID.randomUUID() + " replacement passphrase";
        assertThat(browser.post("password-reset/confirm", Map.of("token", raw, "password", next)).statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT secret_encrypted FROM app.mfa_credential WHERE user_id=?", String.class, user).equals(encrypted)).isTrue();
        password = next; Browser resetLogin = login();
        assertThat(resetLogin.get("/api/web/v1/me").statusCode()).isEqualTo(403);
        assertThat(resetLogin.post("mfa/enrollment/start", Map.of()).statusCode()).isEqualTo(403);
        assertThat(resetLogin.post("mfa/recovery", Map.of("code", enrollment.get("recoveryCodes").get(0).asText())).statusCode()).isEqualTo(204);
    }

    @Test
    void recoveryRegenerationInvalidatesOldCodesAndFactorReplacementRevokesAllSessions() throws Exception {
        Browser browser = login(); JsonNode first = enroll(browser);
        JsonNode regenerated = body(browser.post("mfa/recovery-codes/replace", Map.of()));
        assertThat(regenerated.get("recoveryCodes").size()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.recovery_code WHERE user_id=? AND consumed_at IS NOT NULL", Integer.class, user)).isEqualTo(10);
        JsonNode replacement = body(browser.post("mfa/replacement/start", Map.of()));
        var result = browser.post("mfa/enrollment/confirm", Map.of("enrollmentId", replacement.get("enrollmentId").asText(),
                "code", otp(replacement.get("secret").asText())));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(result.body()).get("status").asText()).isEqualTo("REAUTHENTICATION_REQUIRED");
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.auth_session WHERE user_id=? AND revoked_at IS NULL", Integer.class, user)).isZero();
        assertThat(jdbc.queryForObject("SELECT security_version FROM app.app_user WHERE user_id=?", Long.class, user)).isEqualTo(1);
        assertThat(first.get("secret").asText().equals(replacement.get("secret").asText())).isFalse();
    }

    @Test
    void mfaAttemptsAreSharedLimitedAndCarryRetryAfter() throws Exception {
        Browser browser = login();
        for (int i = 0; i < 5; i++) { assertThat(browser.post("mfa/challenge", Map.of("code", "000000")).statusCode()).isEqualTo(403); }
        var limited = browser.post("mfa/challenge", Map.of("code", "000000"));
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.headers().firstValue("Retry-After")).isPresent();
    }

    @Test
    void bootstrapNeverPromotesAnExistingClientAndIsNotExposedOverHttp() throws Exception {
        String existingEmail = "client-" + UUID.randomUUID() + "@example.invalid";
        UUID client = provisioning.createPendingClient(existingEmail, password);
        assertThatThrownBy(() -> bootstrap.create(existingEmail, password)).isInstanceOf(IdentityFailure.class);
        assertThat(jdbc.queryForList("SELECT role FROM app.user_role WHERE user_id=?", String.class, client)).containsExactly("CLIENT");
        Browser browser = new Browser();
        assertThat(browser.post("admin/bootstrap", Map.of("email", existingEmail, "password", password, "role", "ADMIN")).statusCode()).isEqualTo(401);
    }

    private String otp(String secret) throws Exception {
        return new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30), 6, "HmacSHA1")
                .generateOneTimePasswordString(new SecretKeySpec(Base32.decode(secret), "HmacSHA1"), clock.instant(), java.util.Locale.ROOT);
    }
    private JsonNode body(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(200);
        return mapper.readTree(response.body());
    }
    private Browser login() throws Exception {
        Browser browser = new Browser();
        var result = browser.post("login", Map.of("email", email, "password", password));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(result.body()).get("status").asText()).isEqualTo("MFA_PENDING");
        return browser;
    }
    private void logout(Browser browser) throws Exception { assertThat(browser.post("logout", Map.of()).statusCode()).isEqualTo(204); }
    private JsonNode enroll(Browser browser) throws Exception {
        JsonNode enrollment = body(browser.post("mfa/enrollment/start", Map.of()));
        JsonNode activated = body(browser.post("mfa/enrollment/confirm", Map.of("enrollmentId", enrollment.get("enrollmentId").asText(),
                "code", otp(enrollment.get("secret").asText()))));
        ((tools.jackson.databind.node.ObjectNode) enrollment).set("recoveryCodes", activated.get("recoveryCodes"));
        return enrollment;
    }
    private final class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
        URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
        HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        String csrf() throws Exception { return mapper.readTree(get("/api/web/v1/auth/csrf").body()).get("token").asText(); }
        String cookie() { return cookies.getCookieStore().getCookies().stream().filter(c -> !c.hasExpired()).map(java.net.HttpCookie::getValue).findFirst().orElse(""); }
        HttpResponse<String> post(String action, Map<String, String> body) throws Exception { return post(action, body, csrf()); }
        HttpResponse<String> post(String action, Map<String, String> body, String csrf) throws Exception {
            var builder = HttpRequest.newBuilder(uri("/api/web/v1/auth/" + action)).header("Content-Type", "application/json");
            if (csrf != null) { builder.header("X-CSRF-TOKEN", csrf); }
            return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
