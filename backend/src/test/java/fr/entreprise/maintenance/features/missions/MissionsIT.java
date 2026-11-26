package fr.entreprise.maintenance.features.missions;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import javax.crypto.spec.SecretKeySpec;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import fr.entreprise.maintenance.features.identity.application.AdministratorBootstrapService;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import fr.entreprise.maintenance.features.identity.publicapi.AccountProvisioning;
import fr.entreprise.maintenance.testinfra.MutableClock;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.bouncycastle.util.encoders.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
@Import(MissionsIT.TimeConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class MissionsIT {
    @TestConfiguration(proxyBeanMethods=false) static class TimeConfiguration { @Bean @Primary MutableClock testClock() { return new MutableClock(); } }
    @Container static final GenericContainer<?> DATABASE=PostgisFixture.container();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) { PostgisFixture.properties(r,DATABASE); }
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;
    @Autowired AdministratorBootstrapService bootstrap;
    @Autowired AccountProvisioning provision;
    @LocalServerPort int port;
    private String password,adminEmail;
    private UUID adminId,accountA,accountB,siteA,hiddenA,siteB,equipmentA;
    private Client a,b;
    private Browser admin;
    private record Client(UUID user,UUID dossier,Browser web) { }

    @BeforeEach void setup() throws Exception {
        clock.reset(); jdbc.update("DELETE FROM app.auth_rate_bucket"); password=OpaqueTokens.generate()+" passphrase";
        adminEmail="missions-admin-"+UUID.randomUUID()+"@example.invalid"; adminId=bootstrap.create(adminEmail,password); verify(adminId); admin=login(adminEmail);
        var enrollment=body(admin.post("/api/web/v1/auth/mfa/enrollment/start",Map.of()),200);
        String otp=new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30),6,"HmacSHA1").generateOneTimePasswordString(new SecretKeySpec(Base32.decode(enrollment.get("secret").asText()),"HmacSHA1"),clock.instant(),java.util.Locale.ROOT);
        body(admin.post("/api/web/v1/auth/mfa/enrollment/confirm",Map.of("enrollmentId",enrollment.get("enrollmentId").asText(),"code",otp)),200);
        a=seed(); b=seed(); accountA=account(a); accountB=account(b);
        siteA=site(accountA,"Site autorisé"); hiddenA=site(accountA,"Site caché"); siteB=site(accountB,"Autre compte");
        equipmentA=equipment(accountA,siteA);
        grant(accountA,a.user,List.of(siteA),0); grant(accountB,b.user,List.of(siteB),0);
    }
    @Test void publicationAndFinancialRevisionsAreImmutableAndIdempotent() throws Exception {
        String key=key(); var created=body(admin.send("POST",root(accountA,siteA),missionBody(null,null),null,key,true),201);
        var replay=body(admin.send("POST",root(accountA,siteA),missionBody(null,null),null,key,true),201);
        assertThat(replay).isEqualTo(created); UUID mission=id(created,"resourceId");
        assertThat(created.get("version").asLong()).isZero(); assertThat(created.get("state").asText()).isEqualTo("DRAFT");
        var draft=body(admin.get(root(accountA,siteA)+"/"+mission),200);
        assertThat(draft.get("mission").get("state").asText()).isEqualTo("DRAFT"); assertThat(draft.get("mission").get("skills").size()).isEqualTo(2);
        assertThat(draft.get("financial").isNull()).isTrue();
        String publishKey=key(); var first=admin.send("POST",root(accountA,siteA)+"/"+mission+"/publish",financial("150.00","20.50"),0L,publishKey,true);
        var second=admin.send("POST",root(accountA,siteA)+"/"+mission+"/publish",financial("150.00","20.50"),0L,publishKey,true);
        JsonNode published=body(first,200); assertThat(body(second,200)).isEqualTo(published);
        assertThat(published.get("version").asLong()).isEqualTo(1); assertThat(published.get("state").asText()).isEqualTo("PUBLISHED");
        var paid=body(admin.get(root(accountA,siteA)+"/"+mission),200);
        assertThat(paid.get("mission").get("state").asText()).isEqualTo("PUBLISHED");
        assertThat(paid.get("financial").get("number").asLong()).isZero();
        assertThat(paid.get("financial").get("remuneration").asText()).isEqualTo("150.00");
        assertThat(paid.get("financial").get("expenses").asText()).isEqualTo("20.50");
        var revised=body(admin.send("POST",root(accountA,siteA)+"/"+mission+"/financial-revisions",financial("180.00",null),1L,key(),true),200);
        assertThat(revised.get("version").asLong()).isEqualTo(2);
        var current=body(admin.get(root(accountA,siteA)+"/"+mission),200).get("financial");
        assertThat(current.get("number").asLong()).isEqualTo(1); assertThat(current.get("expenses").isNull()).isTrue();
        var history=body(admin.get(root(accountA,siteA)+"/"+mission+"/financial-revisions"),200).get("items");
        assertThat(history.size()).isEqualTo(2); assertThat(history.get(0).get("remuneration").asText()).isEqualTo("150.00");
        assertThat(history.get(1).get("remuneration").asText()).isEqualTo("180.00");
        assertThat(admin.send("POST",root(accountA,siteA)+"/"+mission+"/financial-revisions",financial("190.00",null),1L,key(),true).statusCode()).isEqualTo(412);
        assertThat(admin.send("POST",root(accountA,siteA)+"/"+mission+"/publish",financial("190.00",null),2L,key(),true).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mission_financial_revision WHERE mission_id=?",Integer.class,mission)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.business_event_outbox WHERE resource_id=? AND event_kind IN ('MISSION_CREATED','MISSION_PUBLISHED','MISSION_REVISED')",Integer.class,mission)).isEqualTo(3);
    }
    @Test void wrongInputsWrongSitesAndWrongRolesNeverCreateOrChangeAMission() throws Exception {
        for(var body:List.of(missionBodyWith("title"," "),missionBodyWith("windowEnd",clock.instant().minusSeconds(3600).toString()),
                missionBodyWith("publicLatitude",new BigDecimal("91")),missionBodyWith("publicLongitude",new BigDecimal("-181")),
                missionBodyWith("publicLatitude",new BigDecimal("48.1234567")),missionBodyWith("skills",List.of()),
                missionBodyWith("skills",java.util.Collections.nCopies(11,"x")))) {
            assertThat(admin.send("POST",root(accountA,siteA),body,null,key(),true).statusCode()).isEqualTo(400);
        }
        assertThat(admin.send("POST",root(accountA,siteA),missionBody(UUID.randomUUID(),null),null,key(),true).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mission WHERE account_id=?",Integer.class,accountA)).isZero();
        UUID mission=mission(null,null);
        String email="missions-tech-"+UUID.randomUUID()+"@example.invalid"; UUID tech=provision.createPendingTechnician(email,password); verify(tech);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?",tech); Browser technician=login(email); Browser preMfa=login(adminEmail);
        for(Browser denied:List.of(a.web,technician,preMfa)) {
            assertThat(denied.send("POST",root(accountA,siteA),missionBody(null,null),null,key(),true).statusCode()).isEqualTo(403);
            assertThat(denied.get(root(accountA,siteA)+"/"+mission).statusCode()).isEqualTo(403);
        }
        assertThat(admin.send("POST",root(accountB,siteA),missionBody(null,null),null,key(),true).statusCode()).isEqualTo(404);
        assertThat(admin.get(root(accountB,siteA)+"/"+mission).statusCode()).isEqualTo(404);
        UUID expired=id(body(admin.send("POST",root(accountA,siteA),expiredWindowBody(),null,key(),true),201),"resourceId");
        assertThat(admin.send("POST",root(accountA,siteA)+"/"+expired+"/publish",financial("10.00",null),0L,key(),true).statusCode()).isEqualTo(400);
        assertThat(admin.send("POST",root(accountA,siteA)+"/"+mission+"/financial-revisions",financial("10.00",null),0L,key(),true).statusCode()).isEqualTo(409);
    }
    @Test void linkedTicketsMustBelongToTheSiteAndBeQualifiedBeforePublication() throws Exception {
        UUID ticket=ticket(null); UUID mission=id(body(admin.send("POST",root(accountA,siteA),missionBody(ticket,null),null,key(),true),201),"resourceId");
        assertThat(admin.send("POST",root(accountA,siteA)+"/"+mission+"/publish",financial("150.00",null),0L,key(),true).statusCode()).isEqualTo(409);
        qualify(ticket,0);
        assertThat(admin.send("POST",root(accountA,siteA)+"/"+mission+"/publish",financial("150.00",null),0L,key(),true).statusCode()).isEqualTo(200);
        UUID foreignTicket=id(body(b.web.post(clientRoot(accountB,siteB),ticketBody(null)),201),"resourceId");
        assertThat(admin.send("POST",root(accountA,siteA),missionBody(foreignTicket,null),null,key(),true).statusCode()).isEqualTo(400);
    }
    @Test void optionalContractsMustCoverTheSiteAtPublicationTime() throws Exception {
        UUID revision=id(body(admin.post(contracts(accountA),contractBody(List.of(coverage(siteA,true,List.of())))),201),"revisionId");
        UUID mission=id(body(admin.send("POST",root(accountA,siteA),missionBody(null,revision),null,key(),true),201),"resourceId");
        assertThat(admin.send("POST",root(accountA,siteA)+"/"+mission+"/publish",financial("150.00",null),0L,key(),true).statusCode()).isEqualTo(200);
        UUID equipmentOnly=id(body(admin.post(contracts(accountA),contractBody(List.of(coverage(siteA,false,List.of(equipmentA))))),201),"revisionId");
        UUID second=id(body(admin.send("POST",root(accountA,siteA),missionBody(null,equipmentOnly),null,key(),true),201),"resourceId");
        assertThat(admin.send("POST",root(accountA,siteA)+"/"+second+"/publish",financial("150.00",null),0L,key(),true).statusCode()).isEqualTo(409);
        assertThat(admin.send("POST",root(accountA,siteA),missionBody(null,UUID.randomUUID()),null,key(),true).statusCode()).isEqualTo(404);
    }
    @Test void administrativeProjectionNeverLeaksTicketOrContractPrivateText() throws Exception {
        UUID ticket=id(body(a.web.post(clientRoot(accountA,siteA),ticketBody(null)),201),"resourceId"); qualify(ticket,0);
        UUID revision=id(body(admin.post(contracts(accountA),contractBody(List.of(coverage(siteA,true,List.of())))),201),"revisionId");
        UUID mission=id(body(admin.send("POST",root(accountA,siteA),missionBody(ticket,revision),null,key(),true),201),"resourceId");
        body(admin.send("POST",root(accountA,siteA)+"/"+mission+"/publish",financial("150.00",null),0L,key(),true),200);
        var detail=admin.get(root(accountA,siteA)+"/"+mission); body(detail,200);
        assertThat(detail.body()).doesNotContain("Incident synthétique","Description privée synthétique","Note contractuelle interne","Motif interne de révision");
    }
    @Test void oneConcurrentPublishWinsAndTheOtherSeesTheStaleVersion() throws Exception {
        UUID mission=mission(null,null); String csrf=admin.csrf(); var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> command=()->{ start.await(); return admin.sendWithCsrf("POST",root(accountA,siteA)+"/"+mission+"/publish",financial("150.00",null),0L,key(),csrf).statusCode(); };
            var one=pool.submit(command); var two=pool.submit(command); start.countDown();
            assertThat(List.of(one.get(),two.get())).containsExactlyInAnyOrder(200,412);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.mission_financial_revision WHERE mission_id=?",Integer.class,mission)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM app.mission WHERE mission_id=?",Long.class,mission)).isEqualTo(1);
    }
    @Test void postgresEnforcesCoordinateScaleMoneyAndImmutableRuntimePrivileges() throws Exception {
        UUID mission=mission(null,null);
        assertThatThrownBy(()->jdbc.update("INSERT INTO app.mission_financial_revision(account_id,mission_id,revision_number,remuneration,author_user_id,created_at) VALUES (?,?,9,-1.00,?,now())",accountA,mission,adminId)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO app.mission_financial_revision(account_id,mission_id,revision_number,remuneration,author_user_id,created_at) VALUES (?,?,9,1.234,?,now())",accountA,mission,adminId)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO app.mission_skill(account_id,mission_id,skill_order,skill) VALUES (?,?,9,'réseau')",accountA,mission)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        for(String sql:List.of("UPDATE app.mission SET public_latitude=1","UPDATE app.mission_financial_revision SET remuneration=1","DELETE FROM app.mission")) {
            try(var c=PostgisFixture.runtime(DATABASE);var st=c.createStatement()) { assertThatThrownBy(()->st.execute(sql)).isInstanceOf(SQLException.class).satisfies(f->assertThat(((SQLException)f).getSQLState()).isEqualTo("42501")); }
        }
    }
    @Test void discoveryExposesOnlyPublishedPublicTermsWithinTheBoundedRadius() throws Exception {
        UUID paris=publishedMission(new BigDecimal("58.8566"),new BigDecimal("12.3522"),"Maintenance Paris",List.of("Réseau","Windows"),"Motif interne de découverte");
        UUID lyon=publishedMission(new BigDecimal("57.7640"),new BigDecimal("13.8357"),"Maintenance Lyon",List.of("Réseau"),"Motif interne Lyon");
        UUID draft=mission(null,null);
        String token=activeTechnician().token();
        var local=mobileGet(token,nearby("58.8566","12.3522","50",null,null,null,null,"20"));
        JsonNode one=body(local,200);
        assertThat(one.get("items").size()).isEqualTo(1);
        JsonNode item=one.get("items").get(0);
        assertThat(item.get("missionId").asText()).isEqualTo(paris.toString()).isNotEqualTo(draft.toString());
        assertThat(item.get("distanceMeters").asLong()).isZero();
        assertThat(item.get("publicSummary").asText()).isEqualTo("Résumé public synthétique");
        assertThat(item.get("financial").get("remuneration").asText()).isEqualTo("150.00");
        assertThat(item.get("financial").get("expenses").isNull()).isTrue();
        for(String privateField:List.of("accountId","siteId","ticketId","contractRevisionId","reason","authorUserId","internalNotes")) {
            assertThat(item.has(privateField)).as(privateField).isFalse();
        }
        assertThat(local.body()).doesNotContain("Motif interne");
        assertThat(one.get("nextCursor").isNull()).isTrue();
        var wide=body(mobileGet(token,nearby("58.8566","12.3522","200",null,null,null,null,"20")),200);
        assertThat(wide.get("items").size()).isEqualTo(2);
        assertThat(wide.get("items").get(0).get("missionId").asText()).isEqualTo(paris.toString());
        assertThat(wide.get("items").get(1).get("missionId").asText()).isEqualTo(lyon.toString());
        assertThat(wide.get("items").get(1).get("distanceMeters").asLong()).isBetween(100000L,200000L);
        var filtered=body(mobileGet(token,nearby("58.8566","12.3522","200","windows",null,null,null,"20")),200);
        assertThat(filtered.get("items").size()).isEqualTo(1);
        assertThat(filtered.get("items").get(0).get("missionId").asText()).isEqualTo(paris.toString());
        assertThat(body(mobileGet(token,nearby("58.8566","12.3522","200",null,clock.instant().plusSeconds(10800).toString(),null,null,"20")),200)
                .get("items").size()).isZero();
        assertThat(body(mobileGet(token,nearby("58.8566","12.3522","200",null,null,clock.instant().plusSeconds(1800).toString(),null,"20")),200)
                .get("items").size()).isZero();
        assertThat(body(mobileGet(token,nearby("58.8566","12.3522","200",null,clock.instant().plusSeconds(5400).toString(),
                clock.instant().plusSeconds(6000).toString(),null,"20")),200).get("items").size()).isEqualTo(2);
        var first=body(mobileGet(token,nearby("58.8566","12.3522","200",null,null,null,null,"1")),200);
        assertThat(first.get("items").size()).isEqualTo(1);
        assertThat(first.get("nextCursor").isNull()).isFalse();
        var second=body(mobileGet(token,nearby("58.8566","12.3522","200",null,null,null,first.get("nextCursor").asText(),"1")),200);
        assertThat(second.get("items").size()).isEqualTo(1);
        assertThat(second.get("items").get(0).get("missionId").asText()).isEqualTo(lyon.toString());
        assertThat(second.get("nextCursor").isNull()).isTrue();
    }
    @Test void discoveryRejectsInvertedInvalidAndUnboundedQueriesAndRefusesInactiveTechnicians() throws Exception {
        publishedMission(new BigDecimal("38.8566"),new BigDecimal("-7.6478"),"Maintenance Paris",List.of("Réseau","Windows"),"Motif interne");
        MobileTechnician active=activeTechnician();
        assertThat(body(mobileGet(active.token(),nearby("-7.6478","38.8566","50",null,null,null,null,"20")),200)
                .get("items").size()).isZero();
        for(String path:List.of(
                nearby("91","-7.6478","50",null,null,null,null,"20"),
                nearby("38.8566","181","50",null,null,null,null,"20"),
                nearby("38.8566","-7.6478","9",null,null,null,null,"20"),
                nearby("38.8566","-7.6478","201",null,null,null,null,"20"),
                nearby("38.8566","-7.6478","50",null,null,null,null,"0"),
                nearby("38.8566","-7.6478","50",null,null,null,null,"101"),
                nearby("38.8566","-7.6478","50",null,clock.instant().plusSeconds(7200).toString(),
                        clock.instant().plusSeconds(3600).toString(),null,"20"),
                nearby("38.8566","-7.6478","50",null,null,null,"not-a-cursor","20"),
                nearby("38.8566","-7.6478","50","a,b,c,d,e,f,g,h,i,j,k",null,null,null,"20"))) {
            assertThat(mobileGet(active.token(),path).statusCode()).as(path).isEqualTo(400);
        }
        assertThat(mobileGet(null,nearby("38.8566","-7.6478","50",null,null,null,null,"20")).statusCode()).isEqualTo(401);
        String pendingEmail="discovery-pending-"+UUID.randomUUID()+"@example.invalid";
        UUID pending=provision.createPendingTechnician(pendingEmail,password); verify(pending);
        assertThat(mobileGet(mobileLogin(pendingEmail),nearby("38.8566","-7.6478","50",null,null,null,null,"20")).statusCode()).isEqualTo(403);
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?",active.user());
        assertThat(mobileGet(active.token(),nearby("38.8566","-7.6478","50",null,null,null,null,"20")).statusCode()).isIn(401,403);
    }
    @Test void discoveryPlanUsesThePublishedGiSTIndexOnARealisticGrid() throws Exception {
        jdbc.update("""
                INSERT INTO app.mission(mission_id,account_id,site_id,title,public_summary,window_start,window_end,
                    public_latitude,public_longitude,public_label,public_location,state,financial_revision,version,created_by,created_at,updated_at,published_at)
                SELECT ('00000000-0000-0000-0000-'||lpad(i::text,12,'0'))::uuid,?,?,'Mission '||i,'Résumé public',
                    now()+interval '1 hour',now()+interval '2 hours',
                    48.80+(i%50)*0.001,2.30+(i/50)*0.001,'Paris',
                    ST_SetSRID(ST_MakePoint(2.30+(i/50)*0.001,48.80+(i%50)*0.001),4326)::geography,
                    'PUBLISHED',0,1,?,now(),now(),now()
                FROM generate_series(1,2500) i
                """,accountA,siteA,adminId);
        jdbc.update("""
                INSERT INTO app.mission_financial_revision(account_id,mission_id,revision_number,remuneration,expenses,reason,author_user_id,created_at)
                SELECT ?,mission_id,0,100.00,NULL,NULL,?,now() FROM app.mission WHERE account_id=? AND site_id=?
                """,accountA,adminId,accountA,siteA);
        jdbc.update("INSERT INTO app.mission_skill(account_id,mission_id,skill_order,skill) SELECT ?,mission_id,0,'Réseau' FROM app.mission WHERE account_id=? AND site_id=?",accountA,accountA,siteA);
        jdbc.update("INSERT INTO app.mission_skill(account_id,mission_id,skill_order,skill) SELECT ?,mission_id,1,'Windows' FROM app.mission WHERE account_id=? AND site_id=?",accountA,accountA,siteA);
        jdbc.execute("ANALYZE app.mission"); jdbc.execute("ANALYZE app.mission_skill");
        String plan=new TransactionTemplate(transactions).execute(status -> {
            jdbc.execute("SET LOCAL enable_seqscan=off");
            return String.join("\n",jdbc.queryForList("""
                    EXPLAIN (FORMAT TEXT)
                    WITH origin AS (SELECT ST_SetSRID(ST_MakePoint(2.30,48.80),4326)::geography AS point)
                    SELECT m.mission_id FROM app.mission m CROSS JOIN origin o
                    WHERE m.state='PUBLISHED' AND m.window_end>now()
                      AND ST_DWithin(m.public_location,o.point,5000)
                      AND EXISTS(SELECT 1 FROM app.mission_skill s2 WHERE s2.account_id=m.account_id
                          AND s2.mission_id=m.mission_id AND lower(s2.skill)=ANY(ARRAY['windows']::text[]))
                    ORDER BY ST_Distance(m.public_location,o.point),m.mission_id LIMIT 21
                    """,String.class));
        });
        assertThat(plan).contains("mission_published_location_gix");
    }
    private UUID publishedMission(BigDecimal latitude,BigDecimal longitude,String title,List<String> skills,String reason) throws Exception {
        var input=missionBody(null,null); input.put("title",title); input.put("skills",skills);
        input.put("publicLatitude",latitude); input.put("publicLongitude",longitude); input.put("publicLabel",title);
        UUID mission=id(body(admin.send("POST",root(accountA,siteA),input,null,key(),true),201),"resourceId");
        var terms=new LinkedHashMap<String,Object>(); terms.put("remuneration","150.00"); terms.put("expenses",null); terms.put("reason",reason);
        body(admin.send("POST",root(accountA,siteA)+"/"+mission+"/publish",terms,0L,key(),true),200);
        return mission;
    }
    private MobileTechnician activeTechnician() throws Exception {
        String email="discovery-tech-"+UUID.randomUUID()+"@example.invalid";
        UUID user=provision.createPendingTechnician(email,password); verify(user);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?",user);
        return new MobileTechnician(user,mobileLogin(email));
    }
    private String mobileLogin(String email) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(base("/api/mobile/v1/auth/login"))).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email",email,"password",password)))).build();
        var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
        return body(response,200).get("accessToken").asText();
    }
    private HttpResponse<String> mobileGet(String token,String path) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(base(path))).GET();
        if(token!=null) { request.header("Authorization","Bearer "+token); }
        return HttpClient.newHttpClient().send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    private String base(String path) { return "http://127.0.0.1:"+port+path; }
    private static String nearby(String latitude,String longitude,String radius,String skills,String from,String to,String cursor,String size) {
        var query=new StringBuilder("/api/mobile/v1/technician/missions/nearby?latitude=").append(latitude)
                .append("&longitude=").append(longitude).append("&radiusKm=").append(radius).append("&size=").append(size);
        if(skills!=null) { query.append("&skills=").append(skills); }
        if(from!=null) { query.append("&from=").append(from); }
        if(to!=null) { query.append("&to=").append(to); }
        if(cursor!=null) { query.append("&cursor=").append(cursor); }
        return query.toString();
    }
    private record MobileTechnician(UUID user,String token) { }
    private Client seed() throws Exception {
        String email="missions-client-"+UUID.randomUUID()+"@example.invalid"; UUID user=provision.createPendingClient(email,password); verify(user); jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?",user);
        UUID invitation=UUID.randomUUID(),dossier=UUID.randomUUID(); Timestamp now=Timestamp.from(clock.instant());
        jdbc.update("INSERT INTO app.client_invitation(invitation_id,email_normalized,email_display,invited_by,last_sent_by,accepted_user_id,created_at,expires_at,accepted_at) VALUES (?,?,?,?,?,?,?,?,?)",invitation,email,email,adminId,adminId,user,now,Timestamp.from(clock.instant().plusSeconds(172800)),now);
        jdbc.update("INSERT INTO app.client_onboarding(dossier_id,user_id,invitation_id,state,account_type,first_name,last_name,phone,address,organization_name,created_at,updated_at) VALUES (?,?,?,'APPROVED','BUSINESS','Client','Synthétique','+33 100000000','Adresse synthétique','Organisation synthétique',?,?)",dossier,user,invitation,now,now);
        return new Client(user,dossier,login(email));
    }
    private void verify(UUID user) { jdbc.update("UPDATE app.local_credential SET email_verified_at=? WHERE user_id=?",Timestamp.from(clock.instant()),user); }
    private UUID account(Client c) throws Exception { return id(body(admin.post("/api/web/v1/admin/client-accounts",Map.of("dossierId",c.dossier)),201),"accountId"); }
    private UUID site(UUID account,String name) throws Exception { return id(body(admin.post(adminAccount(account)+"/sites",Map.of("name",name,"address","Adresse synthétique","timeZone","Europe/Paris")),201),"siteId"); }
    private UUID equipment(UUID account,UUID site) throws Exception { return id(body(admin.post(adminAccount(account)+"/sites/"+site+"/equipment",Map.of("label","Poste synthétique","category","PC")),201),"equipmentId"); }
    private void grant(UUID account,UUID user,List<UUID> sites,long version) throws Exception { body(admin.send("PUT",adminAccount(account)+"/members/"+user+"/site-access",Map.of("allSites",false,"siteIds",sites),version,null,true),200); }
    private LinkedHashMap<String,Object> ticketBody(UUID equipment) { var input=new LinkedHashMap<String,Object>(); input.put("equipmentId",equipment); input.put("title","Incident synthétique"); input.put("description","Description privée synthétique"); input.put("requestedPriority","HIGH"); return input; }
    private UUID ticket(UUID equipment) throws Exception { return id(body(a.web.post(clientRoot(accountA,siteA),ticketBody(equipment)),201),"resourceId"); }
    private UUID mission(UUID ticket,UUID contract) throws Exception { return id(body(admin.send("POST",root(accountA,siteA),missionBody(ticket,contract),null,key(),true),201),"resourceId"); }
    private LinkedHashMap<String,Object> missionBody(UUID ticket,UUID contract) {
        var input=new LinkedHashMap<String,Object>(); input.put("ticketId",ticket); input.put("contractRevisionId",contract);
        input.put("title","Maintenance réseau"); input.put("publicSummary","Résumé public synthétique");
        input.put("skills",List.of("Réseau","Windows")); input.put("windowStart",clock.instant().plusSeconds(3600).toString());
        input.put("windowEnd",clock.instant().plusSeconds(7200).toString()); input.put("publicLatitude",new BigDecimal("48.8566"));
        input.put("publicLongitude",new BigDecimal("2.3522")); input.put("publicLabel","Paris"); return input;
    }
    private LinkedHashMap<String,Object> expiredWindowBody() {
        var input=missionBody(null,null); input.put("windowStart",clock.instant().minusSeconds(7200).toString()); input.put("windowEnd",clock.instant().minusSeconds(3600).toString()); return input;
    }
    private LinkedHashMap<String,Object> missionBodyWith(String field,Object value) { var input=missionBody(null,null); input.put(field,value); return input; }
    private Map<String,Object> financial(String remuneration,Object expenses) { var input=new LinkedHashMap<String,Object>(); input.put("remuneration",remuneration); input.put("expenses",expenses); input.put("reason","Révision tarifaire"); return input; }
    private Map<String,Object> coverage(UUID site,boolean whole,List<UUID> equipment) { return Map.of("siteId",site,"wholeSite",whole,"equipmentIds",equipment); }
    private LinkedHashMap<String,Object> contractBody(List<Map<String,Object>> coverage) {
        var input=new LinkedHashMap<String,Object>(); input.put("publicName","Contrat public"); input.put("internalNotes","Note contractuelle interne"); input.put("reason","Motif interne de révision");
        input.put("validFrom",clock.instant().minusSeconds(3600).toString()); input.put("validUntil",clock.instant().plusSeconds(86400).toString()); input.put("coverage",coverage);
        input.put("calendar",Map.of("mode","ALWAYS","weekly",List.of(),"overrides",List.of())); input.put("slaRules",List.of()); return input;
    }
    private void qualify(UUID ticket,long version) throws Exception { body(admin.send("POST",adminAccount(accountA)+"/sites/"+siteA+"/tickets/"+ticket+"/qualify",Map.of("retainedPriority","NORMAL","reason","Diagnostic synthétique"),version,key(),true),200); }
    private String adminAccount(UUID account) { return "/api/web/v1/admin/client-accounts/"+account; }
    private String contracts(UUID account) { return adminAccount(account)+"/contracts"; }
    private String clientRoot(UUID account,UUID site) { return "/api/web/v1/client/accounts/"+account+"/sites/"+site+"/tickets"; }
    private String root(UUID account,UUID site) { return adminAccount(account)+"/sites/"+site+"/missions"; }
    private static String key() { return UUID.randomUUID().toString(); }
    private static UUID id(JsonNode node,String field) { return UUID.fromString(node.get(field).asText()); }
    private JsonNode body(HttpResponse<String> response,int expected) { assertThat(response.statusCode()).isEqualTo(expected); return json.readTree(response.body().isEmpty()?"null":response.body()); }
    private Browser login(String email) throws Exception { Browser browser=new Browser(); body(browser.post("/api/web/v1/auth/login",Map.of("email",email,"password",password)),200); return browser; }
    private final class Browser {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        HttpResponse<String> get(String path) throws Exception { return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString()); }
        String csrf() throws Exception { return body(get("/api/web/v1/auth/csrf"),200).get("token").asText(); }
        HttpResponse<String> post(String path,Object input) throws Exception { return send("POST",path,input,null,key(),true); }
        HttpResponse<String> send(String method,String path,Object input,Long version,String key,boolean csrf) throws Exception { return sendWithCsrf(method,path,input,version,key,csrf?csrf():null); }
        HttpResponse<String> sendWithCsrf(String method,String path,Object input,Long version,String key,String csrf) throws Exception {
            var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json");
            if(version!=null) { request.header("If-Match","\""+version+"\""); } if(key!=null) { request.header("Idempotency-Key",key); } if(csrf!=null) { request.header("X-CSRF-TOKEN",csrf); }
            return http.send(request.method(method,HttpRequest.BodyPublishers.ofString(json.writeValueAsString(input))).build(),HttpResponse.BodyHandlers.ofString());
        }
    }
}
