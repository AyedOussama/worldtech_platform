package fr.entreprise.maintenance.features.clients.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.clients.domain.AccountFailure;
import fr.entreprise.maintenance.features.clients.domain.ClientAccount;
import fr.entreprise.maintenance.features.clients.domain.ClientAccounts;
import fr.entreprise.maintenance.features.clients.publicapi.ClientAccountAccess;
import fr.entreprise.maintenance.features.identity.publicapi.BusinessIdentity;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientAccountService implements ClientAccountAccess {
    public record AccountsPage(List<ClientAccount> items,long total,int page,int size) { }
    public record MembersPage(List<ClientAccounts.Member> items,long total,int page,int size) { }
    private final ClientAccounts store;
    private final BusinessIdentity identity;
    private final Clock clock;
    public ClientAccountService(ClientAccounts store,BusinessIdentity identity,Clock clock) { this.store=store; this.identity=identity; this.clock=clock; }
    public ClientAccounts.Created create(IdentitySession actor,UUID dossier) {
        identity.checkAdministrator(actor); var source=store.approvedSource(dossier);
        identity.authorize(actor,true,source.userId());
        return store.create(dossier,source,actor.userId(),clock.instant());
    }
    @Override public Scope authorize(IdentitySession actor,UUID accountId,boolean administratorOnly) {
        var current=identity.authorize(actor,administratorOnly,null); store.scoped(accountId,current.userId(),current.administrator());
        UUID member=current.administrator() ? null : store.currentMember(accountId,current.userId()).memberId();
        return new Scope(accountId,current.userId(),member,current.administrator());
    }
    @Override public UUID currentMember(Scope scope,UUID userId) {
        if(!scope.administrator()) { throw new AccountFailure(AccountFailure.Code.ACCOUNT_NOT_FOUND); }
        return store.currentMember(scope.accountId(),userId).memberId();
    }
    public AccountsPage list(IdentitySession actor,boolean admin,int page,int size) {
        bounds(page,size); var current=identity.authorize(actor,admin,null);
        return new AccountsPage(store.list(current.userId(),current.administrator(),page*size,size),store.count(current.userId(),current.administrator()),page,size);
    }
    public ClientAccount detail(IdentitySession actor,UUID id,boolean admin) { authorize(actor,id,admin); return store.lock(id); }
    public ClientAccount rename(IdentitySession actor,UUID id,String name,long expected) {
        authorize(actor,id,true); var account=store.lock(id); account.expect(expected);
        return store.rename(account,ClientAccount.name(name),actor.userId(),clock.instant());
    }
    public MembersPage members(IdentitySession actor,UUID id,int page,int size) {
        bounds(page,size); authorize(actor,id,true);
        return new MembersPage(store.members(id,page*size,size),store.memberCount(id),page,size);
    }
    public ClientAccounts.Member member(IdentitySession actor,UUID id,UUID member) { authorize(actor,id,true); return store.member(id,member); }
    public ClientAccounts.Member add(IdentitySession actor,UUID id,UUID user) {
        identity.authorize(actor,true,user); var account=store.lock(id); account.allowMember(user);
        if(!store.approved(user)) { throw new AccountFailure(AccountFailure.Code.CLIENT_NOT_APPROVED); }
        return store.add(account,user,actor.userId(),clock.instant());
    }
    public ClientAccounts.Member remove(IdentitySession actor,UUID id,UUID member,long expected) {
        authorize(actor,id,true); return store.remove(id,member,expected,actor.userId(),clock.instant());
    }
    private static void bounds(int page,int size) { if(page<0 || page>10000 || size<1 || size>100) { throw new AccountFailure(AccountFailure.Code.INVALID_ACCOUNT_INPUT); } }
}
