package fr.entreprise.maintenance.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import fr.entreprise.architecturefixtures.features.clients.infrastructure.ClientRepository;
import fr.entreprise.architecturefixtures.features.clients.publicapi.ClientAccess;
import fr.entreprise.architecturefixtures.features.clients.publicapi.ClientCyclePort;
import fr.entreprise.architecturefixtures.features.tickets.api.InvalidTicketController;
import fr.entreprise.architecturefixtures.features.tickets.api.ValidTicketController;
import fr.entreprise.architecturefixtures.features.tickets.application.InvalidTicketUseCase;
import fr.entreprise.architecturefixtures.features.tickets.application.ValidTicketUseCase;
import fr.entreprise.architecturefixtures.features.tickets.domain.InvalidDomainValue;
import fr.entreprise.architecturefixtures.features.tickets.domain.ValidDomainValue;
import fr.entreprise.architecturefixtures.features.tickets.publicapi.TicketCyclePort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchitectureRulesTest {

    private static final String FIXTURES = "fr.entreprise.architecturefixtures";

    @Test
    void validDependencyExamplesAreAccepted() {
        var examples = new ClassFileImporter().importClasses(ValidDomainValue.class, ClientRepository.class,
                ClientAccess.class, ValidTicketUseCase.class, ValidTicketController.class);
        ArchitectureRules.independentDomain().check(examples);
        ArchitectureRules.noPersistenceInHttpApi().check(examples);
        ArchitectureRules.publicModuleBoundaries(FIXTURES).check(examples);
        ArchitectureRules.acyclicFeatures(FIXTURES).check(examples);
    }

    @Test
    void aFrameworkDependencyInsideDomainIsDetected() {
        var invalid = new ClassFileImporter().importClasses(InvalidDomainValue.class);
        assertThatThrownBy(() -> ArchitectureRules.independentDomain().check(invalid))
                .isInstanceOf(AssertionError.class).hasMessageContaining("org.springframework");
    }

    @Test
    void accessingAnotherModulesRepositoryIsDetected() {
        var invalid = new ClassFileImporter().importClasses(InvalidTicketUseCase.class, ClientRepository.class);
        assertThatThrownBy(() -> ArchitectureRules.publicModuleBoundaries(FIXTURES).check(invalid))
                .isInstanceOf(AssertionError.class).hasMessageContaining("ClientRepository");
    }

    @Test
    void persistenceTypeInGenericHttpReturnTypeIsDetected() {
        var invalid = new ClassFileImporter().importClasses(InvalidTicketController.class, ClientRepository.class);
        assertThatThrownBy(() -> ArchitectureRules.noPersistenceInHttpApi().check(invalid))
                .isInstanceOf(AssertionError.class).hasMessageContaining("ClientRepository");
    }

    @Test
    void cyclicPublicContractsAreDetectedEvenWithoutRepositoryAccess() {
        var invalid = new ClassFileImporter().importClasses(ClientCyclePort.class, TicketCyclePort.class);
        ArchitectureRules.publicModuleBoundaries(FIXTURES).check(invalid);
        assertThatThrownBy(() -> ArchitectureRules.acyclicFeatures(FIXTURES).check(invalid))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Cycle detected");
    }
}
