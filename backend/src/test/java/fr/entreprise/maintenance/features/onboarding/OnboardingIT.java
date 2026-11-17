package fr.entreprise.maintenance.features.onboarding;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import javax.crypto.spec.SecretKeySpec;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import fr.entreprise.maintenance.features.identity.application.AdministratorBootstrapService;
import fr.entreprise.maintenance.features.identity.publicapi.AccountProvisioning;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import fr.entreprise.maintenance.features.notifications.infrastructure.MailDeliveryWorker;
import fr.entreprise.maintenance.shared.crypto.VersionedSecretCipher;
import fr.entreprise.maintenance.testinfra.MailpitFixture;
import fr.entreprise.maintenance.testinfra.MutableClock;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.bouncycastle.util.encoders.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(OnboardingIT.TimeConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class OnboardingIT {
    @TestConfiguration(proxyBeanMethods=false)
    static class TimeConfiguration { @Bean @Primary MutableClock testClock() { return new MutableClock(); } }
    @Container static final GenericContainer<?> DATABASE=PostgisFixture.container();
    @Container static final GenericContainer<?> MAILPIT=MailpitFixture.container();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        PostgisFixture.properties(registry,DATABASE); registry.add("spring.mail.port",() -> MAILPIT.getMappedPort(1025));
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    @Autowired MutableClock clock;
    @Autowired AdministratorBootstrapService bootstrap;
    @Autowired AccountProvisioning provisioning;
    @Autowired VersionedSecretCipher cipher;
    @Autowired MailDeliveryWorker mail;
    @LocalServerPort int port;
    private String email;
    private String password;
    private String adminEmail;
    private UUID adminUser;
    private Browser admin;
    private static final String CLIENT="/api/web/v1/onboarding/client";
    private static final String TECH="/api/mobile/v1/onboarding/technician";
    private static final String INVITATIONS="/api/web/v1/admin/client-invitations";
    private static final String CLIENT_ADMIN="/api/web/v1/admin/onboardings/clients";
    private static final String TECH_ADMIN="/api/web/v1/admin/onboardings/technicians";

    @BeforeEach void setup() throws Exception {
        clock.reset(); jdbc.update("DELETE FROM app.auth_rate_bucket");
        try (var connection=PostgisFixture.migrator(DATABASE); var statement=connection.createStatement()) {
            statement.execute("DELETE FROM app.mail_outbox"); // Only this test's ephemeral Mailpit/DB fixture.
        }
        email="admission-"+UUID.randomUUID()+"@example.invalid"; password=OpaqueTokens.generate()+" passphrase";
        adminEmail="admin-"+UUID.randomUUID()+"@example.invalid"; adminUser=bootstrap.create(adminEmail,password); verifyInFixture(adminUser);
        admin=webLogin(adminEmail);
        JsonNode enrollment=json(admin.post("/api/web/v1/auth/mfa/enrollment/start",Map.of()),200);
        String secret=enrollment.get("secret").asText();
        String otp=new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30),6,"HmacSHA1")
                .generateOneTimePasswordString(new SecretKeySpec(Base32.decode(secret),"HmacSHA1"),clock.instant(),java.util.Locale.ROOT);
        json(admin.post("/api/web/v1/auth/mfa/enrollment/confirm",Map.of("enrollmentId",enrollment.get("enrollmentId").asText(),"code",otp)),200);
    }

    @Test void clientInvitationVerificationDraftSubmitAndApprovalRemainSeparate() throws Exception {
        JsonNode invitation=invite(email); String token=queuedToken(email,"Invitation");
        var response=new Browser().post(CLIENT+"/register",Map.of("email",email,"password",password,"token",token));
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location").orElse("")).isEqualTo(CLIENT);
        UUID user=user(email); assertThat(status(user)).isEqualTo("PENDING");
        Browser client=webLogin(email); JsonNode draft=json(client.get(CLIENT),200);
        assertThat(draft.get("state").asText()).isEqualTo("DRAFT");
        assertThat(client.command(CLIENT+"/submit",Map.of(),0,key()).statusCode()).isEqualTo(403);
        assertThat(client.post("/api/web/v1/auth/email-verification/confirm",Map.of("token",queuedToken(email,"Vérifiez"))).statusCode()).isEqualTo(204);
        assertThat(client.command(CLIENT+"/submit",Map.of(),0,key()).statusCode()).isEqualTo(409);
        json(client.command(CLIENT+"/profile",clientProfile(),0,null),200);
        assertThat(client.command(CLIENT+"/profile",clientProfile(),0,null).statusCode()).isEqualTo(412);
        String key=key(); var submitted=client.command(CLIENT+"/submit",Map.of(),1,key);
        json(submitted,200); assertThat(client.command(CLIENT+"/submit",Map.of(),1,key).body()).isEqualTo(submitted.body());
        assertThat(client.command(CLIENT+"/submit",Map.of(),2,key).statusCode()).isEqualTo(409);
        assertThat(status(user)).isEqualTo("PENDING");
        assertThat(client.post("/api/web/v1/client/tickets",Map.of()).statusCode()).isEqualTo(403);
        String dossier=draft.get("dossierId").asText();
        json(admin.command(CLIENT_ADMIN+"/"+dossier+"/approve",Map.of("reason","Formulaire relu"),2,null),200);
        assertThat(status(user)).isEqualTo("ACTIVE");
        assertThat(json(client.get(CLIENT),200).get("state").asText()).isEqualTo("APPROVED");
        assertThat(client.command(CLIENT+"/profile",clientProfile(),3,null).statusCode()).isEqualTo(409);
        JsonNode history=json(admin.get(CLIENT_ADMIN+"/"+dossier+"/history"),200);
        assertThat(history.get(history.size()-1).get("actorUserId").asText()).isEqualTo(adminUser.toString());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_onboarding_history WHERE dossier_id=?",Integer.class,UUID.fromString(dossier))).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_invitation_link WHERE invitation_id=? AND consumed_at IS NOT NULL",Integer.class,UUID.fromString(invitation.get("invitationId").asText()))).isEqualTo(1);
    }

    @Test void anExistingIdentityMustAuthenticateAndMatchTheVerifiedRecipientWithoutRoleConversion() throws Exception {
        UUID existing=provisioning.createPendingClient(email,password); verifyInFixture(existing);
        invite(email.toUpperCase(java.util.Locale.ROOT)); String raw=queuedToken(email,"Invitation");
        String hash=jdbc.queryForObject("SELECT password_hash FROM app.local_credential WHERE user_id=?",String.class,existing);
        assertThat(new Browser().post(CLIENT+"/register",Map.of("email",email,"password",password,"token",raw)).statusCode()).isEqualTo(409);
        String wrong="other-"+UUID.randomUUID()+"@example.invalid"; UUID other=provisioning.createPendingClient(wrong,password); verifyInFixture(other);
        Browser wrongClient=webLogin(wrong);
        assertThat(wrongClient.post(CLIENT+"/accept-invitation",Map.of("token",raw)).statusCode()).isEqualTo(400);
        Browser owner=webLogin(email); JsonNode accepted=json(owner.post(CLIENT+"/accept-invitation",Map.of("token",raw)),200);
        assertThat(owner.post(CLIENT+"/accept-invitation",Map.of("token",raw)).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT user_id FROM app.client_onboarding WHERE dossier_id=?",UUID.class,UUID.fromString(accepted.get("dossierId").asText()))).isEqualTo(existing);
        assertThat(jdbc.queryForObject("SELECT password_hash FROM app.local_credential WHERE user_id=?",String.class,existing).equals(hash)).isTrue();
        assertThat(jdbc.queryForList("SELECT role FROM app.user_role WHERE user_id=?",String.class,existing)).containsExactly("CLIENT");
        assertThat(wrongClient.get(CLIENT_ADMIN+"/"+accepted.get("dossierId").asText()).statusCode()).isEqualTo(403);
        assertThat(wrongClient.get(CLIENT).body()).doesNotContain(email,accepted.get("dossierId").asText());
    }

    @Test void resendInvalidatesPreviousLinkAndGetNeverConsumesAnInvitation() throws Exception {
        JsonNode invite=invite(email); String first=queuedToken(email,"Invitation");
        assertThat(new Browser().get(CLIENT+"/register?token="+first).statusCode()).isEqualTo(401);
        clock.advance(1);
        JsonNode resent=json(admin.command(INVITATIONS+"/"+invite.get("invitationId").asText()+"/resend",Map.of(),0,null),200);
        assertThat(resent.get("version").asLong()).isEqualTo(1);
        String next=queuedToken(email,"Invitation"); assertThat(first.equals(next)).isFalse();
        assertThat(new Browser().post(CLIENT+"/register",Map.of("email",email,"password",password,"token",first)).statusCode()).isEqualTo(400);
        assertThat(new Browser().post(CLIENT+"/register",Map.of("email",email,"password",password,"token",next)).statusCode()).isEqualTo(201);
        assertThat(admin.command(INVITATIONS+"/"+invite.get("invitationId").asText()+"/resend",Map.of(),2,null).statusCode()).isEqualTo(409);
    }

    @Test void exactInvitationExpiryAndWrongRecipientDoNotCreateOrConsumeAccounts() throws Exception {
        invite(email); String raw=queuedToken(email,"Invitation");
        assertThat(new Browser().post(CLIENT+"/register",Map.of("email","wrong-"+email,"password",password,"token",raw)).statusCode()).isEqualTo(400);
        clock.advance(48*3600);
        assertThat(new Browser().post(CLIENT+"/register",Map.of("email",email,"password",password,"token",raw)).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.local_credential WHERE email_normalized=?",Integer.class,email)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_invitation i JOIN app.client_invitation_link l USING(invitation_id) WHERE email_normalized=? AND consumed_at IS NOT NULL",Integer.class,email)).isZero();
    }

    @Test void concurrentNewAccountAcceptanceHasExactlyOneDossierAndOneVerificationEmail() throws Exception {
        invite(email); String raw=queuedToken(email,"Invitation");
        Browser first=new Browser(); Browser second=new Browser(); String c1=first.csrf(); String c2=second.csrf();
        Map<String,String> command=Map.of("email",email,"password",password,"token",raw);
        List<Integer> results=concurrent(() -> first.post(CLIENT+"/register",command,c1,null,null).statusCode(),
                () -> second.post(CLIENT+"/register",command,c2,null,null).statusCode());
        assertThat(results).containsExactlyInAnyOrder(201,400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.local_credential WHERE email_normalized=?",Integer.class,email)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_onboarding WHERE user_id=?",Integer.class,user(email))).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox WHERE recipient=? AND subject LIKE 'Vérifiez%'",Integer.class,email)).isEqualTo(1);
    }

    @Test void requestInformationPreservesSnapshotsAndRequiresExplicitResubmission() throws Exception {
        Browser client=registeredClient(); String dossier=json(client.get(CLIENT),200).get("dossierId").asText();
        json(client.command(CLIENT+"/profile",clientProfile(),0,null),200); json(client.command(CLIENT+"/submit",Map.of(),1,key()),200);
        json(admin.command(CLIENT_ADMIN+"/"+dossier+"/request-information",Map.of("reason","Préciser la raison sociale"),2,null),200);
        assertThat(admin.command(CLIENT_ADMIN+"/"+dossier+"/approve",Map.of("reason","Lecture"),3,null).statusCode()).isEqualTo(409);
        var changed=new java.util.HashMap<String,Object>(clientProfile()); changed.put("organizationName","Organisation corrigée");
        json(client.command(CLIENT+"/profile",changed,3,null),200); json(client.command(CLIENT+"/submit",Map.of(),4,key()),200);
        json(admin.command(CLIENT_ADMIN+"/"+dossier+"/approve",Map.of("reason","Complément accepté"),5,null),200);
        assertThat(jdbc.queryForObject("SELECT organization_name FROM app.client_onboarding_history WHERE dossier_id=? AND revision=2",String.class,UUID.fromString(dossier))).isEqualTo("Organisation de test");
        assertThat(json(client.get(CLIENT+"/history?after=2&size=1"),200).get(0).get("reason").asText()).isEqualTo("Préciser la raison sociale");
    }

    @Test void parallelSavesAndDecisionsDoNotSilentlyOverwriteARevision() throws Exception {
        Browser client=registeredClient(); String dossier=json(client.get(CLIENT),200).get("dossierId").asText();
        Browser second=webLogin(email); String c1=client.csrf(); String c2=second.csrf();
        assertThat(concurrent(() -> client.post(CLIENT+"/profile",clientProfile(),c1,0L,null).statusCode(),
                () -> second.post(CLIENT+"/profile",clientProfile(),c2,0L,null).statusCode())).containsExactlyInAnyOrder(200,412);
        json(client.command(CLIENT+"/submit",Map.of(),1,key()),200);
        String ca=admin.csrf();
        assertThat(concurrent(() -> admin.post(CLIENT_ADMIN+"/"+dossier+"/approve",Map.of("reason","Relu"),ca,2L,null).statusCode(),
                () -> admin.post(CLIENT_ADMIN+"/"+dossier+"/reject",Map.of("reason","Refus motivé"),ca,2L,null).statusCode())).containsExactlyInAnyOrder(200,412);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_onboarding_history WHERE dossier_id=? AND action IN ('APPROVED','REJECTED')",Integer.class,UUID.fromString(dossier))).isEqualTo(1);
    }

    @Test void allAdministrativeOperationsNeedMfaAndFreshProofAndCannotReactivateSuspension() throws Exception {
        Browser pendingAdmin=webLogin(adminEmail);
        assertThat(pendingAdmin.post(INVITATIONS,Map.of("email",email)).statusCode()).isEqualTo(403);
        Browser client=registeredClient(); String dossier=json(client.get(CLIENT),200).get("dossierId").asText();
        json(client.command(CLIENT+"/profile",clientProfile(),0,null),200); json(client.command(CLIENT+"/submit",Map.of(),1,key()),200);
        assertThat(client.command(CLIENT_ADMIN+"/"+dossier+"/approve",Map.of("reason","Auto-approbation"),2,null).statusCode()).isEqualTo(403);
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?",user(email));
        assertThat(admin.command(CLIENT_ADMIN+"/"+dossier+"/approve",Map.of("reason","Relu"),2,null).statusCode()).isEqualTo(409);
        assertThat(status(user(email))).isEqualTo("SUSPENDED");
        clock.advance(301);
        assertThat(admin.post(INVITATIONS,Map.of("email","next-"+email)).statusCode()).isEqualTo(403);
        assertThat(admin.get(CLIENT_ADMIN+"?size=101").statusCode()).isEqualTo(400);
    }

    @Test void technicianRegistrationDoesNotRevealExistingMailboxesOrChangeTheirCredentials() throws Exception {
        var first=mobile(TECH+"/register",Map.of("email",email,"password",password),null,null,null);
        assertThat(first.statusCode()).isEqualTo(202); assertThat(first.headers().allValues("Set-Cookie")).isEmpty();
        UUID user=user(email); String hash=jdbc.queryForObject("SELECT password_hash FROM app.local_credential WHERE user_id=?",String.class,user);
        var second=mobile(TECH+"/register",Map.of("email",email,"password",OpaqueTokens.generate()),null,null,null);
        assertThat(second.statusCode()).isEqualTo(202); assertThat(second.body()).isEqualTo(first.body());
        assertThat(jdbc.queryForObject("SELECT password_hash FROM app.local_credential WHERE user_id=?",String.class,user).equals(hash)).isTrue();
        assertThat(status(user)).isEqualTo("PENDING");
        assertThat(jdbc.queryForList("SELECT role FROM app.user_role WHERE user_id=?",String.class,user)).containsExactly("TECHNICIAN");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.technician_onboarding WHERE user_id=?",Integer.class,user)).isEqualTo(1);
    }

    @Test void technicianEvidenceIsMandatoryAndCannotBeClaimedByADtoOrAnAdministrator() throws Exception {
        assertThat(mobile(TECH+"/register",Map.of("email",email,"password",password,"role","ADMIN"),null,null,null).statusCode()).isEqualTo(400);
        mobile(TECH+"/register",Map.of("email",email,"password",password),null,null,null);
        String access=mobileLogin(email); JsonNode draft=json(mobileGet(TECH,access),200); String dossier=draft.get("dossierId").asText();
        assertThat(mobile(TECH+"/submit",Map.of(),access,0L,key()).statusCode()).isEqualTo(403);
        json(new Browser().post("/api/web/v1/auth/email-verification/confirm",Map.of("token",queuedToken(email,"Vérifiez"))),204);
        assertThat(mobile(TECH+"/submit",Map.of(),access,0L,key()).statusCode()).isEqualTo(409);
        json(mobile(TECH+"/profile",technicianProfile(),access,0L,null),200);
        json(mobile(TECH+"/submit",Map.of(),access,1L,key()),200);
        int messages=jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox WHERE recipient=?",Integer.class,email);
        var refused=admin.command(TECH_ADMIN+"/"+dossier+"/approve",Map.of("reason","Pièces non disponibles"),2,null);
        assertThat(refused.statusCode()).isEqualTo(409);
        assertThat(mapper.readTree(refused.body()).get("code").asText()).isEqualTo("TECHNICIAN_DOCUMENTS_REQUIRED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox WHERE recipient=?",Integer.class,email)).isEqualTo(messages);
        assertThat(status(user(email))).isEqualTo("PENDING");
        assertThat(mobile(TECH+"/profile",Map.of("documentsStatus","CLEAN","approved",true),access,2L,null).statusCode()).isEqualTo(400);
        json(admin.command(TECH_ADMIN+"/"+dossier+"/request-information",Map.of("reason","Préparer les justificatifs demandés"),2,null),200);
        assertThat(mobileGet("/api/mobile/v1/technician/missions",access).statusCode()).isEqualTo(403);
    }

    @Test void technicianSubmissionsAreIdempotentAcrossChannelsAndRevocationIsRechecked() throws Exception {
        mobile(TECH+"/register",Map.of("email",email,"password",password),null,null,null); verifyInFixture(user(email));
        String access=mobileLogin(email); Browser web=webLogin(email);
        json(mobile(TECH+"/profile",technicianProfile(),access,0L,null),200);
        String key=key(); String webPath="/api/web/v1/onboarding/technician";
        var result=mobile(TECH+"/submit",Map.of(),access,1L,key); json(result,200);
        assertThat(web.command(webPath+"/submit",Map.of(),1,key).body()).isEqualTo(result.body());
        String dossier=json(mobileGet(TECH,access),200).get("dossierId").asText();
        json(admin.command(TECH_ADMIN+"/"+dossier+"/reject",Map.of("reason","Dossier refusé après examen"),2,null),200);
        assertThat(mobile(TECH+"/submit",Map.of(),access,1L,key).statusCode()).isEqualTo(401);
        assertThat(status(user(email))).isEqualTo("REJECTED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.auth_session WHERE user_id=? AND revoked_at IS NULL",Integer.class,user(email))).isZero();
    }

    @Test void cookiesNeverAuthenticateMobileAndTwoApplicantsCannotReadPrivateDossiers() throws Exception {
        Browser client=registeredClient(); UUID clientId=user(email);
        String technician="tech-"+UUID.randomUUID()+"@example.invalid"; UUID techUser=provisioning.createPendingTechnician(technician,password); verifyInFixture(techUser);
        Browser tech=webLogin(technician); json(tech.post("/api/web/v1/onboarding/technician/start",Map.of()),200);
        assertThat(tech.get(CLIENT_ADMIN+"?page=0&size=1").statusCode()).isEqualTo(403);
        assertThat(tech.get(CLIENT).statusCode()).isEqualTo(403);
        assertThat(client.get(TECH).statusCode()).isEqualTo(401);
        assertThat(client.post("/api/web/v1/onboarding/technician/start",Map.of()).statusCode()).isEqualTo(403);
        assertThat(tech.get(TECH).statusCode()).isEqualTo(401); // Real web cookie sent, mobile still anonymous.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.technician_onboarding WHERE user_id=?",Integer.class,clientId)).isZero();
        assertThat(admin.get(CLIENT_ADMIN+"?size=1").body()).doesNotContain("password", "fingerprint", "address");
    }

    @Test void registrationAndResendLimitsAreSharedAndCannotBeBypassedByForwardedHeaders() throws Exception {
        Map<String,Object> body=Map.of("email",email,"password",password);
        for(int i=0;i<5;i++) { assertThat(mobile(TECH+"/register",body,null,null,null).statusCode()).isEqualTo(202); }
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri(TECH+"/register")).header("Content-Type","application/json").header("X-Forwarded-For","192.0.2.55")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(429); assertThat(response.headers().firstValue("Retry-After")).isPresent();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.local_credential WHERE email_normalized=?",Integer.class,email)).isEqualTo(1);
    }

    @Test void invitationSecretsStayEncryptedAndMailDeliveryUsesOnlyIsolatedMailpit(CapturedOutput logs) throws Exception {
        var invitation=invite(email); String token=queuedToken(email,"Invitation");
        assertThat(invitation.toString().contains(token)).isFalse();
        for(String encrypted:jdbc.queryForList("SELECT encrypted_body FROM app.mail_outbox WHERE recipient=?",String.class,email)) { assertThat(encrypted.contains(token)).isFalse(); }
        assertThat(admin.post(INVITATIONS,Map.of("email",email,"approved",true)).statusCode()).isEqualTo(400);
        // Remove bootstrap notices only from this disposable fixture, leaving the invitation to deliver.
        try(var c=PostgisFixture.migrator(DATABASE);var statement=c.prepareStatement("DELETE FROM app.mail_outbox WHERE recipient<>?")) { statement.setString(1,email); statement.executeUpdate(); }
        assertThat(mail.deliverOne()).isTrue();
        assertThat(jdbc.queryForObject("SELECT state='DELIVERED' AND encrypted_body IS NULL FROM app.mail_outbox WHERE recipient=?",Boolean.class,email)).isTrue();
        assertThat(logs.toString().contains(token) || logs.toString().contains(password)).isFalse();
    }

    @Test void historyAndReceiptsAreAppendOnlyAndDossierUniquenessIsEnforcedByPostgres() throws Exception {
        Browser client=registeredClient();
        for(String sql:List.of("UPDATE app.client_onboarding_history SET reason='overwrite'","DELETE FROM app.client_onboarding_history",
                "TRUNCATE app.technician_onboarding_history","UPDATE app.command_receipt SET result_state='APPROVED'")) {
            try(var c=PostgisFixture.runtime(DATABASE);var statement=c.createStatement()) {
                assertThatThrownBy(() -> statement.execute(sql)).isInstanceOf(SQLException.class).satisfies(f -> assertThat(((SQLException)f).getSQLState()).isEqualTo("42501"));
            }
        }
        assertThatThrownBy(() -> jdbc.update("INSERT INTO app.client_onboarding(dossier_id,user_id,invitation_id,created_at,updated_at) SELECT ?,user_id,invitation_id,now(),now() FROM app.client_onboarding WHERE user_id=?",UUID.randomUUID(),user(email)))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThat(client.post(CLIENT+"/profile",Map.of("role","ADMIN")).statusCode()).isEqualTo(400);
        assertThat(status(user(email))).isEqualTo("PENDING");
    }

    @Test void everyAdmissionAdminRouteRefusesClientPreMfaAndMissingCsrfWithoutPreparingMail() throws Exception {
        Browser client=registeredClient(); String id=json(client.get(CLIENT),200).get("dossierId").asText();
        Browser preMfa=webLogin(adminEmail);
        for(String path:List.of(CLIENT_ADMIN,TECH_ADMIN,CLIENT_ADMIN+"/"+id,TECH_ADMIN+"/"+id,CLIENT_ADMIN+"/"+id+"/history",TECH_ADMIN+"/"+id+"/history")) {
            assertThat(client.get(path).statusCode()).isEqualTo(403); assertThat(preMfa.get(path).statusCode()).isEqualTo(403);
        }
        int before=jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox",Integer.class);
        for(String path:List.of(INVITATIONS,INVITATIONS+"/"+id+"/resend",CLIENT_ADMIN+"/"+id+"/approve",CLIENT_ADMIN+"/"+id+"/request-information",CLIENT_ADMIN+"/"+id+"/reject",
                TECH_ADMIN+"/"+id+"/approve",TECH_ADMIN+"/"+id+"/request-information",TECH_ADMIN+"/"+id+"/reject")) {
            assertThat(client.command(path,Map.of("reason","Aucune autorisation"),0,null).statusCode()).isEqualTo(403);
            assertThat(preMfa.command(path,Map.of("reason","Facteur manquant"),0,null).statusCode()).isEqualTo(403);
            assertThat(admin.post(path,Map.of("reason","CSRF absent"),null,0L,null).statusCode()).isEqualTo(403);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox",Integer.class)).isEqualTo(before);
    }

    @Test void concurrentIdempotentSubmissionCreatesOneSnapshotAndOneReceipt() throws Exception {
        mobile(TECH+"/register",Map.of("email",email,"password",password),null,null,null); verifyInFixture(user(email));
        String access=mobileLogin(email); json(mobile(TECH+"/profile",technicianProfile(),access,0L,null),200);
        assertThat(mobile(TECH+"/submit",Map.of(),access,1L,"short").statusCode()).isEqualTo(400);
        String key=key();
        assertThat(concurrent(() -> mobile(TECH+"/submit",Map.of(),access,1L,key).statusCode(),
                () -> mobile(TECH+"/submit",Map.of(),access,1L,key).statusCode())).containsExactly(200,200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.command_receipt WHERE actor_user_id=?",Integer.class,user(email))).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.technician_onboarding_history h JOIN app.technician_onboarding d USING(dossier_id) WHERE d.user_id=? AND action='SUBMITTED'",Integer.class,user(email))).isEqualTo(1);
    }

    @Test void twoTechniciansHaveSeparateDossiersAndNoCrossChannelOrOwnerOverride() throws Exception {
        mobile(TECH+"/register",Map.of("email",email,"password",password),null,null,null); String one=mobileLogin(email);
        json(mobile(TECH+"/profile",technicianProfile(),one,0L,null),200);
        String other="second-"+UUID.randomUUID()+"@example.invalid";
        mobile(TECH+"/register",Map.of("email",other,"password",password),null,null,null); String two=mobileLogin(other);
        JsonNode first=json(mobileGet(TECH,one),200); JsonNode second=json(mobileGet(TECH,two),200);
        assertThat(first.get("dossierId").asText()).isNotEqualTo(second.get("dossierId").asText());
        assertThat(second.get("profile").get("firstName").isNull()).isTrue();
        assertThat(mobile(TECH+"/profile",Map.of("userId",user(email).toString(),"firstName","Usurpation"),two,0L,null).statusCode()).isEqualTo(400);
        assertThat(mobileGet(TECH+"/"+first.get("dossierId").asText(),two).statusCode()).isEqualTo(403);
        assertThat(json(mobileGet(TECH,one),200).get("profile").get("firstName").asText()).isEqualTo("Alex");
    }

    @Test void outboxFailureRollsBackInvitationOrNewAccountAndNeverCommitsAPartialAdmission() throws Exception {
        try(var c=PostgisFixture.migrator(DATABASE);var statement=c.createStatement()) {
            statement.execute("REVOKE INSERT ON app.mail_outbox FROM maint_app");
            try {
                assertThat(admin.post(INVITATIONS,Map.of("email",email)).statusCode()).isEqualTo(500);
                assertThat(mobile(TECH+"/register",Map.of("email",email,"password",password),null,null,null).statusCode()).isEqualTo(500);
            } finally { statement.execute("GRANT INSERT ON app.mail_outbox TO maint_app"); }
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_invitation WHERE email_normalized=?",Integer.class,email)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.local_credential WHERE email_normalized=?",Integer.class,email)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox WHERE recipient=?",Integer.class,email)).isZero();
    }

    @Test void databaseRejectsRecipientSubstitutionAndTechnicianApprovalWithoutEvidence() throws Exception {
        registeredClient(); UUID correct=user(email); UUID wrong=provisioning.createPendingClient("wrong-db-"+UUID.randomUUID()+"@example.invalid",password);
        assertSqlState("UPDATE app.client_onboarding SET user_id='"+wrong+"' WHERE user_id='"+correct+"'","23503");
        mobile(TECH+"/register",Map.of("email","tech-db-"+email,"password",password),null,null,null);
        UUID technician=user("tech-db-"+email);
        assertSqlState("UPDATE app.technician_onboarding SET state='APPROVED' WHERE user_id='"+technician+"'","23514");
    }

    @Test void partialGoogleRegistrationContextCannotPassSqlCheckThroughNullSemantics() throws Exception {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO app.google_oauth_transaction(transaction_id,channel,intention,launch_fingerprint,registration_email,created_at,expires_at) "
                + "VALUES (?,'WEB','LOGIN',?,?,now(),now()+interval '5 minutes')",UUID.randomUUID(),OpaqueTokens.fingerprint(OpaqueTokens.generate()),email))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void clientApprovalCannotActivateAnAddedTechnicianRoleOrReplayItsOldSubmission() throws Exception {
        Browser client=registeredClient(); UUID user=user(email);
        String dossier=json(client.get(CLIENT),200).get("dossierId").asText();
        json(client.command(CLIENT+"/profile",clientProfile(),0,null),200);
        String key=key(); json(client.command(CLIENT+"/submit",Map.of(),1,key),200);
        int messages=jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox WHERE recipient=?",Integer.class,email);
        jdbc.update("INSERT INTO app.user_role(user_id,role) VALUES (?,'TECHNICIAN')",user);

        assertThat(client.get(CLIENT).statusCode()).isEqualTo(403);
        assertThat(client.command(CLIENT+"/submit",Map.of(),1,key).statusCode()).isEqualTo(403);
        assertThat(client.post("/api/web/v1/onboarding/technician/start",Map.of()).statusCode()).isEqualTo(403);
        for(String decision:List.of("approve","reject","request-information")) {
            assertThat(admin.command(CLIENT_ADMIN+"/"+dossier+"/"+decision,Map.of("reason","Ne pas activer un autre rôle"),2,null).statusCode()).isEqualTo(409);
        }
        assertThat(status(user)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT state FROM app.client_onboarding WHERE user_id=?",String.class,user)).isEqualTo("SUBMITTED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_onboarding_history WHERE dossier_id=?",Integer.class,UUID.fromString(dossier))).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.technician_onboarding WHERE user_id=?",Integer.class,user)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mail_outbox WHERE recipient=?",Integer.class,email)).isEqualTo(messages);
    }

    @Test void aMixedTechnicianCannotAcceptAClientInvitationOrReplayAnEarlierMobileSubmission() throws Exception {
        json(mobile(TECH+"/register",Map.of("email",email,"password",password),null,null,null),202);
        UUID user=user(email); verifyInFixture(user); String access=mobileLogin(email);
        String dossier=json(mobileGet(TECH,access),200).get("dossierId").asText();
        json(mobile(TECH+"/profile",technicianProfile(),access,0L,null),200);
        String key=key(); json(mobile(TECH+"/submit",Map.of(),access,1L,key),200);
        Browser web=webLogin(email);
        jdbc.update("INSERT INTO app.user_role(user_id,role) VALUES (?,'CLIENT')",user);
        invite(email); String raw=queuedToken(email,"Invitation");

        assertThat(web.post(CLIENT+"/accept-invitation",Map.of("token",raw)).statusCode()).isEqualTo(403);
        assertThat(mobileGet(TECH,access).statusCode()).isEqualTo(403);
        assertThat(mobile(TECH+"/submit",Map.of(),access,1L,key).statusCode()).isEqualTo(403);
        assertThat(admin.command(TECH_ADMIN+"/"+dossier+"/request-information",Map.of("reason","Rôles incompatibles avec cette admission"),2,null).statusCode()).isEqualTo(409);
        assertThat(status(user)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_onboarding WHERE user_id=?",Integer.class,user)).isZero();
        assertThat(jdbc.queryForObject("SELECT accepted_at IS NULL FROM app.client_invitation WHERE email_normalized=?",Boolean.class,email)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.technician_onboarding_history WHERE dossier_id=?",Integer.class,UUID.fromString(dossier))).isEqualTo(3);
    }

    private void assertSqlState(String sql,String expected) throws Exception {
        try(var c=PostgisFixture.runtime(DATABASE);var statement=c.createStatement()) {
            assertThatThrownBy(() -> statement.execute(sql)).isInstanceOf(SQLException.class).satisfies(f -> assertThat(((SQLException)f).getSQLState()).isEqualTo(expected));
        }
    }

    private JsonNode invite(String recipient) throws Exception { return json(admin.post(INVITATIONS,Map.of("email",recipient)),201); }
    private Browser registeredClient() throws Exception {
        invite(email); String token=queuedToken(email,"Invitation");
        assertThat(new Browser().post(CLIENT+"/register",Map.of("email",email,"password",password,"token",token)).statusCode()).isEqualTo(201);
        new Browser().post("/api/web/v1/auth/email-verification/confirm",Map.of("token",queuedToken(email,"Vérifiez")));
        return webLogin(email);
    }
    private Browser webLogin(String mailbox) throws Exception {
        Browser browser=new Browser(); assertThat(browser.post("/api/web/v1/auth/login",Map.of("email",mailbox,"password",password)).statusCode()).isEqualTo(200); return browser;
    }
    private String mobileLogin(String mailbox) throws Exception { return json(mobile("/api/mobile/v1/auth/login",Map.of("email",mailbox,"password",password),null,null,null),200).get("accessToken").asText(); }
    private UUID user(String mailbox) { return jdbc.queryForObject("SELECT user_id FROM app.local_credential WHERE email_normalized=?",UUID.class,mailbox.toLowerCase(java.util.Locale.ROOT)); }
    private String status(UUID user) { return jdbc.queryForObject("SELECT status FROM app.app_user WHERE user_id=?",String.class,user); }
    private void verifyInFixture(UUID user) { jdbc.update("UPDATE app.local_credential SET email_verified_at=? WHERE user_id=?",java.sql.Timestamp.from(clock.instant()),user); }
    private String queuedToken(String recipient,String prefix) {
        String body=jdbc.query("SELECT * FROM app.mail_outbox WHERE recipient=? AND subject LIKE ? ORDER BY created_at DESC,message_id LIMIT 1",(row,index) -> {
            String context="mail-outbox/"+row.getObject("message_id")+"/"+recipient.length()+":"+recipient+"/"+row.getString("subject");
            return cipher.decrypt(row.getString("encrypted_body"),context);
        },recipient,prefix+"%").getFirst();
        var match=java.util.regex.Pattern.compile("#token=([A-Za-z0-9_-]{43})").matcher(body);
        if(!match.find()) { throw new AssertionError("No test-only action link found."); } return match.group(1);
    }
    private Map<String,Object> clientProfile() { return Map.of("accountType","BUSINESS","firstName","Camille","lastName","Exemple","phone","+33 100000000","address","Adresse synthétique","organizationName","Organisation de test"); }
    private Map<String,Object> technicianProfile() { return Map.of("firstName","Alex","lastName","Exemple","phone","+33 100000001","baseCity","Paris","radiusKm",50,"declaredSkills",List.of("Réseaux","Postes de travail")); }
    private JsonNode json(HttpResponse<String> response,int status) throws Exception { assertThat(response.statusCode()).isEqualTo(status); return mapper.readTree(response.body().isEmpty() ? "null" : response.body()); }
    private static String key() { return UUID.randomUUID().toString(); }
    private URI uri(String path) { return URI.create("http://127.0.0.1:"+port+path); }
    private HttpResponse<String> mobileGet(String path,String access) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri(path)).header("Authorization","Bearer "+access).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> mobile(String path,Object body,String access,Long version,String key) throws Exception {
        var request=HttpRequest.newBuilder(uri(path)).header("Content-Type","application/json");
        if(access!=null) { request.header("Authorization","Bearer "+access); }
        if(version!=null) { request.header("If-Match","\""+version+"\""); }
        if(key!=null) { request.header("Idempotency-Key",key); }
        return HttpClient.newHttpClient().send(request.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    private List<Integer> concurrent(Callable<Integer> first,Callable<Integer> second) throws Exception {
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var one=executor.submit(() -> { start.await(); return first.call(); }); var two=executor.submit(() -> { start.await(); return second.call(); }); start.countDown();
            return List.of(one.get(),two.get());
        }
    }
    private final class Browser {
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),HttpResponse.BodyHandlers.ofString()); }
        String csrf() throws Exception { return mapper.readTree(get("/api/web/v1/auth/csrf").body()).get("token").asText(); }
        HttpResponse<String> post(String path,Object body) throws Exception { return post(path,body,csrf(),null,null); }
        HttpResponse<String> command(String path,Object body,long version,String key) throws Exception { return post(path,body,csrf(),version,key); }
        HttpResponse<String> post(String path,Object body,String csrf,Long version,String key) throws Exception {
            var request=HttpRequest.newBuilder(uri(path)).header("Content-Type","application/json");
            if(csrf!=null) { request.header("X-CSRF-TOKEN",csrf); }
            if(version!=null) { request.header("If-Match","\""+version+"\""); }
            if(key!=null) { request.header("Idempotency-Key",key); }
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
    }
}
