package org.example.incident;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchitectureBoundaryTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.example.incident");

    @Test
    void serviceShouldRemainIsolatedFromMonolithAndOtherServices() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.example.QuanLyMuaVu..",
                        "org.example.identity..",
                        "org.example.farm..",
                        "org.example.cropcatalog..",
                        "org.example.season..",
                        "org.example.inventory..",
                        "org.example.finance..",
                        "org.example.sustainability..",
                        "org.example.marketplace..",
                        "org.example.reporting.."
                );
        rule.check(CLASSES);
    }

    @Test
    void layeredArchitectureCompliance() {
        ArchRule rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controller").definedBy("org.example.incident.controller..")
                .layer("Service").definedBy("org.example.incident.service..")
                .layer("Repository").definedBy("org.example.incident.repository..")
                
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

        rule.check(CLASSES);
    }

    @Test
    void repositoriesMustBeInterfacesAndEndWithRepository() {
        ArchRule rule = classes()
                .that().resideInAPackage("org.example.incident.repository..")
                .and().areNotMemberClasses()
                .should().beInterfaces()
                .andShould().haveSimpleNameEndingWith("Repository");
        rule.check(CLASSES);
    }
}
