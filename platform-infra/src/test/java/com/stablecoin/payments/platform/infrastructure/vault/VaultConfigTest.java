package com.stablecoin.payments.platform.infrastructure.vault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VaultConfig")
class VaultConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VaultConfig.class));

    @Test
    @DisplayName("should register VaultConfig when app.vault.enabled=true")
    void shouldRegisterWhenEnabled() {
        contextRunner
                .withPropertyValues("app.vault.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(VaultConfig.class));
    }

    @Test
    @DisplayName("should not register VaultConfig when app.vault.enabled=false")
    void shouldNotRegisterWhenDisabled() {
        contextRunner
                .withPropertyValues("app.vault.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(VaultConfig.class));
    }

    @Test
    @DisplayName("should not register VaultConfig when property is absent (matchIfMissing=false)")
    void shouldNotRegisterWhenPropertyAbsent() {
        contextRunner
                .run(context -> assertThat(context).doesNotHaveBean(VaultConfig.class));
    }
}
