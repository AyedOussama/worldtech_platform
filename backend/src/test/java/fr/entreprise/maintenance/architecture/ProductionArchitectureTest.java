package fr.entreprise.maintenance.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ProductionArchitectureTest {

    private static final String BASE = "fr.entreprise.maintenance";
    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages(BASE);

    @Test
    void productionClassesAreActuallyImportedAndOrganized() {
        assertThat(PRODUCTION).hasSizeGreaterThan(0);
        classes().that().resideOutsideOfPackage(BASE)
                .should().resideInAnyPackage(BASE + ".bootstrap..", BASE + ".shared..", BASE + ".features..")
                .check(PRODUCTION);
        classes().that().resideInAPackage(BASE)
                .should().haveSimpleName("MaintenanceApplication").check(PRODUCTION);
    }

    @Test
    void sharedCodeDoesNotDependOnFeaturesOrBootstrap() {
        noClasses().that().resideInAPackage(BASE + ".shared..")
                .should().dependOnClassesThat().resideInAnyPackage(BASE + ".features..", BASE + ".bootstrap..")
                .check(PRODUCTION);
        noClasses().that().resideInAPackage(BASE + ".features..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".bootstrap..")
                .allowEmptyShould(true).check(PRODUCTION);
    }

    @Test
    void domainsAndHttpContractsCannotDependOnPersistenceOrFrameworkDetails() {
        ArchitectureRules.independentDomain().check(PRODUCTION);
        ArchitectureRules.noPersistenceInHttpApi().check(PRODUCTION);
        noClasses().that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(PRODUCTION);
    }

    @Test
    void featuresMustUsePublicContractsAndBeAcyclic() {
        ArchitectureRules.publicModuleBoundaries(BASE).check(PRODUCTION);
        ArchitectureRules.acyclicFeatures(BASE).check(PRODUCTION);
    }
}
