package com.stablecoin.payments.platform.infrastructure.vault;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.vault.enabled", havingValue = "true", matchIfMissing = false)
public class VaultConfig {

    public VaultConfig() {
        log.info("Vault secrets management enabled — resolving properties from HashiCorp Vault");
    }
}
