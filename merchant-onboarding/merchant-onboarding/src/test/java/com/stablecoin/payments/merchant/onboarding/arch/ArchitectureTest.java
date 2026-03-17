package com.stablecoin.payments.merchant.onboarding.arch;

import com.stablecoin.payments.platform.test.HexagonalArchitectureRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Architecture Rules")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.stablecoin.payments.merchant.onboarding");
    }

    @Test
    @DisplayName("Domain should not depend on Spring (except stereotype and transaction)")
    void domainShouldNotDependOnSpring() {
        HexagonalArchitectureRules.domainShouldNotDependOnSpring().check(importedClasses);
    }

    @Test
    @DisplayName("Domain should not depend on infrastructure")
    void domainShouldNotDependOnInfrastructure() {
        HexagonalArchitectureRules.domainShouldNotDependOnInfrastructure().check(importedClasses);
    }

    @Test
    @DisplayName("Domain should not depend on application layer")
    void domainShouldNotDependOnApplication() {
        HexagonalArchitectureRules.domainShouldNotDependOnApplication().check(importedClasses);
    }

    @Test
    @DisplayName("Infrastructure should not depend on application controller")
    void infrastructureShouldNotDependOnController() {
        HexagonalArchitectureRules.infrastructureShouldNotDependOnController().check(importedClasses);
    }

    @Test
    @DisplayName("Ports should be interfaces")
    void portsShouldBeInterfaces() {
        HexagonalArchitectureRules.portsShouldBeInterfaces().check(importedClasses);
    }

    @Test
    @DisplayName("Domain events should be records")
    void domainEventsShouldBeRecords() {
        HexagonalArchitectureRules.domainEventsShouldBeRecords().check(importedClasses);
    }

    @Test
    @DisplayName("Controllers should reside in application.controller package")
    void controllersShouldResideInApplicationController() {
        HexagonalArchitectureRules.controllersShouldResideInApplicationController().check(importedClasses);
    }
}
