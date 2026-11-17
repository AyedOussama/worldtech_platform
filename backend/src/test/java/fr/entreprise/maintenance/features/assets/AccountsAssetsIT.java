package fr.entreprise.maintenance.features.assets;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.sql.Timestamp;
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
@Import(AccountsAssetsIT.TimeConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class AccountsAssetsIT {
    @TestConfiguration(proxyBeanMethods=false) static class TimeConfiguration { @Bean @Primary MutableClock testClock() { return new MutableClock(); } }
    @Container static final GenericContainer<?> DATABASE=PostgisFixture.container();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) { PostgisFixture.properties(r,DATABASE); }
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;
    @Autowired AdministratorBootstrapService bootstrap;
    @Autowired AccountProvisioning provision;
    @LocalServerPort int port;
    private String password;
    private String adminEmail;
    private UUID adminId;
    private Browser admin;
    private Client a;
    private Client b;
    private UUID accountA;
    private UUID accountB;
    private static final String ADMIN="/api/web/v1/admin/client-accounts";
    private static final String CLIENT="/api/web/v1/client/accounts";
    private record Client(UUID user,UUID dossier,Browser web) { }

    @BeforeEach void setup() throws Exception {
        clock.reset(); jdbc.update("DELETE FROM app.auth_rate_bucket"); password=OpaqueTokens.generate()+" passphrase";
        adminEmail="assets-admin-"+UUID.randomUUID()+"@example.invalid";
        adminId=bootstrap.create(adminEmail,password); verify(adminId); admin=login(adminEmail);
        var enrollment=body(admin.post("/api/web/v1/auth/mfa/enrollment/start",Map.of()),200);
        String otp=new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30),6,"HmacSHA1").generateOneTimePasswordString(new SecretKeySpec(Base32.decode(enrollment.get("secret").asText()),"HmacSHA1"),clock.instant(),java.util.Locale.ROOT);
        body(admin.post("/api/web/v1/auth/mfa/enrollment/confirm",Map.of("enrollmentId",enrollment.get("enrollmentId").asText(),"code",otp)),200);
        a=seed("BUSINESS"); b=seed("BUSINESS"); accountA=create(a); accountB=create(b);
    }
    @Test void approvedDossierCreatesOneAccountAndNoDefaultSiteAccess() throws Exception {
        var again=body(admin.post(ADMIN,Map.of("dossierId",a.dossier)),200);
        assertThat(again.get("accountId").asText()).isEqualTo(accountA.toString());
        UUID site=site(accountA,"Privé"); equipment(accountA,site,"Équipement privé","PC");
        assertThat(body(a.web.get(CLIENT),200).get("total").asInt()).isEqualTo(1);
        assertThat(body(a.web.get(cp(accountA)+"/sites"),200).get("total").asInt()).isZero();
        assertThat(body(a.web.get(cp(accountA)+"/summary"),200).get("equipment").asInt()).isZero();
        assertThat(a.web.get(cp(accountA)+"/sites/"+site).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_account WHERE dossier_id=?",Integer.class,a.dossier)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.site_access_policy WHERE account_id=?",Integer.class,accountA)).isZero();
    }
    @Test void twoAccountsAndHiddenSitesNeverLeakRowsTotalsContactsOrFilteredEquipment() throws Exception {
        UUID visible=site(accountA,"Visible"), hidden=site(accountA,"Secret A"), foreign=site(accountB,"Secret B");
        UUID visibleEquipment=equipment(accountA,visible,"Visible PC","PC"), hiddenEquipment=equipment(accountA,hidden,"Secret équipement","SERVER");
        equipment(accountB,foreign,"Autre compte","PC"); grant(accountA,a.user,false,List.of(visible),0);
        JsonNode sites=body(a.web.get(cp(accountA)+"/sites?size=1"),200);
        assertThat(sites.get("total").asInt()).isEqualTo(1); assertThat(sites.get("items").size()).isEqualTo(1);
        JsonNode devices=body(a.web.get(cp(accountA)+"/equipment?size=1"),200);
        assertThat(devices.get("total").asInt()).isEqualTo(1); assertThat(devices.get("items").get(0).get("equipmentId").asText()).isEqualTo(visibleEquipment.toString());
        assertThat(body(a.web.get(cp(accountA)+"/equipment?category=SERVER"),200).get("total").asInt()).isZero();
        for(String path:List.of(cp(accountB),cp(accountB)+"/sites",cp(accountB)+"/summary",cp(accountA)+"/sites/"+hidden,cp(accountA)+"/sites/"+foreign,cp(accountA)+"/equipment/"+hiddenEquipment,cp(accountA)+"/equipment?siteId="+foreign,cp(accountA)+"/equipment?siteId="+hidden)) {
            var refused=a.web.get(path); assertThat(refused.statusCode()).isEqualTo(404);
            assertThat(refused.body()).doesNotContain("Secret","Adresse confidentielle","Contact privé","Consigne confidentielle",foreign.toString(),hidden.toString());
        }
        assertThat(body(a.web.get(cp(accountA)+"/summary"),200).get("sites").asInt()).isEqualTo(1);
        assertThat(b.web.get(cp(accountA)+"/equipment/"+visibleEquipment).statusCode()).isEqualTo(404);
    }
    @Test void memberRemovalAndReadditionCannotRestoreOldAllSitesRights() throws Exception {
        UUID site=site(accountA,"Site A"); body(admin.post(ap(accountA)+"/members",Map.of("userId",b.user)),201);
        grant(accountA,b.user,true,List.of(),0);
        JsonNode members=body(admin.get(ap(accountA)+"/members"),200); UUID old=null;
        for(JsonNode m:members.get("items")) { if(m.get("userId").asText().equals(b.user.toString())) { old=UUID.fromString(m.get("memberId").asText()); } }
        assertThat(old).isNotNull(); assertThat(b.web.get(cp(accountA)+"/sites/"+site).statusCode()).isEqualTo(200);
        body(admin.command("POST",ap(accountA)+"/members/"+old+"/remove",Map.of(),0L),200);
        assertThat(b.web.get(cp(accountA)).statusCode()).isEqualTo(404);
        var readded=body(admin.post(ap(accountA)+"/members",Map.of("userId",b.user)),201);
        assertThat(readded.get("memberId").asText()).isNotEqualTo(old.toString());
        assertThat(body(b.web.get(cp(accountA)+"/sites"),200).get("total").asInt()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.site_access_policy WHERE member_id=? AND all_sites",Integer.class,old)).isEqualTo(1);
    }
    @Test void changedSiteRightsApplyWithTheSameSessionAndAllSitesExplicitlyIncludesFutureSites() throws Exception {
        UUID first=site(accountA,"Premier"); grant(accountA,a.user,true,List.of(),0); UUID next=site(accountA,"Nouveau");
        assertThat(body(a.web.get(cp(accountA)+"/sites"),200).get("total").asInt()).isEqualTo(2);
        grant(accountA,a.user,false,List.of(first),1);
        assertThat(a.web.get(cp(accountA)+"/sites/"+next).statusCode()).isEqualTo(404);
        grant(accountA,a.user,false,List.of(),2);
        assertThat(body(a.web.get(cp(accountA)+"/summary"),200).get("sites").asInt()).isZero();
    }
    @Test void scopeChangesRejectForeignSitesWrongAccountsAndStaleVersionsAtomically() throws Exception {
        UUID local=site(accountA,"A"), foreign=site(accountB,"B"); grant(accountA,a.user,false,List.of(local),0);
        assertThat(admin.command("PUT",access(accountA,a.user),Map.of("allSites",false,"siteIds",List.of(foreign)),1L).statusCode()).isEqualTo(404);
        assertThat(admin.command("PUT",access(accountA,a.user),Map.of("allSites",true,"siteIds",List.of()),0L).statusCode()).isEqualTo(412);
        assertThat(admin.command("PUT",access(accountA,b.user),Map.of("allSites",true,"siteIds",List.of()),0L).statusCode()).isEqualTo(404);
        assertThat(body(a.web.get(cp(accountA)+"/sites"),200).get("total").asInt()).isEqualTo(1);
        assertThat(body(admin.get(access(accountA,a.user)),200).get("version").asInt()).isEqualTo(1);
    }
    @Test void clientIsReadOnlyAndEveryAdministrativeVerbRequiresMfaAndCsrf() throws Exception {
        UUID site=site(accountA,"A"), equipment=equipment(accountA,site,"PC","PC"); Browser preMfa=login(adminEmail);
        for(String path:List.of(ADMIN,ap(accountA),ap(accountA)+"/members",ap(accountA)+"/sites",ap(accountA)+"/equipment",access(accountA,a.user))) {
            assertThat(a.web.get(path).statusCode()).isEqualTo(403); assertThat(preMfa.get(path).statusCode()).isEqualTo(403);
        }
        for(var entry:Map.of("POST",ap(accountA)+"/sites","PUT",ap(accountA)+"/sites/"+site,"PATCH",ap(accountA)+"/equipment/"+equipment).entrySet()) {
            assertThat(a.web.command(entry.getKey(),entry.getValue(),Map.of(),0L).statusCode()).isEqualTo(403);
            assertThat(preMfa.command(entry.getKey(),entry.getValue(),Map.of(),0L).statusCode()).isEqualTo(403);
            assertThat(admin.send(entry.getKey(),entry.getValue(),Map.of(),0L,false).statusCode()).isEqualTo(403);
        }
        assertThat(a.web.command("POST",cp(accountA)+"/sites",siteBody("Interdit"),null).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT name FROM app.site WHERE site_id=?",String.class,site)).isEqualTo("A");
    }
    @Test void versionsProtectPatchReplacementAndConcurrentUpdatesWithoutChangingOwnership() throws Exception {
        UUID site=site(accountA,"Original");
        body(admin.command("PATCH",ap(accountA)+"/sites/"+site,Map.of("name","Modifié"),0L),200);
        assertThat(admin.command("PUT",ap(accountA)+"/sites/"+site,siteBody("Périmé"),0L).statusCode()).isEqualTo(412);
        JsonNode updated=body(admin.get(ap(accountA)+"/sites/"+site),200);
        assertThat(updated.get("profile").get("contacts").size()).isEqualTo(1);
        String csrf=admin.csrf(); var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> command=()->{ start.await(); return admin.sendWithCsrf("PATCH",ap(accountA)+"/sites/"+site,Map.of("name","Concurrent"),1L,csrf).statusCode(); };
            var one=pool.submit(command); var two=pool.submit(command); start.countDown(); assertThat(List.of(one.get(),two.get())).containsExactlyInAnyOrder(200,412);
        }
        assertThat(admin.command("PATCH",ap(accountB)+"/sites/"+site,Map.of("name","Transfert"),2L).statusCode()).isEqualTo(404);
        assertThat(admin.command("PATCH",ap(accountA)+"/sites/"+site,Map.of("accountId",accountB),2L).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT account_id FROM app.site WHERE site_id=?",UUID.class,site)).isEqualTo(accountA);
    }
    @Test void equipmentCannotAttachOrMoveAcrossAccountsAndAttributesAreStrictlyValidated() throws Exception {
        UUID site=site(accountA,"A"), foreign=site(accountB,"B"), equipment=equipment(accountA,site,"PC","PC");
        assertThat(admin.post(ap(accountA)+"/sites/"+foreign+"/equipment",Map.of("label","Bad","category","PC")).statusCode()).isEqualTo(404);
        for(Object attrs:List.of(Map.of("ram",16),Map.of("nested",Map.of("secret","x")),Map.of("bad key","x"))) {
            assertThat(admin.command("PATCH",ap(accountA)+"/equipment/"+equipment,Map.of("attributes",attrs),0L).statusCode()).isEqualTo(400);
        }
        assertThat(admin.command("PATCH",ap(accountA)+"/equipment/"+equipment,Map.of("siteId",foreign),0L).statusCode()).isEqualTo(400);
        body(admin.command("PATCH",ap(accountA)+"/equipment/"+equipment,Map.of("attributes",Map.of("ram","16 GiB")),0L),200);
        assertThat(jdbc.queryForObject("SELECT site_id FROM app.equipment WHERE equipment_id=?",UUID.class,equipment)).isEqualTo(site);
    }
    @Test void individualCannotAcquireAnotherMemberAndUnapprovedSuspendedOrMixedTargetsAreRefused() throws Exception {
        Client individual=seed("INDIVIDUAL"); UUID account=create(individual);
        assertThat(admin.post(ap(account)+"/members",Map.of("userId",b.user)).statusCode()).isEqualTo(409);
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?",b.user);
        assertThat(admin.post(ap(accountA)+"/members",Map.of("userId",b.user)).statusCode()).isEqualTo(409);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?",b.user);
        jdbc.update("INSERT INTO app.user_role(user_id,role) VALUES (?,'TECHNICIAN')",b.user);
        assertThat(admin.post(ap(accountA)+"/members",Map.of("userId",b.user)).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_member WHERE account_id=?",Integer.class,account)).isEqualTo(1);
    }
    @Test void suspensionRoleChangeAndExpiredAdministrativeProofCloseAccessOnTheNextRequest() throws Exception {
        UUID site=site(accountA,"A"); grant(accountA,a.user,true,List.of(),0);
        assertThat(a.web.get(cp(accountA)+"/sites/"+site).statusCode()).isEqualTo(200);
        jdbc.update("INSERT INTO app.user_role(user_id,role) VALUES (?,'TECHNICIAN')",a.user);
        assertThat(a.web.get(cp(accountA)+"/sites/"+site).statusCode()).isEqualTo(403);
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?",a.user);
        assertThat(a.web.get(cp(accountA)).statusCode()).isEqualTo(401);
        clock.advance(301);
        assertThat(admin.get(ap(accountA)).statusCode()).isEqualTo(403);
        assertThat(admin.post(ap(accountA)+"/sites",siteBody("Interdit")).statusCode()).isEqualTo(403);
    }
    @Test void archivesHideTheWholePrivateSiteWithoutDeletingEquipmentOrAuditHistory() throws Exception {
        UUID site=site(accountA,"A"), equipment=equipment(accountA,site,"PC","PC"); grant(accountA,a.user,true,List.of(),0);
        body(admin.command("POST",ap(accountA)+"/sites/"+site+"/archive",Map.of(),0L),200);
        assertThat(a.web.get(cp(accountA)+"/equipment/"+equipment).statusCode()).isEqualTo(404);
        assertThat(body(a.web.get(cp(accountA)+"/summary"),200).get("equipment").asInt()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.equipment WHERE equipment_id=?",Integer.class,equipment)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.asset_change WHERE resource_id=?",Integer.class,site)).isEqualTo(2);
    }
    @Test void paginationInputLimitsAndPrivateResponseCachingAreExplicit() throws Exception {
        for(String query:List.of("size=101","page=-1","page=10001","size=0")) { assertThat(a.web.get(cp(accountA)+"/sites?"+query).statusCode()).isEqualTo(400); }
        assertThat(admin.post(ap(accountA)+"/sites",Map.of("name","X","address","Y","timeZone","+02:00")).statusCode()).isEqualTo(400);
        assertThat(admin.post(ap(accountA)+"/sites",Map.of("accessInstructions","x".repeat(17000))).statusCode()).isEqualTo(413);
        var response=a.web.get(CLIENT); assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(a.web.get("/api/mobile/v1/client/accounts").statusCode()).isEqualTo(401);
    }
    @Test void postgresRejectsIncoherentRelationsInvalidAttributesAndHistoryMutation() throws Exception {
        UUID site=site(accountA,"A"), foreign=site(accountB,"B");
        assertThatThrownBy(()->jdbc.update("INSERT INTO app.equipment(equipment_id,account_id,site_id,label,category,created_at,updated_at) VALUES (?,?,?,'X','Y',now(),now())",UUID.randomUUID(),accountA,foreign)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        UUID equipment=equipment(accountA,site,"PC","PC");
        assertThatThrownBy(()->jdbc.update("UPDATE app.equipment SET attributes='{"+"\"ram\":16}'::jsonb WHERE equipment_id=?",equipment)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        grant(accountA,a.user,false,List.of(site),0); UUID member=jdbc.queryForObject("SELECT member_id FROM app.client_member WHERE account_id=? AND user_id=? AND active",UUID.class,accountA,a.user);
        assertThatThrownBy(()->jdbc.update("INSERT INTO app.site_access_grant(account_id,member_id,site_id) VALUES (?,?,?)",accountA,member,foreign)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        for(String sql:List.of("DELETE FROM app.site","UPDATE app.asset_change SET action='UPDATED'","TRUNCATE app.client_account_change","DELETE FROM app.client_member")) {
            try(var c=PostgisFixture.runtime(DATABASE);var st=c.createStatement()) { assertThatThrownBy(()->st.execute(sql)).isInstanceOf(SQLException.class).satisfies(f->assertThat(((SQLException)f).getSQLState()).isEqualTo("42501")); }
        }
    }
    @Test void auditFailureRollsBackTheEntireSiteMutationAndDoesNotLogPrivateContent(CapturedOutput output) throws Exception {
        int count=jdbc.queryForObject("SELECT count(*) FROM app.site WHERE account_id=?",Integer.class,accountA);
        try(var c=PostgisFixture.migrator(DATABASE);var s=c.createStatement()) {
            s.execute("REVOKE INSERT ON app.asset_change FROM maint_app");
            try { assertThat(admin.post(ap(accountA)+"/sites",siteBody("ConfidentielRollback")).statusCode()).isEqualTo(500); }
            finally { s.execute("GRANT INSERT ON app.asset_change TO maint_app"); }
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.site WHERE account_id=?",Integer.class,accountA)).isEqualTo(count);
        assertThat(output.toString()).doesNotContain("ConfidentielRollback","Consigne confidentielle",password);
    }
    @Test void accountCreationCannotPromoteUnapprovedPendingOrUnverifiedIdentities() throws Exception {
        Client candidate=seed("BUSINESS");
        jdbc.update("UPDATE app.client_onboarding SET state='SUBMITTED' WHERE dossier_id=?",candidate.dossier);
        assertThat(admin.post(ADMIN,Map.of("dossierId",candidate.dossier)).statusCode()).isEqualTo(409);
        jdbc.update("UPDATE app.client_onboarding SET state='APPROVED' WHERE dossier_id=?",candidate.dossier);
        jdbc.update("UPDATE app.app_user SET status='PENDING' WHERE user_id=?",candidate.user);
        assertThat(admin.post(ADMIN,Map.of("dossierId",candidate.dossier)).statusCode()).isEqualTo(409);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?",candidate.user);
        jdbc.update("UPDATE app.local_credential SET email_verified_at=NULL WHERE user_id=?",candidate.user);
        assertThat(admin.post(ADMIN,Map.of("dossierId",candidate.dossier)).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_account WHERE dossier_id=?",Integer.class,candidate.dossier)).isZero();
        assertThat(admin.post(ADMIN,Map.of("dossierId",a.dossier,"accountType","INDIVIDUAL","founderUserId",b.user)).statusCode()).isEqualTo(400);
    }
    @Test void everyNewAdministrativeMutationRefusesTechnicianAndPendingClientWithoutSideEffects() throws Exception {
        String email="active-tech-fixture-"+UUID.randomUUID()+"@example.invalid"; UUID tech=provision.createPendingTechnician(email,password); verify(tech);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?",tech); Browser technician=login(email);
        jdbc.update("UPDATE app.app_user SET status='PENDING' WHERE user_id=?",b.user);
        UUID site=site(accountA,"A"), equipment=equipment(accountA,site,"PC","PC");
        String root=ap(accountA); UUID member=jdbc.queryForObject("SELECT member_id FROM app.client_member WHERE account_id=? AND user_id=? AND active",UUID.class,accountA,a.user);
        List<String> commands=List.of("POST "+ADMIN,"POST "+root+"/members","POST "+root+"/members/"+member+"/remove","POST "+root+"/sites","POST "+root+"/sites/"+site+"/archive","POST "+root+"/sites/"+site+"/equipment","POST "+root+"/equipment/"+equipment+"/archive","PATCH "+root,"PATCH "+root+"/sites/"+site,"PUT "+root+"/sites/"+site,"PATCH "+root+"/equipment/"+equipment,"PUT "+root+"/equipment/"+equipment,"PUT "+access(accountA,a.user));
        int before=jdbc.queryForObject("SELECT count(*) FROM app.asset_change WHERE account_id=?",Integer.class,accountA);
        for(String command:commands) { String[] parts=command.split(" ",2); assertThat(technician.command(parts[0],parts[1],Map.of(),0L).statusCode()).isEqualTo(403); assertThat(b.web.command(parts[0],parts[1],Map.of(),0L).statusCode()).isEqualTo(403); }
        assertThat(technician.get(cp(accountA)+"/sites").statusCode()).isEqualTo(403);
        assertThat(b.web.get(cp(accountB)).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.asset_change WHERE account_id=?",Integer.class,accountA)).isEqualTo(before);
    }
    @Test void postgresEnforcesIndividualOwnershipAndApprovedSourceWithoutRelyingOnHttp() throws Exception {
        Client individual=seed("INDIVIDUAL"); UUID account=create(individual);
        assertThatThrownBy(()->jdbc.update("INSERT INTO app.client_member(member_id,account_id,account_type,founder_user_id,user_id,created_at,updated_at) VALUES (?,?,'INDIVIDUAL',?,?,now(),now())",UUID.randomUUID(),account,individual.user,b.user)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        Client unapproved=seed("BUSINESS"); jdbc.update("UPDATE app.client_onboarding SET state='SUBMITTED' WHERE dossier_id=?",unapproved.dossier);
        assertThatThrownBy(()->jdbc.update("INSERT INTO app.client_account(account_id,dossier_id,founder_user_id,account_type,display_name,created_at,updated_at) VALUES (?,?,?,'BUSINESS','Interdit',now(),now())",UUID.randomUUID(),unapproved.dossier,unapproved.user)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_member WHERE account_id=?",Integer.class,account)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_account WHERE dossier_id=?",Integer.class,unapproved.dossier)).isZero();
    }
    private Client seed(String type) throws Exception {
        String email="asset-client-"+UUID.randomUUID()+"@example.invalid"; UUID user=provision.createPendingClient(email,password); verify(user);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?",user);
        UUID invitation=UUID.randomUUID(),dossier=UUID.randomUUID(); Timestamp now=Timestamp.from(clock.instant());
        jdbc.update("INSERT INTO app.client_invitation(invitation_id,email_normalized,email_display,invited_by,last_sent_by,accepted_user_id,created_at,expires_at,accepted_at) VALUES (?,?,?,?,?,?,?,?,?)",invitation,email,email,adminId,adminId,user,now,Timestamp.from(clock.instant().plusSeconds(172800)),now);
        jdbc.update("INSERT INTO app.client_onboarding(dossier_id,user_id,invitation_id,state,account_type,first_name,last_name,phone,address,organization_name,created_at,updated_at) VALUES (?,?,?,'APPROVED',?,'Client','Synthétique','+33 100000000','Adresse synthétique',?,?,?)",dossier,user,invitation,type,type.equals("BUSINESS")?"Organisation synthétique":null,now,now);
        return new Client(user,dossier,login(email));
    }
    private UUID create(Client c) throws Exception { return UUID.fromString(body(admin.post(ADMIN,Map.of("dossierId",c.dossier)),201).get("accountId").asText()); }
    private UUID site(UUID account,String name) throws Exception { return UUID.fromString(body(admin.post(ap(account)+"/sites",siteBody(name)),201).get("siteId").asText()); }
    private UUID equipment(UUID account,UUID site,String label,String category) throws Exception { return UUID.fromString(body(admin.post(ap(account)+"/sites/"+site+"/equipment",Map.of("label",label,"category",category)),201).get("equipmentId").asText()); }
    private Map<String,Object> siteBody(String name) { return Map.of("name",name,"address","Adresse confidentielle synthétique","timeZone","Europe/Paris","contacts",List.of(Map.of("name","Contact privé","phone","+33 100000000")),"accessInstructions","Consigne confidentielle"); }
    private void grant(UUID account,UUID user,boolean all,List<UUID> sites,long version) throws Exception { body(admin.command("PUT",access(account,user),Map.of("allSites",all,"siteIds",sites),version),200); }
    private String ap(UUID account) { return ADMIN+"/"+account; }
    private String cp(UUID account) { return CLIENT+"/"+account; }
    private String access(UUID account,UUID user) { return ap(account)+"/members/"+user+"/site-access"; }
    private void verify(UUID user) { jdbc.update("UPDATE app.local_credential SET email_verified_at=? WHERE user_id=?",Timestamp.from(clock.instant()),user); }
    private Browser login(String email) throws Exception { Browser web=new Browser(); body(web.post("/api/web/v1/auth/login",Map.of("email",email,"password",password)),200); return web; }
    private JsonNode body(HttpResponse<String> response,int expected) { assertThat(response.statusCode()).isEqualTo(expected); return json.readTree(response.body().isEmpty()?"null":response.body()); }
    private final class Browser {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        HttpResponse<String> get(String path) throws Exception { return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString()); }
        String csrf() throws Exception { return body(get("/api/web/v1/auth/csrf"),200).get("token").asText(); }
        HttpResponse<String> post(String path,Object value) throws Exception { return command("POST",path,value,null); }
        HttpResponse<String> command(String method,String path,Object value,Long version) throws Exception { return send(method,path,value,version,true); }
        HttpResponse<String> send(String method,String path,Object value,Long version,boolean csrf) throws Exception { return sendWithCsrf(method,path,value,version,csrf?csrf():null); }
        HttpResponse<String> sendWithCsrf(String method,String path,Object value,Long version,String csrf) throws Exception {
            var r=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json");
            if(version!=null) { r.header("If-Match","\""+version+"\""); } if(csrf!=null) { r.header("X-CSRF-TOKEN",csrf); }
            return http.send(r.method(method,HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))).build(),HttpResponse.BodyHandlers.ofString());
        }
    }
}
