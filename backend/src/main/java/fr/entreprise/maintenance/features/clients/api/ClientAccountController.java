package fr.entreprise.maintenance.features.clients.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.clients.application.ClientAccountService;
import fr.entreprise.maintenance.features.clients.domain.AccountFailure;
import fr.entreprise.maintenance.features.clients.domain.ClientAccount;
import fr.entreprise.maintenance.features.clients.domain.ClientAccounts;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientAccountController {
    private final ClientAccountService accounts;
    public ClientAccountController(ClientAccountService accounts) { this.accounts=accounts; }
    @PostMapping("/api/web/v1/admin/client-accounts")
    public ResponseEntity<AccountView> create(@AuthenticationPrincipal IdentitySession actor,@Valid @RequestBody CreateBody body) {
        var result=accounts.create(actor,body.dossierId());
        return ResponseEntity.status(result.created()?201:200).location(URI.create("/api/web/v1/admin/client-accounts/"+result.account().accountId())).cacheControl(CacheControl.noStore()).eTag(tag(result.account().version())).body(view(result.account()));
    }
    @GetMapping({"/api/web/v1/admin/client-accounts","/api/web/v1/client/accounts"})
    public ResponseEntity<AccountPage> list(@AuthenticationPrincipal IdentitySession actor,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        var p=accounts.list(actor,admin(request),page,size); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new AccountPage(p.items().stream().map(ClientAccountController::view).toList(),p.total(),p.page(),p.size()));
    }
    @GetMapping({"/api/web/v1/admin/client-accounts/{accountId}","/api/web/v1/client/accounts/{accountId}"})
    public ResponseEntity<AccountView> detail(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,HttpServletRequest request) { var a=accounts.detail(actor,accountId,admin(request)); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(a.version())).body(view(a)); }
    @PatchMapping("/api/web/v1/admin/client-accounts/{accountId}")
    public ResponseEntity<AccountView> rename(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@RequestHeader("If-Match") String expected,@Valid @RequestBody NameBody body) { var a=accounts.rename(actor,accountId,body.displayName(),version(expected)); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(a.version())).body(view(a)); }
    @GetMapping("/api/web/v1/admin/client-accounts/{accountId}/members")
    public ResponseEntity<MemberPage> members(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        var p=accounts.members(actor,accountId,page,size); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new MemberPage(p.items().stream().map(ClientAccountController::member).toList(),p.total(),p.page(),p.size()));
    }
    @PostMapping("/api/web/v1/admin/client-accounts/{accountId}/members")
    public ResponseEntity<MemberView> add(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@Valid @RequestBody MemberBody body) { var m=accounts.add(actor,accountId,body.userId()); return ResponseEntity.created(URI.create("/api/web/v1/admin/client-accounts/"+accountId+"/members/"+m.memberId())).cacheControl(CacheControl.noStore()).eTag(tag(m.version())).body(member(m)); }
    @GetMapping("/api/web/v1/admin/client-accounts/{accountId}/members/{memberId}")
    public ResponseEntity<MemberView> member(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID memberId) { var m=accounts.member(actor,accountId,memberId); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(m.version())).body(member(m)); }
    @PostMapping("/api/web/v1/admin/client-accounts/{accountId}/members/{memberId}/remove")
    public ResponseEntity<MemberView> remove(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID memberId,@RequestHeader("If-Match") String expected) { var m=accounts.remove(actor,accountId,memberId,version(expected)); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(m.version())).body(member(m)); }
    private static boolean admin(HttpServletRequest r) { return r.getRequestURI().startsWith("/api/web/v1/admin/"); }
    private static String tag(long version) { return "\""+version+"\""; }
    private static long version(String value) { if(!value.matches("\"[0-9]{1,18}\"")) { throw new AccountFailure(AccountFailure.Code.INVALID_ACCOUNT_INPUT); } return Long.parseLong(value.substring(1,value.length()-1)); }
    private static AccountView view(ClientAccount a) { return new AccountView(a.accountId(),a.accountType().name(),a.displayName(),a.version()); }
    private static MemberView member(ClientAccounts.Member m) { return new MemberView(m.memberId(),m.userId(),m.active(),m.version()); }
    public record CreateBody(@NotNull UUID dossierId) { }
    public record NameBody(@NotNull @Size(min=1,max=240) String displayName) { @Override public String toString() { return "NameBody[redacted]"; } }
    public record MemberBody(@NotNull UUID userId) { }
    public record AccountView(UUID accountId,String accountType,String displayName,long version) { @Override public String toString() { return "AccountView[redacted]"; } }
    public record MemberView(UUID memberId,UUID userId,boolean active,long version) { }
    public record AccountPage(List<AccountView> items,long total,int page,int size) { }
    public record MemberPage(List<MemberView> items,long total,int page,int size) { }
}
