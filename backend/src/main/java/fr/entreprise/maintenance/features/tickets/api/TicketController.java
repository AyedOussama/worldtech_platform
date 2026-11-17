package fr.entreprise.maintenance.features.tickets.api;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.tickets.application.TicketService;
import fr.entreprise.maintenance.features.tickets.domain.ClientProposal;
import fr.entreprise.maintenance.features.tickets.domain.Ticket;
import fr.entreprise.maintenance.features.tickets.domain.TicketFailure;
import fr.entreprise.maintenance.shared.idempotency.CommandReceipts;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {
    public static final String ADMIN="/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/tickets";
    public static final String CLIENT="/api/web/v1/client/accounts/{accountId}/sites/{siteId}/tickets";
    private final TicketService tickets;
    public TicketController(TicketService tickets) { this.tickets=tickets; }
    @PostMapping(CLIENT)
    public ResponseEntity<CommandView> create(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateBody body) {
        var r=tickets.create(actor,accountId,siteId,body.equipmentId(),body.title(),body.description(),body.requestedPriority(),body.proposal()==null?ClientProposal.parse(null,null):body.proposal().proposal(),key);
        return ResponseEntity.created(URI.create("/api/web/v1/client/accounts/"+accountId+"/sites/"+siteId+"/tickets/"+r.resourceId())).cacheControl(CacheControl.noStore()).eTag(tag(r.version())).body(command(r));
    }
    @GetMapping({ADMIN,CLIENT})
    public ResponseEntity<TicketPage> list(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@RequestParam(required=false) Ticket.State state,@RequestParam(required=false) Ticket.Priority requestedPriority,@RequestParam(required=false) UUID equipmentId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        var p=tickets.list(actor,accountId,siteId,admin(request),state,requestedPriority,equipmentId,page,size); return noStore(new TicketPage(p.items().stream().map(TicketController::summary).toList(),p.total(),p.page(),p.size()));
    }
    @GetMapping({ADMIN+"/{ticketId}",CLIENT+"/{ticketId}"})
    public ResponseEntity<TicketView> detail(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,HttpServletRequest request) {
        var t=tickets.detail(actor,accountId,siteId,ticketId,admin(request)); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(t.version())).body(new TicketView(summary(t),t.description(),t.siteTimeZone(),t.contractRevisionId(),!t.qualified()?"UNQUALIFIED":t.contractRevisionId()==null?"OUT_OF_CONTRACT":"COVERED"));
    }
    @PostMapping(CLIENT+"/{ticketId}/proposals")
    public ResponseEntity<CommandView> propose(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ProposalBody body) { return response(tickets.propose(actor,accountId,siteId,ticketId,version(expected),body.proposal(),key)); }
    @GetMapping({ADMIN+"/{ticketId}/proposals",CLIENT+"/{ticketId}/proposals"})
    public ResponseEntity<ProposalHistory> proposals(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        var items=tickets.proposals(actor,accountId,siteId,ticketId,admin(request),after,size); return noStore(new ProposalHistory(items.stream().map(p->new ProposalView(p.number(),amount(p.amounts().remuneration()),amount(p.amounts().expenses()),"EUR","HT","FIXED","FIXED",p.authorUserId(),p.createdAt())).toList(),items.isEmpty()?after:items.getLast().number()));
    }
    @GetMapping({ADMIN+"/{ticketId}/history",CLIENT+"/{ticketId}/history"})
    public ResponseEntity<History> history(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        var items=tickets.history(actor,accountId,siteId,ticketId,admin(request),after,size); return noStore(new History(items.stream().map(c->new ChangeView(c.version(),c.action(),c.state(),c.actorUserId(),c.publicReason(),c.occurredAt())).toList(),items.isEmpty()?after:items.getLast().version()));
    }
    @PostMapping(ADMIN+"/{ticketId}/qualify")
    public ResponseEntity<CommandView> qualify(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody QualifyBody body) { return response(tickets.transition(actor,accountId,siteId,ticketId,version(expected),Ticket.Action.QUALIFY,body.retainedPriority(),body.contractRevisionId(),body.reason(),key)); }
    @PostMapping(ADMIN+"/{ticketId}/start")
    public ResponseEntity<CommandView> start(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonBody body) { return transition(actor,accountId,siteId,ticketId,expected,key,body,Ticket.Action.START); }
    @PostMapping(ADMIN+"/{ticketId}/wait-client")
    public ResponseEntity<CommandView> waitClient(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonBody body) { return transition(actor,accountId,siteId,ticketId,expected,key,body,Ticket.Action.WAIT); }
    @PostMapping(ADMIN+"/{ticketId}/resume")
    public ResponseEntity<CommandView> resume(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonBody body) { return transition(actor,accountId,siteId,ticketId,expected,key,body,Ticket.Action.RESUME); }
    @PostMapping(ADMIN+"/{ticketId}/resolve")
    public ResponseEntity<CommandView> resolve(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonBody body) { return transition(actor,accountId,siteId,ticketId,expected,key,body,Ticket.Action.RESOLVE); }
    @PostMapping(ADMIN+"/{ticketId}/cancel")
    public ResponseEntity<CommandView> cancel(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonBody body) { return transition(actor,accountId,siteId,ticketId,expected,key,body,Ticket.Action.CANCEL); }
    @PostMapping(CLIENT+"/{ticketId}/confirm-resolution")
    public ResponseEntity<CommandView> confirm(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonBody body) { return transition(actor,accountId,siteId,ticketId,expected,key,body,Ticket.Action.CLOSE); }
    @PostMapping(CLIENT+"/{ticketId}/report-persistence")
    public ResponseEntity<CommandView> persist(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonBody body) { return transition(actor,accountId,siteId,ticketId,expected,key,body,Ticket.Action.REOPEN); }
    @GetMapping({ADMIN+"/{ticketId}/messages",CLIENT+"/{ticketId}/messages"})
    public ResponseEntity<MessageHistory> messages(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size,HttpServletRequest request) { return messages(actor,accountId,siteId,ticketId,admin(request),false,after,size); }
    @GetMapping(ADMIN+"/{ticketId}/internal-notes")
    public ResponseEntity<MessageHistory> notes(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size) { return messages(actor,accountId,siteId,ticketId,true,true,after,size); }
    @PostMapping({ADMIN+"/{ticketId}/messages",CLIENT+"/{ticketId}/messages"})
    public ResponseEntity<CommandView> message(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody MessageBody body,HttpServletRequest request) { return response(tickets.message(actor,accountId,siteId,ticketId,admin(request),false,version(expected),body.text(),key)); }
    @PostMapping(ADMIN+"/{ticketId}/internal-notes")
    public ResponseEntity<CommandView> note(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID ticketId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody MessageBody body) { return response(tickets.message(actor,accountId,siteId,ticketId,true,true,version(expected),body.text(),key)); }
    private ResponseEntity<MessageHistory> messages(IdentitySession actor,UUID account,UUID site,UUID ticket,boolean admin,boolean internal,long after,int size) {
        var items=tickets.messages(actor,account,site,ticket,admin,internal,after,size); return noStore(new MessageHistory(items.stream().map(m->new MessageView(m.number(),m.messageId(),m.authorUserId(),m.text(),m.createdAt())).toList(),items.isEmpty()?after:items.getLast().number()));
    }
    private ResponseEntity<CommandView> transition(IdentitySession actor,UUID account,UUID site,UUID ticket,String expected,String key,ReasonBody body,Ticket.Action action) { return response(tickets.transition(actor,account,site,ticket,version(expected),action,null,null,body.reason(),key)); }
    private static <T> ResponseEntity<T> noStore(T value) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value); }
    private static ResponseEntity<CommandView> response(CommandReceipts.ResourceResult r) { return noStore(command(r)); }
    private static CommandView command(CommandReceipts.ResourceResult r) { return new CommandView(r.resourceId(),r.version(),r.state()); }
    private static String tag(long v) { return "\""+v+"\""; }
    private static long version(String input) { if(!input.matches("\"[0-9]{1,18}\"")) { throw TicketFailure.invalid(); } return Long.parseLong(input.substring(1,input.length()-1)); }
    private static boolean admin(HttpServletRequest request) { return request.getRequestURI().startsWith("/api/web/v1/admin/"); }
    private static String amount(java.math.BigDecimal value) { return value==null?null:value.toPlainString(); }
    private static SummaryView summary(Ticket t) { return new SummaryView(t.ticketId(),t.siteId(),t.equipmentId(),t.title(),t.requestedPriority(),t.retainedPriority(),t.state(),t.proposalNumber(),t.version(),t.createdAt()); }
    public record ProposalBody(Object remuneration,Object expenses) {
        ClientProposal proposal() { return ClientProposal.parse(string(remuneration),string(expenses)); }
        private static String string(Object value) { if(value==null) { return null; } if(value instanceof String text) { return text; } throw TicketFailure.invalid(); }
        @Override public String toString() { return "ProposalBody[redacted]"; }
    }
    public record CreateBody(UUID equipmentId,@NotNull @Size(max=160) String title,@NotNull @Size(max=4000) String description,@NotNull Ticket.Priority requestedPriority,@Valid ProposalBody proposal) { @Override public String toString() { return "CreateBody[redacted]"; } }
    public record QualifyBody(@NotNull Ticket.Priority retainedPriority,UUID contractRevisionId,@NotNull @Size(max=2000) String reason) { @Override public String toString() { return "QualifyBody[redacted]"; } }
    public record ReasonBody(@NotNull @Size(max=2000) String reason) { @Override public String toString() { return "ReasonBody[redacted]"; } }
    public record MessageBody(@NotNull @Size(max=4000) String text) { @Override public String toString() { return "MessageBody[redacted]"; } }
    public record CommandView(UUID resourceId,long version,String state) { }
    public record SummaryView(UUID ticketId,UUID siteId,UUID equipmentId,String title,Ticket.Priority requestedPriority,Ticket.Priority retainedPriority,Ticket.State state,long proposalNumber,long version,Instant createdAt) { }
    public record TicketView(SummaryView ticket,String description,String siteTimeZone,UUID contractRevisionId,String coverage) { }
    public record TicketPage(List<SummaryView> items,long total,int page,int size) { }
    public record ProposalView(long number,String remuneration,String expenses,String currency,String taxBasis,String remunerationBasis,String expensesBasis,UUID authorUserId,Instant createdAt) { }
    public record ProposalHistory(List<ProposalView> items,long nextAfter) { }
    public record ChangeView(long version,String action,Ticket.State state,UUID actorUserId,String publicReason,Instant occurredAt) { }
    public record History(List<ChangeView> items,long nextAfter) { }
    public record MessageView(long number,UUID messageId,UUID authorUserId,String text,Instant createdAt) { }
    public record MessageHistory(List<MessageView> items,long nextAfter) { }
}
