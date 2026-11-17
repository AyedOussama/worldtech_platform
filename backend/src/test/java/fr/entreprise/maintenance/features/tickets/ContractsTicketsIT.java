package fr.entreprise.maintenance.features.tickets;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
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
@Import(ContractsTicketsIT.TimeConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ContractsTicketsIT {
    @TestConfiguration(proxyBeanMethods=false) static class TimeConfiguration { @Bean @Primary MutableClock testClock() { return new MutableClock(); } }
    @Container static final GenericContainer<?> DATABASE=PostgisFixture.container();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) { PostgisFixture.properties(r,DATABASE); }
    @Autowired JdbcTemplate jdbc;
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
        adminEmail="tickets-admin-"+UUID.randomUUID()+"@example.invalid"; adminId=bootstrap.create(adminEmail,password); verify(adminId); admin=login(adminEmail);
        var enrollment=body(admin.post("/api/web/v1/auth/mfa/enrollment/start",Map.of()),200);
        String otp=new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30),6,"HmacSHA1").generateOneTimePasswordString(new SecretKeySpec(Base32.decode(enrollment.get("secret").asText()),"HmacSHA1"),clock.instant(),java.util.Locale.ROOT);
        body(admin.post("/api/web/v1/auth/mfa/enrollment/confirm",Map.of("enrollmentId",enrollment.get("enrollmentId").asText(),"code",otp)),200);
        a=seed(); b=seed(); accountA=account(a); accountB=account(b);
        siteA=site(accountA,"Site autorisé"); hiddenA=site(accountA,"Site caché"); siteB=site(accountB,"Autre compte");
        equipmentA=equipment(accountA,siteA);
        grant(accountA,a.user,List.of(siteA),0); grant(accountB,b.user,List.of(siteB),0);
    }
    @Test void declarationWithoutEquipmentContractOrMoneyIsIdempotentAndPreparesOnlyMetadata() throws Exception {
        String key=key(); var input=ticketBody(null); var one=a.web.send("POST",ct(siteA),input,null,key,true); var two=a.web.send("POST",ct(siteA),input,null,key,true);
        JsonNode first=body(one,201); assertThat(body(two,201)).isEqualTo(first); UUID ticket=id(first,"resourceId");
        assertThat(one.headers().firstValue("Location").orElse("")).endsWith(ticket.toString()); assertThat(a.web.get(one.headers().firstValue("Location").orElseThrow()).statusCode()).isEqualTo(200);
        var detail=body(a.web.get(ct(siteA)+"/"+ticket),200); assertThat(detail.get("coverage").asText()).isEqualTo("UNQUALIFIED"); assertThat(detail.get("ticket").get("equipmentId").isNull()).isTrue();
        JsonNode proposal=body(a.web.get(ct(siteA)+"/"+ticket+"/proposals"),200).get("items").get(0);
        assertThat(proposal.get("remuneration").isNull()).isTrue(); assertThat(proposal.get("expenses").isNull()).isTrue();
        assertThat(proposal.get("taxBasis").asText()).isEqualTo("HT"); assertThat(proposal.get("currency").asText()).isEqualTo("EUR");
        assertThat(count("ticket",ticket)).isEqualTo(1); assertThat(jdbc.queryForObject("SELECT count(*) FROM app.business_event_outbox WHERE resource_id=? AND event_kind='TICKET_CREATED'",Integer.class,ticket)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.command_receipt WHERE result_resource_id=?",Integer.class,ticket)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.ticket_change WHERE ticket_id=?",Integer.class,ticket)).isEqualTo(1);
    }
    @Test void currentSiteRightsRejectCreationWrongEquipmentAndEveryPrivateSubresourceBeforeTotals() throws Exception {
        UUID hiddenEquipment=equipment(accountA,hiddenA),foreignEquipment=equipment(accountB,siteB);
        for(UUID equipment:List.of(hiddenEquipment,foreignEquipment)) { assertThat(a.web.post(ct(siteA),ticketBody(equipment)).statusCode()).isEqualTo(404); }
        assertThat(a.web.post(ct(hiddenA),ticketBody(null)).statusCode()).isEqualTo(404);
        UUID ticket=ticket(equipmentA);
        var foreignInput=ticketBody(null); foreignInput.put("title","Incident autre compte confidentiel");
        UUID foreignTicket=id(body(b.web.post(clientRoot(accountB,siteB),foreignInput),201),"resourceId");
        var hiddenResponse=a.web.get(ct(siteA)+"/"+foreignTicket); assertThat(hiddenResponse.statusCode()).isEqualTo(404); assertThat(hiddenResponse.body()).doesNotContain("Incident autre compte confidentiel",foreignTicket.toString());
        for(String suffix:List.of("","/proposals","/history","/messages")) {
            var foreign=b.web.get(clientRoot(accountA,siteA)+"/"+ticket+suffix); assertThat(foreign.statusCode()).isEqualTo(404); assertThat(foreign.body()).doesNotContain("Incident synthétique","Description privée",ticket.toString());
            assertThat(a.web.get(clientRoot(accountA,hiddenA)+"/"+ticket+suffix).statusCode()).isEqualTo(404);
        }
        for(String query:List.of("","?size=1","?equipmentId="+foreignEquipment,"?state=RESOLVED")) { assertThat(a.web.get(clientRoot(accountB,siteB)+query).statusCode()).isEqualTo(404); }
        assertThat(a.web.get(ct(siteA)+"?equipmentId="+hiddenEquipment).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.ticket WHERE account_id=?",Integer.class,accountA)).isEqualTo(1);
    }
    @Test void listsAndCountsUseTheSameSiteAndRequestedPriorityRemainsDistinct() throws Exception {
        UUID ticket=ticket(null); ticket(equipmentA);
        body(admin.send("POST",at(siteA)+"/"+ticket+"/qualify",Map.of("retainedPriority","LOW","reason","Diagnostic"),0L,key(),true),200);
        var page=body(a.web.get(ct(siteA)+"?size=1&requestedPriority=HIGH"),200); assertThat(page.get("total").asInt()).isEqualTo(2); assertThat(page.get("items").size()).isEqualTo(1);
        var detail=body(a.web.get(ct(siteA)+"/"+ticket),200).get("ticket"); assertThat(detail.get("requestedPriority").asText()).isEqualTo("HIGH"); assertThat(detail.get("retainedPriority").asText()).isEqualTo("LOW");
        assertThat(body(a.web.get(ct(siteA)+"?state=QUALIFIED"),200).get("total").asInt()).isEqualTo(1);
        assertThat(body(a.web.get(ct(siteA)+"?requestedPriority=LOW"),200).get("total").asInt()).isZero();
    }
    @Test void clientContractProjectionContainsNeitherOtherSitesNorInternalNotesOrReasons() throws Exception {
        var input=contractBody(List.of(coverage(siteA,true,List.of()),coverage(hiddenA,true,List.of())));
        var revision=body(admin.post(contracts(accountA),input),201); UUID rev=id(revision,"revisionId");
        var response=a.web.get(coverageRoot(accountA,siteA)+"/"+rev); var visible=body(response,200);
        assertThat(visible.get("wholeSite").asBoolean()).isTrue(); assertThat(response.body()).doesNotContain(hiddenA.toString(),"Note contractuelle interne","Motif interne de révision","internalNotes","coverage");
        assertThat(body(a.web.get(coverageRoot(accountA,siteA)+"?size=1"),200).get("total").asInt()).isEqualTo(1);
        assertThat(a.web.get(coverageRoot(accountA,hiddenA)).statusCode()).isEqualTo(404);
        assertThat(b.web.get(coverageRoot(accountA,siteA)+"/"+rev).statusCode()).isEqualTo(404);
        assertThat(a.web.get(contracts(accountA)).statusCode()).isEqualTo(403);
    }
    @Test void aContractRevisionAndTheSiteTimeZoneRemainFrozenForAnAlreadyQualifiedTicket() throws Exception {
        var revision=body(admin.post(contracts(accountA),contractBody(List.of(coverage(siteA,true,List.of())))),201); UUID rev=id(revision,"revisionId"),contract=id(revision,"contractId");
        UUID futureEquipment=equipment(accountA,siteA),ticket=ticket(futureEquipment); qualify(ticket,rev,0);
        var updated=contractBody(List.of(coverage(hiddenA,true,List.of()))); updated.put("publicName","Nouvelles conditions");
        var next=body(admin.send("POST",contracts(accountA)+"/"+contract+"/revisions",updated,0L,null,true),201);
        assertThat(next.get("version").asInt()).isEqualTo(1); assertThat(next.get("revisionId").asText()).isNotEqualTo(rev.toString());
        assertThat(admin.send("POST",contracts(accountA)+"/"+contract+"/revisions",updated,0L,null,true).statusCode()).isEqualTo(412);
        body(admin.send("PATCH",adminAccount(accountA)+"/sites/"+siteA,Map.of("timeZone","Indian/Reunion"),0L,null,true),200);
        var t=body(a.web.get(ct(siteA)+"/"+ticket),200); assertThat(t.get("contractRevisionId").asText()).isEqualTo(rev.toString()); assertThat(t.get("siteTimeZone").asText()).isEqualTo("Europe/Paris");
        assertThat(body(admin.get(contracts(accountA)+"/"+contract+"/revisions/"+rev),200).get("definition").get("publicName").asText()).isEqualTo("Contrat public");
        assertThat(body(admin.get(contracts(accountA)+"/"+contract+"/revisions?size=1"),200).get("items").size()).isEqualTo(1);
    }
    @Test void qualificationChecksHalfOpenValidityAtDeclarationNotAtTheLaterAdminRequest() throws Exception {
        UUID ticket=ticket(null); Instant declared=jdbc.queryForObject("SELECT created_at FROM app.ticket WHERE ticket_id=?",Timestamp.class,ticket).toInstant();
        for(var bounds:List.of(List.of(declared.minusSeconds(10),declared),List.of(declared.plusSeconds(1),declared.plusSeconds(10)))) {
            var input=contractBody(List.of(coverage(siteA,true,List.of()))); input.put("validFrom",bounds.get(0).toString()); input.put("validUntil",bounds.get(1).toString());
            UUID revision=id(body(admin.post(contracts(accountA),input),201),"revisionId"); assertThat(admin.send("POST",at(siteA)+"/"+ticket+"/qualify",Map.of("retainedPriority","NORMAL","contractRevisionId",revision,"reason","Qualification"),0L,key(),true).statusCode()).isEqualTo(409);
        }
        var valid=contractBody(List.of(coverage(siteA,true,List.of()))); valid.put("validFrom",declared.toString()); valid.put("validUntil",declared.plusSeconds(1).toString());
        UUID revision=id(body(admin.post(contracts(accountA),valid),201),"revisionId"); clock.advance(2); qualify(ticket,revision,0);
        assertThat(body(a.web.get(ct(siteA)+"/"+ticket),200).get("coverage").asText()).isEqualTo("COVERED");
    }
    @Test void equipmentOnlyCoverageCannotCoverAGeneralIncidentOrAnotherEquipment() throws Exception {
        UUID other=equipment(accountA,siteA); var revision=body(admin.post(contracts(accountA),contractBody(List.of(coverage(siteA,false,List.of(equipmentA))))),201); UUID rev=id(revision,"revisionId");
        for(UUID ticket:List.of(ticket(null),ticket(other))) { assertThat(admin.send("POST",at(siteA)+"/"+ticket+"/qualify",Map.of("retainedPriority","NORMAL","contractRevisionId",rev,"reason","Qualification"),0L,key(),true).statusCode()).isEqualTo(409); }
        qualify(ticket(equipmentA),rev,0);
        assertThat(body(a.web.get(coverageRoot(accountA,siteA)+"/"+rev),200).get("equipmentIds").size()).isEqualTo(1);
    }
    @Test void contractCoverageCommandsRejectOtherAccountsOrSitesAndCalendarErrorsWithoutCreatingRows() throws Exception {
        for(var covered:List.of(coverage(siteB,true,List.of()),coverage(hiddenA,false,List.of(equipmentA)))) { assertThat(admin.post(contracts(accountA),contractBody(List.of(covered))).statusCode()).isEqualTo(404); }
        var input=contractBody(List.of(coverage(siteA,true,List.of()))); input.put("calendar",Map.of("mode","WEEKLY","weekly",List.of(Map.of("dayOfWeek",1,"startMinute",540,"endMinute",720),Map.of("dayOfWeek",1,"startMinute",700,"endMinute",800)),"overrides",List.of()));
        assertThat(admin.post(contracts(accountA),input).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.maintenance_contract WHERE account_id=?",Integer.class,accountA)).isZero();
    }
    @Test void proposalsPreserveNullAndZeroAndRejectNumericJsonNegativeOrExcessPrecision() throws Exception {
        for(Object value:List.of(123,1.25,Map.of("x","1.00"),List.of("1.00"),"-1.00","1.234","10000000000.00","1e2")) {
            var input=ticketBody(null); input.put("proposal",Map.of("remuneration",value)); assertThat(a.web.post(ct(siteA),input).statusCode()).isEqualTo(400);
        }
        UUID ticket=ticket(null); body(a.web.send("POST",ct(siteA)+"/"+ticket+"/proposals",Map.of("remuneration","0.00","expenses","9999999999.99"),0L,key(),true),200);
        var items=body(a.web.get(ct(siteA)+"/"+ticket+"/proposals"),200).get("items"); assertThat(items.size()).isEqualTo(2);
        assertThat(items.get(0).get("remuneration").isNull()).isTrue(); assertThat(items.get(1).get("remuneration").asText()).isEqualTo("0.00");
        assertThat(items.get(1).get("authorUserId").asText()).isEqualTo(a.user.toString());
        qualify(ticket,null,1); assertThat(a.web.send("POST",ct(siteA)+"/"+ticket+"/proposals",Map.of(),2L,key(),true).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_financial_proposal WHERE ticket_id=?",Integer.class,ticket)).isEqualTo(2);
    }
    @Test void publicExchangesAndCursorsNeverIncludeInternalNotesOrTheirCounts() throws Exception {
        UUID ticket=ticket(null); String path=ct(siteA)+"/"+ticket,adminPath=at(siteA)+"/"+ticket;
        body(admin.send("POST",adminPath+"/internal-notes",Map.of("text","Note strictement interne"),0L,key(),true),200);
        String key=key(); body(admin.send("POST",adminPath+"/messages",Map.of("text","Réponse publique"),0L,key,true),200); body(admin.send("POST",adminPath+"/messages",Map.of("text","Réponse publique"),0L,key,true),200);
        var publicResponse=a.web.get(path+"/messages"); var messages=body(publicResponse,200); assertThat(messages.get("items").size()).isEqualTo(1); assertThat(messages.get("nextAfter").asLong()).isZero();
        assertThat(publicResponse.body()).doesNotContain("Note strictement interne","internal"); assertThat(body(a.web.get(path),200).get("ticket").get("version").asLong()).isZero();
        assertThat(body(a.web.get(path+"/history"),200).get("items").size()).isEqualTo(1);
        assertThat(a.web.get(adminPath+"/internal-notes").statusCode()).isEqualTo(403); assertThat(a.web.get(path+"/internal-notes").statusCode()).isEqualTo(403);
        assertThat(body(admin.get(adminPath+"/internal-notes"),200).get("items").get(0).get("text").asText()).isEqualTo("Note strictement interne");
    }
    @Test void remoteResolutionCanBeReopenedByClientThenExplicitlyClosedWithoutAnyMission() throws Exception {
        UUID ticket=ticket(null); qualify(ticket,null,0); transition(admin,at(siteA),ticket,"resolve",1);
        assertThat(body(a.web.get(ct(siteA)+"/"+ticket),200).get("ticket").get("state").asText()).isEqualTo("RESOLVED");
        transition(a.web,ct(siteA),ticket,"report-persistence",2); transition(admin,at(siteA),ticket,"resolve",3); transition(a.web,ct(siteA),ticket,"confirm-resolution",4);
        assertThat(body(a.web.get(ct(siteA)+"/"+ticket),200).get("ticket").get("state").asText()).isEqualTo("CLOSED");
        assertThat(a.web.send("POST",ct(siteA)+"/"+ticket+"/report-persistence",Map.of("reason","Toujours en panne"),5L,key(),true).statusCode()).isEqualTo(409);
        assertThat(admin.send("POST",at(siteA)+"/"+ticket+"/resolve",Map.of("reason","Interdit"),5L,key(),true).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.ticket_change WHERE ticket_id=?",Integer.class,ticket)).isEqualTo(6);
    }
    @Test void waitingAndResumptionKeepAnEventTrailWithoutInventingASlaPause() throws Exception {
        var input=contractBody(List.of(coverage(siteA,true,List.of()))); input.put("slaRules",List.of(Map.of("priority","NORMAL","metric","RESOLUTION","targetMinutes",120,"pauseWhileWaitingClient",false)));
        UUID revision=id(body(admin.post(contracts(accountA),input),201),"revisionId"),ticket=ticket(null); qualify(ticket,revision,0);
        transition(admin,at(siteA),ticket,"wait-client",1); clock.advance(60); transition(admin,at(siteA),ticket,"resume",2);
        var changes=body(a.web.get(ct(siteA)+"/"+ticket+"/history?after=0"),200).get("items"); assertThat(changes.size()).isEqualTo(3);
        assertThat(changes.get(1).get("action").asText()).isEqualTo("WAIT"); assertThat(changes.get(2).get("action").asText()).isEqualTo("RESUME");
        assertThat(jdbc.queryForObject("SELECT pause_while_waiting_client FROM app.contract_sla_rule WHERE revision_id=?",Boolean.class,revision)).isFalse();
    }
    @Test void lossOfSiteOrMembershipAccessAlsoRefusesAReceiptReplayWithTheSameSession() throws Exception {
        String key=key(); var input=ticketBody(null); UUID ticket=id(body(a.web.send("POST",ct(siteA),input,null,key,true),201),"resourceId");
        grant(accountA,a.user,List.of(),1); assertThat(a.web.send("POST",ct(siteA),input,null,key,true).statusCode()).isEqualTo(404); assertThat(a.web.get(ct(siteA)+"/"+ticket).statusCode()).isEqualTo(404);
        grant(accountA,a.user,List.of(siteA),2); UUID member=jdbc.queryForObject("SELECT member_id FROM app.client_member WHERE account_id=? AND user_id=? AND active",UUID.class,accountA,a.user);
        body(admin.send("POST",adminAccount(accountA)+"/members/"+member+"/remove",Map.of(),0L,null,true),200);
        assertThat(a.web.send("POST",ct(siteA),input,null,key,true).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.ticket WHERE ticket_id=?",Integer.class,ticket)).isEqualTo(1);
    }
    @Test void mismatchedAndExpiredIdempotencyKeysNeverCreateAdditionalEffects() throws Exception {
        String key=key(); UUID ticket=id(body(a.web.send("POST",ct(siteA),ticketBody(null),null,key,true),201),"resourceId");
        var different=ticketBody(null); different.put("title","Autre demande"); assertThat(a.web.send("POST",ct(siteA),different,null,key,true).statusCode()).isEqualTo(409);
        assertThat(a.web.send("POST",ct(siteA),ticketBody(null),null,"short",true).statusCode()).isEqualTo(400);
        try(var c=PostgisFixture.migrator(DATABASE);var s=c.prepareStatement("UPDATE app.command_receipt SET created_at=created_at-interval '2 days',expires_at=created_at-interval '1 day' WHERE result_resource_id=?")) { s.setObject(1,ticket); s.executeUpdate(); }
        assertThat(a.web.send("POST",ct(siteA),ticketBody(null),null,key,true).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.ticket WHERE account_id=?",Integer.class,accountA)).isEqualTo(1);
    }
    @Test void concurrentCreationWithOneKeyHasOneEffectAndConcurrentQualificationHasOneWinner() throws Exception {
        String csrf=a.web.csrf(),key=key(); var start=new CountDownLatch(1);
        UUID ticket;
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<JsonNode> command=()->{ start.await(); return body(a.web.sendWithCsrf("POST",ct(siteA),ticketBody(null),null,key,csrf),201); };
            var one=pool.submit(command); var two=pool.submit(command); start.countDown(); var first=one.get(); assertThat(two.get()).isEqualTo(first); ticket=id(first,"resourceId");
        }
        String adminCsrf=admin.csrf(); var startQualification=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> command=()->{ startQualification.await(); return admin.sendWithCsrf("POST",at(siteA)+"/"+ticket+"/qualify",Map.of("retainedPriority","NORMAL","reason","Diagnostic concurrent"),0L,key(),adminCsrf).statusCode(); };
            var one=pool.submit(command); var two=pool.submit(command); startQualification.countDown(); assertThat(List.of(one.get(),two.get())).containsExactlyInAnyOrder(200,412);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.ticket_change WHERE ticket_id=?",Integer.class,ticket)).isEqualTo(2);
    }
    @Test void concurrentProposalAndQualificationCannotSilentlyOverwriteTheVersionSeenByEitherActor() throws Exception {
        UUID ticket=ticket(null); String clientCsrf=a.web.csrf(),adminCsrf=admin.csrf(); var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var proposal=pool.submit(()->{ start.await(); return a.web.sendWithCsrf("POST",ct(siteA)+"/"+ticket+"/proposals",Map.of("remuneration","150.00"),0L,key(),clientCsrf).statusCode(); });
            var qualification=pool.submit(()->{ start.await(); return admin.sendWithCsrf("POST",at(siteA)+"/"+ticket+"/qualify",Map.of("retainedPriority","NORMAL","reason","Diagnostic"),0L,key(),adminCsrf).statusCode(); });
            start.countDown(); int proposed=proposal.get(),qualified=qualification.get(); assertThat(List.of(proposed,qualified)).containsExactlyInAnyOrder(200,412);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_financial_proposal WHERE ticket_id=?",Integer.class,ticket)).isEqualTo(proposed==200?2:1);
            assertThat(jdbc.queryForObject("SELECT state FROM app.ticket WHERE ticket_id=?",String.class,ticket)).isEqualTo(qualified==200?"QUALIFIED":"OPEN");
        }
    }
    @Test void concurrentContractRevisionsKeepExactlyOneNewImmutableDefinition() throws Exception {
        var original=body(admin.post(contracts(accountA),contractBody(List.of(coverage(siteA,true,List.of())))),201); UUID contract=id(original,"contractId");
        String csrf=admin.csrf(); var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> command=()->{ start.await(); return admin.sendWithCsrf("POST",contracts(accountA)+"/"+contract+"/revisions",contractBody(List.of(coverage(siteA,true,List.of()))),0L,null,csrf).statusCode(); };
            var one=pool.submit(command); var two=pool.submit(command); start.countDown(); assertThat(List.of(one.get(),two.get())).containsExactlyInAnyOrder(201,412);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.contract_revision WHERE contract_id=?",Integer.class,contract)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.business_event_outbox WHERE resource_id=? AND event_kind='CONTRACT_REVISED'",Integer.class,contract)).isEqualTo(2);
    }
    @Test void everyAdministrativeAndClientMutationRefusesWrongRolesPendingAndPreMfa() throws Exception {
        UUID ticket=ticket(null); Browser preMfa=login(adminEmail);
        String email="ticket-tech-"+UUID.randomUUID()+"@example.invalid"; UUID tech=provision.createPendingTechnician(email,password); verify(tech); jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?",tech); Browser technician=login(email);
        jdbc.update("UPDATE app.app_user SET status='PENDING' WHERE user_id=?",b.user);
        var contract=body(admin.post(contracts(accountA),contractBody(List.of(coverage(siteA,true,List.of())))),201);
        List<String> administrative=List.of(contracts(accountA),contracts(accountA)+"/"+id(contract,"contractId")+"/revisions",at(siteA)+"/"+ticket+"/qualify",at(siteA)+"/"+ticket+"/start",at(siteA)+"/"+ticket+"/wait-client",at(siteA)+"/"+ticket+"/resume",at(siteA)+"/"+ticket+"/resolve",at(siteA)+"/"+ticket+"/cancel",at(siteA)+"/"+ticket+"/messages",at(siteA)+"/"+ticket+"/internal-notes");
        for(String path:administrative) { for(Browser denied:List.of(a.web,b.web,technician,preMfa)) { assertThat(denied.send("POST",path,Map.of(),0L,key(),true).statusCode()).isEqualTo(403); } }
        for(String path:List.of(ct(siteA),ct(siteA)+"/"+ticket+"/proposals",ct(siteA)+"/"+ticket+"/confirm-resolution",ct(siteA)+"/"+ticket+"/report-persistence",ct(siteA)+"/"+ticket+"/messages")) {
            assertThat(technician.send("POST",path,Map.of(),0L,key(),true).statusCode()).isEqualTo(403); assertThat(b.web.send("POST",path,Map.of(),0L,key(),true).statusCode()).isEqualTo(403);
        }
        assertThat(jdbc.queryForObject("SELECT version FROM app.ticket WHERE ticket_id=?",Long.class,ticket)).isZero();
    }
    @Test void csrfBodyLimitsDtosAndTerminalStatesDoNotAllowMassAssignmentOrBypass() throws Exception {
        var input=ticketBody(null); input.put("assignedTechnicianId",b.user); input.put("publishedRemuneration","10.00"); input.put("state","CLOSED");
        assertThat(a.web.post(ct(siteA),input).statusCode()).isEqualTo(400);
        var attachment=ticketBody(null); attachment.put("attachmentUrl","http://127.0.0.1/private"); assertThat(a.web.post(ct(siteA),attachment).statusCode()).isEqualTo(400);
        assertThat(a.web.send("POST",ct(siteA),ticketBody(null),null,key(),false).statusCode()).isEqualTo(403);
        var tooLarge=ticketBody(null); tooLarge.put("description","x".repeat(17000)); assertThat(a.web.post(ct(siteA),tooLarge).statusCode()).isEqualTo(413);
        UUID ticket=ticket(null); assertThat(admin.send("POST",at(siteA)+"/"+ticket+"/qualify",Map.of("retainedPriority","NORMAL","reason","Diagnostic"),0L,key(),false).statusCode()).isEqualTo(403);
        for(String query:List.of("size=101","page=-1","page=10001","state=UNKNOWN","requestedPriority=99")) { assertThat(a.web.get(ct(siteA)+"?"+query).statusCode()).isEqualTo(400); }
        var numeric=ticketBody(null); numeric.put("requestedPriority",1); assertThat(a.web.post(ct(siteA),numeric).statusCode()).isEqualTo(400);
        assertThat(a.web.get(ct(siteA)).headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    }
    @Test void postgresEnforcesMoneyScaleCompositeRelationsAndImmutableRuntimePrivileges() throws Exception {
        UUID ticket=ticket(equipmentA); var contract=body(admin.post(contracts(accountA),contractBody(List.of(coverage(siteA,false,List.of(equipmentA))))),201); UUID revision=id(contract,"revisionId");
        for(String amount:List.of("-1.00","1.234","10000000000.00","'NaN'::numeric")) { assertThatThrownBy(()->jdbc.update("INSERT INTO app.client_financial_proposal(account_id,ticket_id,proposal_number,remuneration,author_user_id,created_at) VALUES (?,?,9,"+amount+",?,now())",accountA,ticket,a.user)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class); }
        assertIncoherentCoverageRejected(false); assertIncoherentCoverageRejected(true);
        for(String sql:List.of("UPDATE app.contract_revision SET public_name='Changed'","DELETE FROM app.client_financial_proposal","UPDATE app.ticket SET site_id=site_id","UPDATE app.ticket_change SET action='CANCEL'","DELETE FROM app.ticket_internal_note","TRUNCATE app.business_event_outbox")) {
            try(var c=PostgisFixture.runtime(DATABASE);var st=c.createStatement()) { assertThatThrownBy(()->st.execute(sql)).isInstanceOf(SQLException.class).satisfies(f->assertThat(((SQLException)f).getSQLState()).isEqualTo("42501")); }
        }
        assertThatThrownBy(()->jdbc.update("INSERT INTO app.contract_day_override(account_id,revision_id,service_date) VALUES (?,?,date '2027-01-01')",accountA,revision)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO app.contract_sla_rule(account_id,revision_id,priority,metric,target_minutes,pause_while_waiting_client) VALUES (?,?,'NORMAL','RESOLUTION',120,false)",accountA,revision)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void auditOrOutboxFailureRollsBackTicketProposalHistoryAndReceiptTogether(CapturedOutput output) throws Exception {
        for(String table:List.of("audit_event","business_event_outbox")) {
            try(var c=PostgisFixture.migrator(DATABASE);var s=c.createStatement()) {
                s.execute("REVOKE INSERT ON app."+table+" FROM maint_app");
                try {
                    assertThat(a.web.post(ct(siteA),ticketBody(null)).statusCode()).isEqualTo(500);
                    assertThat(admin.post(contracts(accountA),contractBody(List.of(coverage(siteA,true,List.of())))).statusCode()).isEqualTo(500);
                }
                finally { s.execute("GRANT INSERT ON app."+table+" TO maint_app"); }
            }
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.ticket WHERE account_id=?",Integer.class,accountA)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.client_financial_proposal WHERE account_id=?",Integer.class,accountA)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.command_receipt WHERE actor_user_id=?",Integer.class,a.user)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.maintenance_contract WHERE account_id=?",Integer.class,accountA)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app.contract_revision WHERE account_id=?",Integer.class,accountA)).isZero();
        assertThat(output.toString()).doesNotContain(password,"Description privée synthétique","Note contractuelle interne");
    }
    @Test void archivedSitesSuspensionAndStaleAdministrativeProofCloseAllNewAccessPaths() throws Exception {
        UUID ticket=ticket(null); body(admin.send("POST",adminAccount(accountA)+"/sites/"+siteA+"/archive",Map.of(),0L,null,true),200);
        assertThat(a.web.get(ct(siteA)+"/"+ticket).statusCode()).isEqualTo(404); assertThat(a.web.get(coverageRoot(accountA,siteA)).statusCode()).isEqualTo(404);
        jdbc.update("UPDATE app.app_user SET status='SUSPENDED' WHERE user_id=?",a.user); assertThat(a.web.get(ct(siteA)).statusCode()).isEqualTo(401);
        clock.advance(301); assertThat(admin.get(contracts(accountA)).statusCode()).isEqualTo(403);
    }
    private Client seed() throws Exception {
        String email="ticket-client-"+UUID.randomUUID()+"@example.invalid"; UUID user=provision.createPendingClient(email,password); verify(user); jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?",user);
        UUID invitation=UUID.randomUUID(),dossier=UUID.randomUUID(); Timestamp now=Timestamp.from(clock.instant());
        jdbc.update("INSERT INTO app.client_invitation(invitation_id,email_normalized,email_display,invited_by,last_sent_by,accepted_user_id,created_at,expires_at,accepted_at) VALUES (?,?,?,?,?,?,?,?,?)",invitation,email,email,adminId,adminId,user,now,Timestamp.from(clock.instant().plusSeconds(172800)),now);
        jdbc.update("INSERT INTO app.client_onboarding(dossier_id,user_id,invitation_id,state,account_type,first_name,last_name,phone,address,organization_name,created_at,updated_at) VALUES (?,?,?,'APPROVED','BUSINESS','Client','Synthétique','+33 100000000','Adresse synthétique','Organisation synthétique',?,?)",dossier,user,invitation,now,now);
        return new Client(user,dossier,login(email));
    }
    private void assertIncoherentCoverageRejected(boolean equipment) throws Exception {
        // Fresh revision and children in one real SQL transaction: V16's freeze is not the reason
        // for this failure. The expected 23503 demonstrates the actual composite foreign key.
        try(var c=PostgisFixture.runtime(DATABASE)) {
            c.setAutoCommit(false); UUID contract=UUID.randomUUID(),revision=UUID.randomUUID();
            try {
                try(var s=c.prepareStatement("INSERT INTO app.maintenance_contract(contract_id,account_id,created_at,updated_at) VALUES (?,?,now(),now())")) { s.setObject(1,contract); s.setObject(2,accountA); s.executeUpdate(); }
                try(var s=c.prepareStatement("INSERT INTO app.contract_revision(revision_id,account_id,contract_id,revision_number,public_name,valid_from,valid_until,calendar_mode,reason,author_user_id,created_at) VALUES (?,?,?,0,'Test',now(),now()+interval '1 day','ALWAYS','Test',?,now())")) { s.setObject(1,revision); s.setObject(2,accountA); s.setObject(3,contract); s.setObject(4,adminId); s.executeUpdate(); }
                if(equipment) {
                    try(var s=c.prepareStatement("INSERT INTO app.contract_site_coverage(account_id,revision_id,site_id,coverage_mode) VALUES (?,?,?,'EQUIPMENT')")) { s.setObject(1,accountA); s.setObject(2,revision); s.setObject(3,hiddenA); s.executeUpdate(); }
                    try(var s=c.prepareStatement("INSERT INTO app.contract_equipment_coverage(account_id,revision_id,site_id,equipment_id) VALUES (?,?,?,?)")) { s.setObject(1,accountA); s.setObject(2,revision); s.setObject(3,hiddenA); s.setObject(4,equipmentA); assertThatThrownBy(s::executeUpdate).isInstanceOf(SQLException.class).satisfies(f->assertThat(((SQLException)f).getSQLState()).isEqualTo("23503")); }
                } else {
                    try(var s=c.prepareStatement("INSERT INTO app.contract_site_coverage(account_id,revision_id,site_id,coverage_mode) VALUES (?,?,?,'SITE')")) { s.setObject(1,accountA); s.setObject(2,revision); s.setObject(3,siteB); assertThatThrownBy(s::executeUpdate).isInstanceOf(SQLException.class).satisfies(f->assertThat(((SQLException)f).getSQLState()).isEqualTo("23503")); }
                }
            } finally { c.rollback(); }
        }
    }
    private void verify(UUID user) { jdbc.update("UPDATE app.local_credential SET email_verified_at=? WHERE user_id=?",Timestamp.from(clock.instant()),user); }
    private UUID account(Client c) throws Exception { return id(body(admin.post("/api/web/v1/admin/client-accounts",Map.of("dossierId",c.dossier)),201),"accountId"); }
    private UUID site(UUID account,String name) throws Exception { return id(body(admin.post(adminAccount(account)+"/sites",Map.of("name",name,"address","Adresse synthétique","timeZone","Europe/Paris")),201),"siteId"); }
    private UUID equipment(UUID account,UUID site) throws Exception { return id(body(admin.post(adminAccount(account)+"/sites/"+site+"/equipment",Map.of("label","Poste synthétique","category","PC")),201),"equipmentId"); }
    private void grant(UUID account,UUID user,List<UUID> sites,long version) throws Exception { body(admin.send("PUT",adminAccount(account)+"/members/"+user+"/site-access",Map.of("allSites",false,"siteIds",sites),version,null,true),200); }
    private LinkedHashMap<String,Object> ticketBody(UUID equipment) { var input=new LinkedHashMap<String,Object>(); input.put("equipmentId",equipment); input.put("title","Incident synthétique"); input.put("description","Description privée synthétique"); input.put("requestedPriority","HIGH"); return input; }
    private UUID ticket(UUID equipment) throws Exception { return id(body(a.web.post(ct(siteA),ticketBody(equipment)),201),"resourceId"); }
    private Map<String,Object> coverage(UUID site,boolean whole,List<UUID> equipment) { return Map.of("siteId",site,"wholeSite",whole,"equipmentIds",equipment); }
    private LinkedHashMap<String,Object> contractBody(List<Map<String,Object>> coverage) {
        var input=new LinkedHashMap<String,Object>(); input.put("publicName","Contrat public"); input.put("internalNotes","Note contractuelle interne"); input.put("reason","Motif interne de révision");
        input.put("validFrom",clock.instant().minusSeconds(3600).toString()); input.put("validUntil",clock.instant().plusSeconds(86400).toString()); input.put("coverage",coverage);
        input.put("calendar",Map.of("mode","ALWAYS","weekly",List.of(),"overrides",List.of())); input.put("slaRules",List.of()); return input;
    }
    private void qualify(UUID ticket,UUID revision,long version) throws Exception { var input=new LinkedHashMap<String,Object>(); input.put("retainedPriority","NORMAL"); input.put("contractRevisionId",revision); input.put("reason","Diagnostic synthétique"); body(admin.send("POST",at(siteA)+"/"+ticket+"/qualify",input,version,key(),true),200); }
    private void transition(Browser browser,String root,UUID ticket,String action,long version) throws Exception { body(browser.send("POST",root+"/"+ticket+"/"+action,Map.of("reason","Compte rendu public synthétique"),version,key(),true),200); }
    private int count(String table,UUID ticket) { if(!table.equals("ticket")) { throw new IllegalArgumentException(); } return jdbc.queryForObject("SELECT count(*) FROM app.ticket WHERE ticket_id=?",Integer.class,ticket); }
    private String adminAccount(UUID account) { return "/api/web/v1/admin/client-accounts/"+account; }
    private String contracts(UUID account) { return adminAccount(account)+"/contracts"; }
    private String ct(UUID site) { return clientRoot(accountA,site); }
    private String at(UUID site) { return adminAccount(accountA)+"/sites/"+site+"/tickets"; }
    private String clientRoot(UUID account,UUID site) { return "/api/web/v1/client/accounts/"+account+"/sites/"+site+"/tickets"; }
    private String coverageRoot(UUID account,UUID site) { return "/api/web/v1/client/accounts/"+account+"/sites/"+site+"/contract-revisions"; }
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
