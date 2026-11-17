package fr.entreprise.maintenance.features.clients.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ClientAccounts {
    record Source(UUID userId, ClientAccount.Type accountType, String name) { @Override public String toString() { return "Source[redacted]"; } }
    record Member(UUID memberId, UUID userId, boolean active, long version) { }
    record Created(ClientAccount account, boolean created) { }
    Source approvedSource(UUID dossier);
    boolean approved(UUID user);
    Created create(UUID dossier, Source source, UUID actor, Instant now);
    ClientAccount lock(UUID account);
    ClientAccount scoped(UUID account, UUID user, boolean administrator);
    List<ClientAccount> list(UUID user, boolean administrator, int offset, int size);
    long count(UUID user, boolean administrator);
    ClientAccount rename(ClientAccount account, String name, UUID actor, Instant now);
    Member currentMember(UUID account, UUID user);
    Member member(UUID account, UUID memberId);
    List<Member> members(UUID account, int offset, int size);
    long memberCount(UUID account);
    Member add(ClientAccount account, UUID user, UUID actor, Instant now);
    Member remove(UUID account, UUID memberId, long version, UUID actor, Instant now);
}
