package fr.entreprise.maintenance.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

final class ArchitectureRules {

    private ArchitectureRules() {
    }

    static ArchRule independentDomain() {
        return noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "org.hibernate..", "java.sql..", "java.net.http..",
                        "tools.jackson..", "com.fasterxml.jackson..")
                // Lot 01 has no business domain. Positive/negative fixtures test this rule.
                .allowEmptyShould(true);
    }

    static ArchRule noPersistenceInHttpApi() {
        return noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .allowEmptyShould(true);
    }

    static ArchRule publicModuleBoundaries(String basePackage) {
        return classes().should(new ArchCondition<JavaClass>("use other domains only through publicapi") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceFeature = feature(source, basePackage);
                if (sourceFeature == null) {
                    return;
                }
                source.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    String targetFeature = feature(target, basePackage);
                    if (targetFeature != null && !sourceFeature.equals(targetFeature)) {
                        String publicApi = basePackage + ".features." + targetFeature + ".publicapi";
                        if (!target.getPackageName().equals(publicApi)
                                && !target.getPackageName().startsWith(publicApi + ".")) {
                            events.add(SimpleConditionEvent.violated(source, dependency.getDescription()));
                        }
                    }
                });
            }
        });
    }

    static ArchRule acyclicFeatures(String basePackage) {
        return slices().matching(basePackage + ".features.(*)..")
                .should().beFreeOfCycles().allowEmptyShould(true);
    }

    private static String feature(JavaClass type, String basePackage) {
        String prefix = basePackage + ".features.";
        if (!type.getPackageName().startsWith(prefix)) {
            return null;
        }
        return type.getPackageName().substring(prefix.length()).split("\\.", 2)[0];
    }
}
