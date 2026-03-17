package com.stablecoin.payments.platform.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaDlqConfig")
class KafkaDlqConfigTest {

    private final KafkaDlqConfig config = new KafkaDlqConfig();

    @Mock
    private KafkaOperations<String, Object> kafkaOperations;

    @Test
    @DisplayName("should create DeadLetterPublishingRecoverer bean")
    void shouldCreateDeadLetterPublishingRecoverer() {
        // When
        var recoverer = config.deadLetterPublishingRecoverer(kafkaOperations);

        // Then
        assertThat(recoverer).isInstanceOf(DeadLetterPublishingRecoverer.class);
    }

    @Test
    @DisplayName("should create DefaultErrorHandler bean with recoverer")
    void shouldCreateDefaultErrorHandler() {
        // Given
        var recoverer = config.deadLetterPublishingRecoverer(kafkaOperations);

        // When
        var errorHandler = config.kafkaErrorHandler(recoverer);

        // Then
        assertThat(errorHandler).isInstanceOf(DefaultErrorHandler.class);
    }
}
