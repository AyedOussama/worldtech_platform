package fr.entreprise.maintenance.features.identity.application;

import java.time.Clock;
import fr.entreprise.maintenance.features.identity.domain.AccountStatus;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityRole;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.identity.publicapi.ActiveTechnician;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import org.springframework.stereotype.Service;

@Service
public class ActiveTechnicianService implements ActiveTechnician {
    private final IdentityStore identities;
    private final Clock clock;
    public ActiveTechnicianService(IdentityStore identities, Clock clock) { this.identities=identities; this.clock=clock; }
    @Override public void require(IdentitySession session) {
        if(!(session instanceof LoginIdentity login)) { throw refused(); }
        var account=identities.current(login,"MOBILE",clock.instant()).orElseThrow(ActiveTechnicianService::refused).account();
        if(account.status()!=AccountStatus.ACTIVE || !account.emailVerified() || account.roles().size()!=1
                || !account.roles().contains(IdentityRole.TECHNICIAN)) { throw refused(); }
    }
    private static IdentityFailure refused() { return new IdentityFailure(IdentityFailure.Reason.FORBIDDEN); }
}
