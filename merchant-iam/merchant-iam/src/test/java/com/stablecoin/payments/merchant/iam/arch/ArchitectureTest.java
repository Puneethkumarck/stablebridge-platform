package com.stablecoin.payments.merchant.iam.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.stablecoin.payments.merchant.iam",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_must_not_depend_on_application = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..application..")
            .allowEmptyShould(true);

    // Domain services may use Spring stereotype (@Service, @Component) and transaction
    // (@Transactional) annotations — consistent with the reference implementation
    // (fiat-payout-processor: PayoutCreationService, PayoutInternalRetryService, etc.).
    // Forbidden: Spring web, data, security, cloud, integration, and framework internals.
    @ArchTest
    static final ArchRule domain_must_not_import_spring_web = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.web..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_must_not_import_spring_data = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_must_not_import_spring_security = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.security..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_must_not_import_jpa = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_application_controller = noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..application.controller..")
            .allowEmptyShould(true);
}
