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
import java.util.stream.IntStream;

import fr.entreprise.maintenance.features.identity.application.IdentityActions;
import fr.entreprise.maintenance.features.identity.domain.ActionTokens;
import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.IdentityAccount;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityRole;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.PasswordHasher;
import fr.entreprise.maintenance.features.notifications.infrastructure.MailDeliveryWorker;
import fr.entreprise.maintenance.shared.crypto.VersionedSecretCipher;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import fr.entreprise.maintenance.testinfra.MailpitFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IdentityActionsIT {
    @Container static final GenericContainer<?> DATABASE = PostgisFixture.container();
    @Container static final GenericContainer<?> MAILPIT = MailpitFixture.container();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        PostgisFixture.properties(registry, DATABASE);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    }
    @LocalServerPort int port;
    @Autowired IdentityActions actions;
    @Autowired ActionTokens tokens;
    @Autowired IdentityStore store;
    @Autowired PasswordHasher hasher;
    @Autowired VersionedSecretCipher cipher;
    @Autowired MailDeliveryWorker worker;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    @Autowired JavaMailSenderImpl mailSender;
    @Autowired fr.entreprise.maintenance.features.identity.infrastructure.persistence.IdentityMaintenance maintenance;

    private IdentityAccount account;
    private String email;
    private String password;

    @BeforeEach
    void provision() throws Exception {
        try (var connection = PostgisFixture.migrator(DATABASE); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM app.mail_outbox"); // This class's disposable fixture only, not a Compose volume.
        }
        jdbc.update("DELETE FROM app.auth_rate_bucket");
        email = "fixture-" + UUID.randomUUID() + "@example.invalid";
        password = UUID.randomUUID() + " long passphrase";
        var address = EmailAddress.parse(email);
        account = store.createPending(address.normalized(), address.display(), hasher.encode(password), IdentityRole.TECHNICIAN, Instant.now());
    }

    @Test
    void emailRequestsAreGenericAndConfirmationRequiresPostCsrfAndCorrectPurpose() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.post("email-verification/requests", Map.of("email", email), null).statusCode()).isEqualTo(403);
        assertThat(browser.post("email-verification/requests", Map.of("email", email), browser.csrf()).statusCode()).isEqualTo(202);
        assertThat(browser.post("email-verification/requests", Map.of("email", "absent@example.invalid"), browser.csrf()).statusCode()).isEqualTo(202);
        String token = latestToken();
        assertThat(browser.get("/api/web/v1/auth/email-verification/confirm?token=" + token).statusCode()).isEqualTo(401);
        assertThat(verified()).isFalse();
        assertThat(browser.post("password-reset/confirm", Map.of("token", token, "password", password), browser.csrf()).statusCode()).isEqualTo(400);
        assertThat(browser.post("email-verification/confirm", Map.of("token", token), browser.csrf()).statusCode()).isEqualTo(204);
        assertThat(verified()).isTrue();
        assertThat(browser.post("email-verification/confirm", Map.of("token", token), browser.csrf()).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT status FROM app.app_user WHERE user_id=?", String.class, account.userId())).isEqualTo("PENDING");
    }

    @Test
    void resendInvalidatesPreviousTokenAndExpiryIsCheckedAtConsumption() {
        actions.request(email, ActionTokens.Purpose.EMAIL_VERIFICATION, "192.0.2.1");
        String first = latestToken();
        actions.request(email, ActionTokens.Purpose.EMAIL_VERIFICATION, "192.0.2.1");
        String second = latestToken();
        assertThatThrownBy(() -> actions.verify(first, "192.0.2.1")).isInstanceOf(IdentityFailure.class);
        jdbc.update("UPDATE app.action_token SET created_at=now()-interval '2 days', expires_at=now()-interval '1 second' WHERE user_id=?", account.userId());
        assertThatThrownBy(() -> actions.verify(second, "192.0.2.1")).isInstanceOf(IdentityFailure.class);
        assertThat(verified()).isFalse();
    }

    @Test
    void resetRevokesAllSessionsKeepsSuspensionAndRejectsRepeatedOrWeakReset() {
        jdbc.update("UPDATE app.local_credential SET email_verified_at=now() WHERE user_id=?", account.userId());
        var credential = store.credential(email).orElseThrow();
        var session = store.finishPasswordLogin(credential, Instant.now(), Instant.now().plusSeconds(1000), "WEB");
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?", account.userId());
        actions.request(email, ActionTokens.Purpose.PASSWORD_RESET, "192.0.2.1");
        String token = latestToken();
        assertThatThrownBy(() -> actions.reset(token, "short", "192.0.2.1")).isInstanceOf(IdentityFailure.class);
        String next = UUID.randomUUID() + " new passphrase";
        actions.reset(token, next, "192.0.2.1");
        assertThat(jdbc.queryForObject("SELECT status FROM app.app_user WHERE user_id=?", String.class, account.userId())).isEqualTo("SUSPENDED");
        assertThat(jdbc.queryForObject("SELECT security_version FROM app.app_user WHERE user_id=?", Long.class, account.userId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT revoked_at IS NOT NULL FROM app.auth_session WHERE session_id=?", Boolean.class, session.sessionId())).isTrue();
        var updated = store.credential(email).orElseThrow();
        assertThat(hasher.matches(next, updated.hash())).isTrue();
        assertThat(hasher.matches(password, updated.hash())).isFalse();
        assertThatThrownBy(() -> actions.reset(token, next, "192.0.2.1")).isInstanceOf(IdentityFailure.class);
    }

    @Test
    void httpResetFromAnExistingWebSessionDoesNotRecreateItsDeletedSecurityContext() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.post("login", Map.of("email", email, "password", password), browser.csrf()).statusCode()).isEqualTo(200);
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(200);
        assertThat(browser.post("password-reset/requests", Map.of("email", email), browser.csrf()).statusCode()).isEqualTo(202);
        String token = latestToken();
        assertThat(browser.post("password-reset/confirm", Map.of("token", token, "password", UUID.randomUUID() + " changed passphrase"),
                browser.csrf()).statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.spring_session WHERE principal_name=?", Integer.class, account.userId().toString())).isZero();
    }

    @Test
    void concurrentResetHasOneWinnerAndOneFinalCredential() throws Exception {
        actions.request(email, ActionTokens.Purpose.PASSWORD_RESET, "192.0.2.1");
        String token = latestToken();
        String next = UUID.randomUUID() + " concurrent passphrase";
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(IntStream.range(0, 2).mapToObj(i -> (Callable<Boolean>) () -> {
                try { actions.reset(token, next, "192.0.2.2"); return true; }
                catch (IdentityFailure failure) { return false; }
            }).toList());
            int successes = 0; for (var result : results) { if (result.get()) { successes++; } }
            assertThat(successes).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT security_version FROM app.app_user WHERE user_id=?", Long.class, account.userId())).isEqualTo(1);
        assertThat(hasher.matches(next, store.credential(email).orElseThrow().hash())).isTrue();
    }

    @Test
    void databaseContainsOnlyTokenDigestAndEncryptedMailAndFailedDeliveryIsRetried() {
        actions.request(email, ActionTokens.Purpose.PASSWORD_RESET, "192.0.2.1");
        String token = latestToken();
        assertThat(jdbc.queryForList("SELECT fingerprint FROM app.action_token", String.class).stream().noneMatch(x -> x.contains(token))).isTrue();
        assertThat(jdbc.queryForList("SELECT encrypted_body FROM app.mail_outbox WHERE encrypted_body IS NOT NULL", String.class)
                .stream().noneMatch(x -> x.contains(token))).isTrue();
        int smtpPort = mailSender.getPort();
        mailSender.setPort(1); // A closed loopback port, never a real recipient.
        try { assertThat(worker.deliverOne()).isTrue(); }
        finally { mailSender.setPort(smtpPort); }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox WHERE attempts>0 AND state='PENDING'", Integer.class)).isGreaterThan(0);
    }

    @Test
    void localSmtpDeliveryIsAcknowledgedAndEncryptedBodyIsRemovedAfterDelivery() throws Exception {
        actions.request(email, ActionTokens.Purpose.EMAIL_VERIFICATION, "192.0.2.1");
        String token = latestToken();
        assertThat(worker.deliverOne()).isTrue();
        assertThat(worker.deliverOne()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox WHERE state='DELIVERED' AND encrypted_body IS NULL", Integer.class))
                .isEqualTo(1);
        var client = HttpClient.newHttpClient();
        var listing = mapper.readTree(client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + MAILPIT.getMappedPort(8025)
                + "/api/v1/messages")).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
        var message = listing.get("messages").get(0).get("ID").asText();
        String content = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + MAILPIT.getMappedPort(8025)
                + "/api/v1/message/" + message)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
        assertThat(content.contains(token)).isTrue(); // Never print the mail body or its action token.
    }

    @Test
    void expiredTemporarySecretsAreCleanedWithoutDependingOnTheJobForAuthorization() {
        Instant now = Instant.now();
        jdbc.update("INSERT INTO app.auth_rate_bucket(bucket_key,window_start,hits,expires_at) VALUES (?,?,1,?)", "f".repeat(64),
                java.sql.Timestamp.from(now.minusSeconds(100)), java.sql.Timestamp.from(now.minusSeconds(1)));
        UUID transaction = UUID.randomUUID();
        jdbc.update("INSERT INTO app.google_oauth_transaction(transaction_id,channel,intention,launch_fingerprint,authorization_encrypted,created_at,expires_at) "
                + "VALUES (?,'WEB','LOGIN',?,?,?,?)", transaction, "f".repeat(64), cipher.encrypt("temporary fixture", "google-authorization/" + transaction),
                java.sql.Timestamp.from(now.minusSeconds(100)), java.sql.Timestamp.from(now.minusSeconds(1)));
        maintenance.sweep();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.auth_rate_bucket WHERE bucket_key=?", Integer.class, "f".repeat(64))).isZero();
        assertThat(jdbc.queryForObject("SELECT authorization_encrypted IS NULL FROM app.google_oauth_transaction WHERE transaction_id=?", Boolean.class, transaction)).isTrue();
        assertThat(store.credential(email)).isPresent();
    }

    @Test
    void finalMailFailureIsBoundedAndRemovesTheSensitiveBody() {
        actions.request(email, ActionTokens.Purpose.PASSWORD_RESET, "192.0.2.1");
        jdbc.update("UPDATE app.mail_outbox SET attempts=4 WHERE recipient=?", email);
        int port = mailSender.getPort(); mailSender.setPort(1);
        try { assertThat(worker.deliverOne()).isTrue(); }
        finally { mailSender.setPort(port); }
        assertThat(jdbc.queryForObject("SELECT state='FAILED' AND attempts=5 AND encrypted_body IS NULL FROM app.mail_outbox WHERE recipient=?", Boolean.class, email)).isTrue();
        assertThat(worker.deliverOne()).isFalse();
    }

    private boolean verified() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT email_verified_at IS NOT NULL FROM app.local_credential WHERE user_id=?", Boolean.class, account.userId()));
    }
    private String latestToken() {
        var rows = jdbc.query("SELECT * FROM app.mail_outbox WHERE recipient=? ORDER BY created_at DESC LIMIT 1",
                (row, index) -> cipher.decrypt(row.getString("encrypted_body"), "mail-outbox/" + row.getObject("message_id") + "/"
                        + row.getString("recipient").length() + ":" + row.getString("recipient") + "/" + row.getString("subject")), email);
        String body = rows.getFirst();
        int start = body.indexOf("#token=") + 7;
        return body.substring(start, start + 43);
    }

    private final class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
        HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        String csrf() throws Exception { return mapper.readTree(get("/api/web/v1/auth/csrf").body()).get("token").asText(); }
        HttpResponse<String> post(String action, Map<String, String> body, String csrf) throws Exception {
            var builder = HttpRequest.newBuilder(uri("/api/web/v1/auth/" + action)).header("Content-Type", "application/json");
            if (csrf != null) { builder.header("X-CSRF-TOKEN", csrf); }
            return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
