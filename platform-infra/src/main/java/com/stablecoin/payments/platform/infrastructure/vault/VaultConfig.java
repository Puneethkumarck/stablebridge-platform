package com.stablecoin.payments.platform.infrastructure.vault;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Activates Spring Cloud Vault integration for centralised secrets management.
 *
 * <p>Spring Cloud Vault auto-configures a {@code VaultTemplate} and the
 * {@code spring.config.import=optional:vault://} ConfigData source whenever its
 * starter is on the classpath <b>and</b> Vault connectivity is configured.
 *
 * <p>This configuration class acts as the opt-in toggle:
 * <ul>
 *   <li>Production / staging: set {@code app.vault.enabled=true} and supply
 *       {@code VAULT_TOKEN} (or use Kubernetes/AppRole auth).</li>
 *   <li>Local dev: Vault runs in Docker Compose dev mode with a static
 *       root token ({@code dev-root-token}).</li>
 *   <li>Tests: defaults to disabled ({@code matchIfMissing = false}) so that
 *       unit and integration tests never require a running Vault instance.</li>
 * </ul>
 *
 * <p>Secrets are resolved through the KV v2 engine at
 * {@code secret/{application-name}} and the shared context
 * {@code secret/application}. Property names in Vault map directly to
 * Spring property keys (e.g. {@code spring.datasource.password}).
 *
 * @see <a href="https://docs.spring.io/spring-cloud-vault/reference/">
 *     Spring Cloud Vault Reference</a>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.vault.enabled", havingValue = "true", matchIfMissing = false)
public class VaultConfig {

    public VaultConfig() {
        log.info("Vault secrets management enabled — resolving properties from HashiCorp Vault");
    }
}
