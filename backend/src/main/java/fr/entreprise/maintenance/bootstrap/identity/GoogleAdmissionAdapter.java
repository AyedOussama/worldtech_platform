package fr.entreprise.maintenance.bootstrap.identity;

import java.util.UUID;
import fr.entreprise.maintenance.features.clients.publicapi.GoogleClientAdmission;
import fr.entreprise.maintenance.features.identity.domain.GoogleAdmission;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.technicians.publicapi.GoogleTechnicianAdmission;
import org.springframework.stereotype.Component;

@Component
public class GoogleAdmissionAdapter implements GoogleAdmission {
    private final GoogleClientAdmission clients;
    private final GoogleTechnicianAdmission technicians;
    public GoogleAdmissionAdapter(GoogleClientAdmission clients,GoogleTechnicianAdmission technicians) { this.clients=clients; this.technicians=technicians; }
    @Override public void checkClientInvitation(String fingerprint,String email) {
        try { clients.checkInvitation(fingerprint,email); }
        catch(fr.entreprise.maintenance.features.clients.domain.ClientFailure failure) { throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
    }
    @Override public void client(UUID user,String fingerprint,String email) {
        try { clients.enroll(user,fingerprint,email); }
        catch(fr.entreprise.maintenance.features.clients.domain.ClientFailure failure) { throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
    }
    @Override public void lockClientInvitation(String fingerprint,String email) {
        try { clients.lockInvitation(fingerprint,email); }
        catch(fr.entreprise.maintenance.features.clients.domain.ClientFailure failure) { throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
    }
    @Override public void technician(UUID user) { technicians.enroll(user); }
}
