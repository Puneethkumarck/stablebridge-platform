package com.stablecoin.payments.platform.test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public final class HexagonalArchitectureRules {

    private HexagonalArchitectureRules() {}

    public static void verifyAll(String basePackage) {
        JavaClasses importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(basePackage);

        domainShouldNotDependOnSpring().check(importedClasses);
        domainShouldNotDependOnJpa().check(importedClasses);
        domainShouldNotDependOnInfrastructure().check(importedClasses);
        domainShouldNotDependOnApplication().check(importedClasses);
        infrastructureShouldNotDependOnController().check(importedClasses);
        portsShouldBeInterfaces().check(importedClasses);
        domainEventsShouldBeRecords().check(importedClasses);
        controllersShouldResideInApplicationController().check(importedClasses);
    }

    public static ArchRule domainShouldNotDependOnSpring() {
        return noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat(
                        resideInAPackage("org.springframework..")
                                .and(resideOutsideOfPackage("org.springframework.stereotype.."))
                                .and(resideOutsideOfPackage("org.springframework.transaction.."))
                                .and(resideOutsideOfPackage("org.springframework.beans.factory.annotation.."))
                )
                .as("Domain should not depend on Spring (except stereotype and transaction)");
    }

    public static ArchRule domainShouldNotDependOnJpa() {
        return noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                .as("Domain should not depend on JPA");
    }

    public static ArchRule domainShouldNotDependOnInfrastructure() {
        return noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .as("Domain should not depend on infrastructure");
    }

    public static ArchRule domainShouldNotDependOnApplication() {
        return noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .as("Domain should not depend on application layer");
    }

    public static ArchRule infrastructureShouldNotDependOnController() {
        return noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("..application.controller..")
                .as("Infrastructure should not depend on application controller");
    }

    public static ArchRule portsShouldBeInterfaces() {
        return classes()
                .that().resideInAPackage("..domain.port..")
                .and().areNotRecords()
                .should().beInterfaces()
                .allowEmptyShould(true)
                .as("Ports should be interfaces");
    }

    public static ArchRule domainEventsShouldBeRecords() {
        return classes()
                .that().resideInAPackage("..domain.event..")
                .should().beRecords()
                .allowEmptyShould(true)
                .as("Domain events should be records");
    }

    public static ArchRule controllersShouldResideInApplicationController() {
        return noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideOutsideOfPackage("..application.controller..")
                .allowEmptyShould(true)
                .as("Controllers should reside in application.controller package");
    }
}
