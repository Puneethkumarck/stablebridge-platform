package com.stablecoin.payments.offramp.arch;

import com.stablecoin.payments.platform.test.HexagonalArchitectureRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Architecture Rules")
class ArchitectureTest {

    @Test
    @DisplayName("Verify hexagonal architecture rules")
    void verifyHexagonalArchitecture() {
        HexagonalArchitectureRules.verifyAll("com.stablecoin.payments.offramp");
    }
}
