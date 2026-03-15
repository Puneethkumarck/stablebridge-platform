package com.stablecoin.payments.phase3.support;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.awaitility.Awaitility.await;

/**
 * Verifies Kafka events published by services during E2E tests.
 * Creates a fresh consumer per invocation to avoid shared offset state.
 */
public final class KafkaEventVerifier {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventVerifier.class);

    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";

    private final String bootstrapServers;

    public KafkaEventVerifier(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public KafkaEventVerifier() {
        this(DEFAULT_BOOTSTRAP_SERVERS);
    }

    /**
     * Waits for a Kafka event on the given topic that contains the paymentId.
     *
     * @param topic     the Kafka topic to subscribe to
     * @param paymentId the payment ID to search for in event values
     * @param timeout   maximum time to wait
     * @return the first matching event value, or empty if timeout
     */
    public Optional<String> waitForEvent(String topic, String paymentId, Duration timeout) {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "p3t-verify-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of(topic));

            var matchingEvents = new ArrayList<String>();
            await().atMost(timeout)
                    .pollInterval(Duration.ofSeconds(2))
                    .ignoreExceptions()
                    .until(() -> {
                        var records = consumer.poll(Duration.ofMillis(1000));
                        records.records(topic).forEach(record -> {
                            if (record.value() != null && record.value().contains(paymentId)) {
                                log.info("Found matching event on topic={} for paymentId={}",
                                        topic, paymentId);
                                matchingEvents.add(record.value());
                            }
                        });
                        return !matchingEvents.isEmpty();
                    });

            return matchingEvents.isEmpty() ? Optional.empty() : Optional.of(matchingEvents.getFirst());
        } catch (Exception e) {
            log.warn("Kafka event verification failed for topic={}, paymentId={}: {}",
                    topic, paymentId, e.getMessage());
            return Optional.empty();
        }
    }
}
