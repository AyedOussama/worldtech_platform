package fr.entreprise.maintenance.features.identity;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import fr.entreprise.maintenance.features.identity.application.AdministratorBootstrapService;
import fr.entreprise.maintenance.features.identity.application.GoogleIdentityService;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import fr.entreprise.maintenance.features.identity.publicapi.AccountProvisioning;
import fr.entreprise.maintenance.testinfra.LocalOidcProvider;
import fr.entreprise.maintenance.testinfra.MutableClock;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.identity.google.enabled=true", "app.identity.google.mobile-destinations.technician=https://mobile.example.invalid/auth/return"})
@ActiveProfiles("test")
@Testcontainers
@Import(GoogleIdentityIT.OidcConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GoogleIdentityIT {
    static final LocalOidcProvider OIDC = new LocalOidcProvider();
    @TestConfiguration(proxyBeanMethods = false)
    static class OidcConfiguration {
        @Bean @Primary MutableClock testClock() { return new MutableClock(); }
    }
    @org.springframework.test.context.bean.override.convention.TestBean(name = "googleRegistration", methodName = "testRegistration")
    ClientRegistrationRepository registration;
    static ClientRegistrationRepository testRegistration() { return new InMemoryClientRegistrationRepository(OIDC.registration()); }
    @Container static final GenericContainer<?> DATABASE = PostgisFixture.container();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { PostgisFixture.properties(registry, DATABASE); }
    @AfterAll static void closeProvider() { OIDC.close(); }
    @LocalServerPort int port;
    @Autowired AccountProvisioning provisioning;
    @Autowired AdministratorBootstrapService bootstrap;
    @Autowired MutableClock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    private String email;
    private String password;
    private String subject;
    private UUID user;

    @BeforeEach void setup() {
        clock.reset(); jdbc.update("DELETE FROM app.auth_rate_bucket");
        email = "oidc-" + UUID.randomUUID() + "@example.invalid"; password = OpaqueTokens.generate() + " passphrase";
        subject = UUID.randomUUID().toString(); user = provisioning.createPendingTechnician(email, password); verify(user);
    }
    private void verify(UUID id) {
        jdbc.update("UPDATE app.local_credential SET email_verified_at=now() WHERE user_id=?", id);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?", id);
    }
    private void seedLink(UUID id) {
        jdbc.update("INSERT INTO app.external_identity(identity_id,user_id,provider,issuer,subject,created_at) VALUES (?,?,'google','https://accounts.google.com',?,now())",
                UUID.randomUUID(), id, subject);
    }

    @Test void explicitLinkThenStandardOidcLoginPersistsOnlyLocalContextAndRotatesCookie() throws Exception {
        Browser browser = passwordLogin();
        JsonNode start = start(browser, "identities/google/link");
        assertThat(complete(browser, start, subject, "").statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.external_identity WHERE user_id=?", Integer.class, user)).isEqualTo(1);
        Browser fresh = new Browser(); String before = fresh.cookieAfterCsrf();
        var result = complete(fresh, start(fresh, "google/transactions"), subject, "");
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(before.equals(fresh.cookie())).isFalse();
        assertThat(fresh.get("/api/web/v1/me").statusCode()).isEqualTo(200);
        for (byte[] value : jdbc.queryForList("SELECT attribute_bytes FROM app.spring_session_attributes", byte[].class)) {
            String bytes = new String(value, java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(bytes.contains("OidcUser") || bytes.contains("OAuth2AuthenticationToken") || bytes.contains("code_verifier") || bytes.contains("id_token")).isFalse();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.google_oauth_transaction WHERE authorization_encrypted IS NOT NULL AND completed_at IS NOT NULL", Integer.class)).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"issuer", "audience", "signature", "nonce", "expired", "state"})
    void invalidOidcProofNeverCreatesASessionOrLinksAnIdentity(String fault) throws Exception {
        Browser browser = new Browser();
        int sessions = jdbc.queryForObject("SELECT count(*) FROM app.auth_session", Integer.class);
        assertThat(complete(browser, start(browser, "google/transactions"), subject, fault).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.auth_session", Integer.class)).isEqualTo(sessions);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.external_identity WHERE user_id=?", Integer.class, user)).isZero();
    }

    @Test void identicalEmailDoesNotMergeOrCreateAUserAndUnknownIdentityRequiresAuthorizedOnboarding() throws Exception {
        int users = jdbc.queryForObject("SELECT count(*) FROM app.app_user", Integer.class);
        Browser browser = new Browser();
        assertThat(complete(browser, start(browser, "google/transactions"), subject, "").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.app_user", Integer.class)).isEqualTo(users);
        assertThat(jdbc.queryForList("SELECT role FROM app.user_role WHERE user_id=?", String.class, user)).containsExactly("TECHNICIAN");
    }

    @Test void callbackIsBoundToBrowserAndOneUseAndExpiredTransactionsAreRefused() throws Exception {
        seedLink(user); Browser browser = new Browser(); JsonNode start = start(browser, "google/transactions");
        String authorization = launch(browser, start); String callback = callback(authorization, subject, "");
        assertThat(new Browser().get(callback).statusCode()).isEqualTo(401);
        assertThat(browser.get(callback).statusCode()).isEqualTo(200);
        assertThat(browser.get(callback).statusCode()).isEqualTo(401);
        Browser expired = new Browser(); JsonNode next = start(expired, "google/transactions");
        String nextAuthorization = launch(expired, next); String nextCallback = callback(nextAuthorization, subject, "");
        clock.advance(301);
        assertThat(expired.get(nextCallback).statusCode()).isEqualTo(401);
    }

    @Test void linkRequiresRecentLocalProofAndRejectsIdentityOwnedByAnotherAccount() throws Exception {
        String secondEmail = "other-" + UUID.randomUUID() + "@example.invalid";
        UUID second = provisioning.createPendingClient(secondEmail, password); verify(second); seedLink(second);
        Browser browser = passwordLogin();
        assertThat(complete(browser, start(browser, "identities/google/link"), subject, "").statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT user_id FROM app.external_identity WHERE subject=?", UUID.class, subject)).isEqualTo(second);
        clock.advance(301);
        assertThat(browser.post("identities/google/link", Map.of()).statusCode()).isEqualTo(401);
        assertThat(browser.post("reauthenticate", Map.of("password", password)).statusCode()).isEqualTo(204);
        assertThat(browser.post("identities/google/link", Map.of()).statusCode()).isEqualTo(200);
    }

    @Test void resetOrSuspensionDuringLinkCannotBeBypassedByTheCallback() throws Exception {
        Browser browser = passwordLogin(); JsonNode transaction = start(browser, "identities/google/link");
        String authorization = launch(browser, transaction); String callback = callback(authorization, subject, "");
        jdbc.update("UPDATE app.app_user SET security_version=security_version+1 WHERE user_id=?", user);
        assertThat(browser.get(callback).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.external_identity WHERE user_id=?", Integer.class, user)).isZero();
    }

    @Test void googleAdminRemainsMfaPendingAndCannotLinkFromAnUnprovedAdminSession() throws Exception {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.invalid";
        UUID admin = bootstrap.create(adminEmail, password); verify(admin); seedLink(admin);
        Browser browser = new Browser();
        var result = complete(browser, start(browser, "google/transactions"), subject, "");
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(result.body()).get("status").asText()).isEqualTo("MFA_PENDING");
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(403);
        assertThat(browser.post("identities/google/link", Map.of()).statusCode()).isEqualTo(403);
    }

    @Test void handoffHasNoTokensInUrlRequiresVerifierAndTransactionAndIsSingleUse() throws Exception {
        seedLink(user); String verifier = OpaqueTokens.generate(); JsonNode started = mobileStart(verifier, "technician");
        Browser systemBrowser = new Browser(); var callback = complete(systemBrowser, started, subject, "");
        assertThat(callback.statusCode()).isEqualTo(303);
        String destination = callback.headers().firstValue("Location").orElseThrow();
        assertThat(URI.create(destination).getHost()).isEqualTo("mobile.example.invalid");
        assertThat(destination).doesNotContain("access_token", "refresh_token", "id_token", "accessToken", "refreshToken");
        Map<String, String> returned = LocalOidcProvider.parameters(URI.create(destination).getRawFragment());
        assertThat(systemBrowser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
        assertThat(exchange(UUID.randomUUID().toString(), returned.get("code"), verifier).statusCode()).isEqualTo(401);
        assertThat(exchange(started.get("transactionId").asText(), returned.get("code"), OpaqueTokens.generate()).statusCode()).isEqualTo(401);
        var accepted = exchange(started.get("transactionId").asText(), returned.get("code"), verifier);
        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(accepted.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(accepted.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(exchange(started.get("transactionId").asText(), returned.get("code"), verifier).statusCode()).isEqualTo(401);
    }

    @Test void handoffConcurrentExchangeHasOneWinnerAndExpiredCodeHasNone() throws Exception {
        seedLink(user); String verifier = OpaqueTokens.generate(); JsonNode started = mobileStart(verifier, "technician");
        String destination = complete(new Browser(), started, subject, "").headers().firstValue("Location").orElseThrow();
        String code = LocalOidcProvider.parameters(URI.create(destination).getRawFragment()).get("code");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(java.util.List.of((Callable<Integer>) () -> exchange(started.get("transactionId").asText(), code, verifier).statusCode(),
                    () -> exchange(started.get("transactionId").asText(), code, verifier).statusCode()));
            assertThat(java.util.List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200, 401);
        }
        JsonNode next = mobileStart(verifier, "technician");
        String expiredLink = complete(new Browser(), next, subject, "").headers().firstValue("Location").orElseThrow();
        String expired = LocalOidcProvider.parameters(URI.create(expiredLink).getRawFragment()).get("code"); clock.advance(61);
        assertThat(exchange(next.get("transactionId").asText(), expired, verifier).statusCode()).isEqualTo(401);
    }

    @Test void destinationsAreNotCallerUrlsAndUnlinkRevokesAccessOnlyWithAnAlternative() throws Exception {
        assertThat(mobilePost("google/transactions", Map.of("challenge", GoogleIdentityService.challenge(OpaqueTokens.generate()),
                "destinationId", "attacker"), null).statusCode()).isEqualTo(400);
        seedLink(user); Browser browser = passwordLogin();
        assertThat(browser.post("identities/google/unlink", Map.of(), null).statusCode()).isEqualTo(403);
        assertThat(browser.post("identities/google/unlink", Map.of()).statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.external_identity WHERE user_id=?", Integer.class, user)).isZero();
    }

    @Test void mobileLinkWaitsForBoundBearerAndVerifierInsteadOfLinkingFromTheBrowser() throws Exception {
        JsonNode login = mapper.readTree(mobilePost("login", Map.of("email", email, "password", password), null).body());
        String access = login.get("accessToken").asText(); String verifier = OpaqueTokens.generate();
        var prepared = mobilePost("identities/google/link", Map.of("challenge", GoogleIdentityService.challenge(verifier), "destinationId", "technician"), access);
        assertThat(prepared.statusCode()).isEqualTo(200);
        JsonNode started = mapper.readTree(prepared.body());
        var result = complete(new Browser(), started, subject, "");
        assertThat(result.statusCode()).isEqualTo(303);
        String code = LocalOidcProvider.parameters(URI.create(result.headers().firstValue("Location").orElseThrow()).getRawFragment()).get("code");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.external_identity WHERE user_id=?", Integer.class, user)).isZero();
        Map<String, String> exchange = Map.of("transactionId", started.get("transactionId").asText(), "code", code, "verifier", verifier);
        assertThat(mobilePost("google/exchange", exchange, null).statusCode()).isEqualTo(401);
        var accepted = mobilePost("google/exchange", exchange, access);
        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(accepted.body()).get("status").asText()).isEqualTo("LINKED");
        assertThat(accepted.body()).doesNotContain("accessToken", "refreshToken");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.external_identity WHERE user_id=?", Integer.class, user)).isEqualTo(1);
    }

    @Test void googleHandoffCannotProduceMobileAdminPrivilegesOrSurviveSuspension() throws Exception {
        String adminEmail = "admin-mobile-" + UUID.randomUUID() + "@example.invalid";
        UUID admin = bootstrap.create(adminEmail, password); verify(admin); seedLink(admin);
        String verifier = OpaqueTokens.generate(); JsonNode start = mobileStart(verifier, "technician");
        var result = complete(new Browser(), start, subject, ""); assertThat(result.statusCode()).isEqualTo(303);
        String code = LocalOidcProvider.parameters(URI.create(result.headers().firstValue("Location").orElseThrow()).getRawFragment()).get("code");
        assertThat(exchange(start.get("transactionId").asText(), code, verifier).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.auth_session WHERE user_id=? AND channel='MOBILE'", Integer.class, admin)).isZero();
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?", admin);
        Browser web = new Browser();
        assertThat(complete(web, start(web, "google/transactions"), subject, "").statusCode()).isEqualTo(401);
    }

    @Test void unlinkWithoutValidLocalAlternativeIsRefusedWithoutDeletingTheAssociation() throws Exception {
        seedLink(user); Browser browser = passwordLogin();
        jdbc.update("UPDATE app.local_credential SET password_hash='' WHERE user_id=?", user);
        assertThat(browser.post("identities/google/unlink", Map.of()).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.external_identity WHERE user_id=?", Integer.class, user)).isEqualTo(1);
    }

    @Test void transitorySecretsAreNotWrittenInServerLogsAndLaunchCannotBeReused(CapturedOutput output) throws Exception {
        seedLink(user); Browser browser = new Browser(); JsonNode prepared = start(browser, "google/transactions");
        String authorization = launch(browser, prepared);
        String callback = callback(authorization, subject, "");
        int repeated = browser.get(prepared.get("launchPath").asText()).statusCode();
        assertThat(repeated >= 400 && repeated < 500).isTrue();
        assertThat(browser.get(callback).statusCode()).isEqualTo(200);
        var parameters = LocalOidcProvider.parameters(URI.create(authorization).getRawQuery());
        var callbackParameters = LocalOidcProvider.parameters(URI.create(callback).getRawQuery());
        var launchParameters = LocalOidcProvider.parameters(URI.create(prepared.get("launchPath").asText()).getRawQuery());
        String logs = output.toString();
        for (String value : java.util.List.of(password, parameters.get("nonce"), parameters.get("state"), callbackParameters.get("code"), launchParameters.get("launch"))) {
            assertThat(logs.contains(value)).isFalse();
        }
    }

    @Test void explicitGoogleTechnicianRegistrationCreatesOnlyPendingIdentityAndDraftNotApprovalOrPassword() throws Exception {
        email="new-google-tech-"+UUID.randomUUID()+"@example.invalid";
        Browser browser=new Browser();
        assertThat(complete(browser,start(browser,"google/technician-registration/transactions"),subject,"").statusCode()).isEqualTo(200);
        UUID registered=jdbc.queryForObject("SELECT user_id FROM app.local_credential WHERE email_normalized=?",UUID.class,email);
        assertThat(jdbc.queryForObject("SELECT status FROM app.app_user WHERE user_id=?",String.class,registered)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT password_hash='' AND email_verified_at IS NOT NULL FROM app.local_credential WHERE user_id=?",Boolean.class,registered)).isTrue();
        assertThat(jdbc.queryForList("SELECT role FROM app.user_role WHERE user_id=?",String.class,registered)).containsExactly("TECHNICIAN");
        assertThat(jdbc.queryForObject("SELECT state FROM app.technician_onboarding WHERE user_id=?",String.class,registered)).isEqualTo("DRAFT");
        assertThat(browser.get("/api/web/v1/onboarding/technician").statusCode()).isEqualTo(200);
        assertThat(browser.post("identities/google/unlink",Map.of()).statusCode()).isEqualTo(409);
        assertThat(browser.post("password-reset/requests",Map.of("email",email)).statusCode()).isEqualTo(202);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.action_token WHERE user_id=? AND purpose='PASSWORD_RESET'",Integer.class,registered)).isZero();
        assertThat(browser.post("password-reset/confirm",Map.of("token",OpaqueTokens.generate(),"password",password)).statusCode()).isEqualTo(400);
        assertThat(browser.post("login",Map.of("email",email,"password",password)).statusCode()).isEqualTo(401);
    }

    @Test void explicitGoogleMobileRegistrationKeepsPkceHandoffAndDoesNotCreateABrowserLogin() throws Exception {
        email="new-mobile-google-"+UUID.randomUUID()+"@example.invalid"; String verifier=OpaqueTokens.generate();
        var response=mobilePost("google/technician-registration/transactions",Map.of("challenge",GoogleIdentityService.challenge(verifier),"destinationId","technician"),null);
        assertThat(response.statusCode()).isEqualTo(200); JsonNode started=mapper.readTree(response.body()); Browser browser=new Browser();
        var result=complete(browser,started,subject,""); assertThat(result.statusCode()).isEqualTo(303);
        assertThat(browser.get("/api/web/v1/me").statusCode()).isEqualTo(401);
        String code=LocalOidcProvider.parameters(URI.create(result.headers().firstValue("Location").orElseThrow()).getRawFragment()).get("code");
        assertThat(exchange(started.get("transactionId").asText(),code,OpaqueTokens.generate()).statusCode()).isEqualTo(401);
        JsonNode tokens=mapper.readTree(exchange(started.get("transactionId").asText(),code,verifier).body());
        assertThat(tokens.has("accessToken")).isTrue();
        UUID registered=jdbc.queryForObject("SELECT user_id FROM app.local_credential WHERE email_normalized=?",UUID.class,email);
        assertThat(jdbc.queryForObject("SELECT status FROM app.app_user WHERE user_id=?",String.class,registered)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.technician_onboarding WHERE user_id=?",Integer.class,registered)).isEqualTo(1);
    }

    @Test void clientGoogleRegistrationRequiresTheCurrentInvitationAndVerifiedRecipient() throws Exception {
        email="new-google-client-"+UUID.randomUUID()+"@example.invalid"; String invitation=seedInvitation(email);
        Browser browser=new Browser(); var prepared=browser.post("google/client-registration/transactions",Map.of("email",email,"token",invitation));
        assertThat(prepared.statusCode()).isEqualTo(200);
        assertThat(complete(browser,mapper.readTree(prepared.body()),subject,"").statusCode()).isEqualTo(200);
        UUID registered=jdbc.queryForObject("SELECT user_id FROM app.local_credential WHERE email_normalized=?",UUID.class,email);
        assertThat(jdbc.queryForList("SELECT role FROM app.user_role WHERE user_id=?",String.class,registered)).containsExactly("CLIENT");
        assertThat(jdbc.queryForObject("SELECT status FROM app.app_user WHERE user_id=?",String.class,registered)).isEqualTo("PENDING");
        assertThat(browser.get("/api/web/v1/onboarding/client").statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_onboarding d JOIN app.client_invitation i USING(invitation_id) WHERE d.user_id=? AND i.accepted_user_id=d.user_id",Integer.class,registered)).isEqualTo(1);
        assertThat(browser.post("google/client-registration/transactions",Map.of("email",email,"token",invitation)).statusCode()).isEqualTo(401);
    }

    @Test void mismatchedGoogleEmailOrReplacedInvitationNeverConsumesInvitationOrCreatesAnIdentity() throws Exception {
        email="bound-google-client-"+UUID.randomUUID()+"@example.invalid"; String intended=email; String invitation=seedInvitation(email);
        Browser browser=new Browser(); var prepared=browser.post("google/client-registration/transactions",Map.of("email",email,"token",invitation));
        assertThat(prepared.statusCode()).isEqualTo(200); email="different-"+email;
        assertThat(complete(browser,mapper.readTree(prepared.body()),subject,"").statusCode()).isEqualTo(401);
        email=intended;
        var next=browser.post("google/client-registration/transactions",Map.of("email",email,"token",invitation));
        assertThat(next.statusCode()).isEqualTo(200);
        jdbc.update("UPDATE app.client_invitation_link SET revoked_at=? WHERE fingerprint=?",java.sql.Timestamp.from(clock.instant()),OpaqueTokens.fingerprint(invitation));
        assertThat(complete(browser,mapper.readTree(next.body()),subject,"").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.local_credential WHERE email_normalized=?",Integer.class,email)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_invitation WHERE email_normalized=? AND accepted_at IS NOT NULL",Integer.class,email)).isZero();
    }

    @Test void explicitGoogleRegistrationStillNeverMergesAnExistingMailboxOrExternalIdentity() throws Exception {
        Browser browser=new Browser();
        int users=jdbc.queryForObject("SELECT count(*) FROM app.app_user",Integer.class);
        assertThat(complete(browser,start(browser,"google/technician-registration/transactions"),subject,"").statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.app_user",Integer.class)).isEqualTo(users);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.external_identity WHERE user_id=?",Integer.class,user)).isZero();
        seedLink(user); email="other-new-google-"+UUID.randomUUID()+"@example.invalid";
        assertThat(complete(browser,start(browser,"google/technician-registration/transactions"),subject,"").statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.app_user",Integer.class)).isEqualTo(users);
        assertThat(jdbc.queryForObject("SELECT user_id FROM app.external_identity WHERE subject=?",UUID.class,subject)).isEqualTo(user);
    }

    private String seedInvitation(String recipient) {
        UUID inviter=bootstrap.create("fixture-inviter-"+UUID.randomUUID()+"@example.invalid",password);
        UUID id=UUID.randomUUID(); String token=OpaqueTokens.generate();
        jdbc.update("INSERT INTO app.client_invitation(invitation_id,email_normalized,email_display,invited_by,last_sent_by,created_at,expires_at) VALUES (?,?,?,?,?,?,?)",
                id,recipient,recipient,inviter,inviter,java.sql.Timestamp.from(clock.instant()),java.sql.Timestamp.from(clock.instant().plusSeconds(172800)));
        jdbc.update("INSERT INTO app.client_invitation_link(fingerprint,invitation_id,created_at,expires_at) VALUES (?,?,?,?)",
                OpaqueTokens.fingerprint(token),id,java.sql.Timestamp.from(clock.instant()),java.sql.Timestamp.from(clock.instant().plusSeconds(172800)));
        return token;
    }

    private JsonNode mobileStart(String verifier, String destination) throws Exception {
        var response = mobilePost("google/transactions", Map.of("challenge", GoogleIdentityService.challenge(verifier), "destinationId", destination), null);
        assertThat(response.statusCode()).isEqualTo(200); return mapper.readTree(response.body());
    }
    private HttpResponse<String> exchange(String transaction, String code, String verifier) throws Exception {
        return mobilePost("google/exchange", Map.of("transactionId", transaction, "code", code, "verifier", verifier), null);
    }
    private HttpResponse<String> mobilePost(String path, Map<String, String> body, String access) throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/mobile/v1/auth/" + path)).header("Content-Type", "application/json");
        if (access != null) { request.header("Authorization", "Bearer " + access); }
        return HttpClient.newHttpClient().send(request.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private Browser passwordLogin() throws Exception {
        Browser browser = new Browser(); assertThat(browser.post("login", Map.of("email", email, "password", password)).statusCode()).isEqualTo(200); return browser;
    }
    private JsonNode start(Browser browser, String path) throws Exception {
        var response = browser.post(path, Map.of()); assertThat(response.statusCode()).isEqualTo(200); return mapper.readTree(response.body());
    }
    private String launch(Browser browser, JsonNode started) throws Exception {
        var response = browser.get(started.get("launchPath").asText());
        assertThat(response.statusCode()).isEqualTo(302); return response.headers().firstValue("Location").orElseThrow();
    }
    private HttpResponse<String> complete(Browser browser, JsonNode started, String subject, String fault) throws Exception {
        return browser.get(callback(launch(browser, started), subject, fault));
    }
    private String callback(String authorization, String subject, String fault) throws Exception {
        var params = LocalOidcProvider.parameters(URI.create(authorization).getRawQuery());
        String code = OIDC.code(authorization, subject, email, fault);
        String state = fault.equals("state") ? OpaqueTokens.generate() : params.get("state");
        return "/login/oauth2/code/google?code=" + code + "&state=" + state;
    }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    private final class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        String csrf() throws Exception { return mapper.readTree(get("/api/web/v1/auth/csrf").body()).get("token").asText(); }
        String cookie() { return cookies.getCookieStore().getCookies().stream().filter(c -> !c.hasExpired()).map(java.net.HttpCookie::getValue).findFirst().orElse(""); }
        String cookieAfterCsrf() throws Exception { csrf(); return cookie(); }
        HttpResponse<String> post(String path, Map<String, String> body) throws Exception { return post(path, body, csrf()); }
        HttpResponse<String> post(String path, Map<String, String> body, String csrf) throws Exception {
            var request = HttpRequest.newBuilder(uri("/api/web/v1/auth/" + path)).header("Content-Type", "application/json");
            if (csrf != null) { request.header("X-CSRF-TOKEN", csrf); }
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
