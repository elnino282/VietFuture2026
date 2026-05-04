package org.example.QuanLyMuaVu.Architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class PriorityModuleBoundaryTest {

    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("org.example.QuanLyMuaVu.module");

    @Test
    void aiModule_shouldRemainIsolatedFromOtherBusinessModules() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..module.ai..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..module.identity.entity..",
                        "..module.identity.repository..",
                        "..module.farm.entity..",
                        "..module.farm.repository..",
                        "..module.cropcatalog.entity..",
                        "..module.cropcatalog.repository..",
                        "..module.season.entity..",
                        "..module.season.repository..",
                        "..module.financial.entity..",
                        "..module.financial.repository..",
                        "..module.inventory.entity..",
                        "..module.inventory.repository..",
                        "..module.incident.entity..",
                        "..module.incident.repository..",
                        "..module.sustainability.entity..",
                        "..module.sustainability.repository..",
                        "..module.admin.entity..",
                        "..module.admin.repository..");
        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void cropCatalogModule_shouldRemainIsolatedFromOtherBusinessModules() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..module.cropcatalog..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..module.identity.entity..",
                        "..module.identity.repository..",
                        "..module.farm.entity..",
                        "..module.farm.repository..",
                        "..module.ai.entity..",
                        "..module.ai.repository..",
                        "..module.season.entity..",
                        "..module.season.repository..",
                        "..module.financial.entity..",
                        "..module.financial.repository..",
                        "..module.inventory.entity..",
                        "..module.inventory.repository..",
                        "..module.incident.entity..",
                        "..module.incident.repository..",
                        "..module.sustainability.entity..",
                        "..module.sustainability.repository..",
                        "..module.admin.entity..",
                        "..module.admin.repository..");
        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void inventoryModule_shouldNotDependOnOtherModuleRepositoryPackages() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..module.inventory..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..module.identity.repository..",
                        "..module.farm.repository..",
                        "..module.cropcatalog.repository..",
                        "..module.season.repository..",
                        "..module.financial.repository..",
                        "..module.incident.repository..",
                        "..module.sustainability.repository..",
                        "..module.admin.repository..");
        rule.check(IMPORTED_CLASSES);
    }
}
