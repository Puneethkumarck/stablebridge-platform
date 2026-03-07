package com.stablecoin.payments.fx;

import org.junit.jupiter.api.Tag;

/**
 * Base class for business (E2E) tests. Extends {@link AbstractIntegrationTest}
 * to reuse the TimescaleDB and Kafka TestContainers singleton.
 */
@Tag("business")
public abstract class AbstractBusinessTest extends AbstractIntegrationTest {
}
