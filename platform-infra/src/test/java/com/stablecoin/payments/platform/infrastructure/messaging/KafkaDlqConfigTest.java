package com.stablecoin.payments.platform.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.backoff.BackOff;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaDlqConfig")
class KafkaDlqConfigTest {

    private final KafkaDlqConfig config = new KafkaDlqConfig();

    @Mock
    private KafkaOperations<String, Object> kafkaOperations;

    @Test
    @DisplayName("should create DeadLetterPublishingRecoverer wired with KafkaOperations")
    void shouldCreateDeadLetterPublishingRecoverer() {
        // when
        var recoverer = config.deadLetterPublishingRecoverer(kafkaOperations);

        // then
        assertThat(recoverer).isNotNull();
    }

    @Test
    @DisplayName("should configure error handler with exponential backoff: 3 retries, 1s initial, 2x multiplier")
    void shouldConfigureErrorHandlerWithExponentialBackoff() {
        // given
        var recoverer = config.deadLetterPublishingRecoverer(kafkaOperations);
        var expectedBackOff = new ExponentialBackOffWithMaxRetries(3);
        expectedBackOff.setInitialInterval(1_000L);
        expectedBackOff.setMultiplier(2.0);
        expectedBackOff.setMaxInterval(10_000L);

        // when
        var errorHandler = config.kafkaErrorHandler(recoverer);

        // then
        var failureTracker = ReflectionTestUtils.getField(errorHandler, "failureTracker");
        var actualBackOff = (BackOff) ReflectionTestUtils.getField(failureTracker, "backOff");
        assertThat(actualBackOff)
                .usingRecursiveComparison()
                .isEqualTo(expectedBackOff);
    }
}
