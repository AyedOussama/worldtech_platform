package fr.entreprise.maintenance.features.onboarding;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.clients.domain.ClientDossier;
import fr.entreprise.maintenance.features.clients.domain.ClientFailure;
import fr.entreprise.maintenance.features.clients.domain.ClientProfile;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianDossier;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianFailure;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdmissionDomainTest {
    @Test void incompleteClientDraftIsAllowedButNotSubmittableAndBusinessNeedsAnOrganization() {
        var incomplete=new ClientProfile(ClientProfile.Type.BUSINESS,"Camille","Exemple","+33 100000000","Adresse synthétique",null);
        assertThatThrownBy(incomplete::requireComplete).isInstanceOf(ClientFailure.class);
        var individual=new ClientProfile(ClientProfile.Type.INDIVIDUAL,"Camille","Exemple","+33 100000000","Adresse synthétique",null);
        individual.requireComplete();
        assertThatThrownBy(() -> new ClientProfile(ClientProfile.Type.INDIVIDUAL,"Camille","Exemple","+33 100000000","Adresse synthétique","Entreprise"))
                .isInstanceOf(ClientFailure.class);
    }
    @ParameterizedTest @ValueSource(strings={"SUBMITTED","APPROVED","REJECTED"})
    void submittedAndTerminalProfilesCannotBeOverwritten(String state) {
        var d=new ClientDossier(UUID.randomUUID(),UUID.randomUUID(),ClientDossier.State.valueOf(state),new ClientProfile(null,null,null,null,null,null),7,Instant.EPOCH);
        assertThatThrownBy(d::editable).isInstanceOf(ClientFailure.class);
        assertThatThrownBy(() -> d.expect(6)).isInstanceOf(ClientFailure.class);
    }
    @ParameterizedTest @ValueSource(strings={"DRAFT","NEEDS_INFORMATION"})
    void completeProfilesCanBeSubmittedOnlyFromEditableStates(String state) {
        var profile=new ClientProfile(ClientProfile.Type.BUSINESS,"Camille","Exemple","+33 100000000","Adresse synthétique","Organisation");
        new ClientDossier(UUID.randomUUID(),UUID.randomUUID(),ClientDossier.State.valueOf(state),profile,0,Instant.EPOCH).submittable();
    }
    @Test void declaredTechnicianSkillsNeverSatisfyTheMandatoryEvidenceGate() {
        var profile=new TechnicianProfile("Alex","Exemple","+33 100000000","Paris",50,List.of("Réseaux"," Réseaux "));
        assertThat(profile.declaredSkills()).containsExactly("Réseaux");
        profile.requireComplete();
        var dossier=new TechnicianDossier(UUID.randomUUID(),UUID.randomUUID(),TechnicianDossier.State.SUBMITTED,profile,2,Instant.EPOCH);
        assertThatThrownBy(() -> dossier.decidable(TechnicianDossier.Decision.APPROVED)).isInstanceOf(TechnicianFailure.class)
                .satisfies(f -> assertThat(((TechnicianFailure)f).code()).isEqualTo(TechnicianFailure.Code.TECHNICIAN_DOCUMENTS_REQUIRED));
        dossier.decidable(TechnicianDossier.Decision.NEEDS_INFORMATION);
    }
    @ParameterizedTest @ValueSource(ints={0,9,201,1000})
    void mobilityIsBoundedWithoutInventingVerifiedSkillsOrLegalStatus(int radius) {
        assertThatThrownBy(() -> new TechnicianProfile("Alex","Exemple","+33 100000000","Paris",radius,List.of("Réseaux")))
                .isInstanceOf(TechnicianFailure.class);
    }
}
