package com.stablecoin.payments.orchestrator.domain.service;

import com.stablecoin.payments.orchestrator.domain.event.PaymentInitiated;
import com.stablecoin.payments.orchestrator.domain.model.Corridor;
import com.stablecoin.payments.orchestrator.domain.model.Money;
import com.stablecoin.payments.orchestrator.domain.model.Payment;
import com.stablecoin.payments.orchestrator.domain.model.PaymentNotCancellableException;
import com.stablecoin.payments.orchestrator.domain.model.PaymentNotFoundException;
import com.stablecoin.payments.orchestrator.domain.model.PaymentState;
import com.stablecoin.payments.orchestrator.domain.port.PaymentEventPublisher;
import com.stablecoin.payments.orchestrator.domain.port.PaymentRepository;
import com.stablecoin.payments.orchestrator.domain.workflow.PaymentWorkflow;
import com.stablecoin.payments.orchestrator.domain.workflow.dto.CancelRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.CORRELATION_ID;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.IDEMPOTENCY_KEY;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.RECIPIENT_ID;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.SENDER_ID;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.SOURCE_AMOUNT_VALUE;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.SOURCE_COUNTRY;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.SOURCE_CURRENCY;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.TARGET_COUNTRY;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.TARGET_CURRENCY;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.aCompletedPayment;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.aFailedPayment;
import static com.stablecoin.payments.orchestrator.fixtures.PaymentFixtures.anInitiatedPayment;
import static com.stablecoin.payments.platform.test.TestUtils.eqIgnoring;
import static com.stablecoin.payments.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandHandler")
class PaymentCommandHandlerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Mock
    private WorkflowClient workflowClient;

    @InjectMocks
    private PaymentCommandHandler handler;

    private static <T> Class<T> argThatClass(Class<T> expected) {
        return argThat(c -> c == expected);
    }

    @Nested
    @DisplayName("initiatePayment")
    class InitiatePayment {

        @Test
        @DisplayName("should create payment, save, and start Temporal workflow")
        void shouldCreateAndStartWorkflow() {
            // given
            var expectedPayment = Payment.initiate(
                    IDEMPOTENCY_KEY, CORRELATION_ID, SENDER_ID, RECIPIENT_ID,
                    new Money(SOURCE_AMOUNT_VALUE, SOURCE_CURRENCY),
                    SOURCE_CURRENCY, TARGET_CURRENCY,
                    new Corridor(SOURCE_COUNTRY, TARGET_COUNTRY)
            );

            given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                    .willReturn(Optional.empty());
            given(paymentRepository.save(eqIgnoring(expectedPayment, "paymentId", "createdAt", "updatedAt", "expiresAt")))
                    .willAnswer(inv -> inv.getArgument(0));

            var workflowStub = mock(PaymentWorkflow.class);
            given(workflowClient.newWorkflowStub(
                    argThatClass(PaymentWorkflow.class),
                    argThat((WorkflowOptions opts) -> opts != null && opts.getWorkflowId().startsWith("payment-"))))
                    .willReturn(workflowStub);

            // when
            var result = handler.initiatePayment(
                    IDEMPOTENCY_KEY, CORRELATION_ID,
                    SENDER_ID, RECIPIENT_ID, SOURCE_AMOUNT_VALUE,
                    SOURCE_CURRENCY, TARGET_CURRENCY,
                    SOURCE_COUNTRY, TARGET_COUNTRY
            );

            // then
            assertThat(result.replay()).isFalse();

            then(paymentRepository).should().save(eqIgnoring(expectedPayment, "paymentId", "createdAt", "updatedAt", "expiresAt"));

            var expectedEvent = new PaymentInitiated(
                    result.payment().paymentId(), IDEMPOTENCY_KEY, CORRELATION_ID,
                    SENDER_ID, RECIPIENT_ID, new Money(SOURCE_AMOUNT_VALUE, SOURCE_CURRENCY),
                    TARGET_CURRENCY, new Corridor(SOURCE_COUNTRY, TARGET_COUNTRY), null);
            then(eventPublisher).should().publish(eqIgnoringTimestamps(expectedEvent));

            then(workflowClient).should().newWorkflowStub(
                    argThat((Class<?> c) -> c == PaymentWorkflow.class),
                    argThat((WorkflowOptions opts) ->
                            opts != null
                            && opts.getWorkflowId().equals("payment-" + result.payment().paymentId())
                            && PaymentCommandHandler.TASK_QUEUE.equals(opts.getTaskQueue())));
        }

        @Test
        @DisplayName("should return existing payment on idempotent replay")
        void shouldReturnExistingOnReplay() {
            // given
            var existingPayment = anInitiatedPayment();
            given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                    .willReturn(Optional.of(existingPayment));

            // when
            var result = handler.initiatePayment(
                    IDEMPOTENCY_KEY, CORRELATION_ID,
                    SENDER_ID, RECIPIENT_ID, SOURCE_AMOUNT_VALUE,
                    SOURCE_CURRENCY, TARGET_CURRENCY,
                    SOURCE_COUNTRY, TARGET_COUNTRY
            );

            // then
            assertThat(result.replay()).isTrue();
            assertThat(result.payment())
                    .usingRecursiveComparison()
                    .isEqualTo(existingPayment);

            then(paymentRepository).should(never()).save(existingPayment);
            then(eventPublisher).shouldHaveNoInteractions();
            then(workflowClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("should handle concurrent duplicate by returning existing payment")
        void shouldHandleConcurrentDuplicate() {
            // given
            var expectedPayment = Payment.initiate(
                    IDEMPOTENCY_KEY, CORRELATION_ID, SENDER_ID, RECIPIENT_ID,
                    new Money(SOURCE_AMOUNT_VALUE, SOURCE_CURRENCY),
                    SOURCE_CURRENCY, TARGET_CURRENCY,
                    new Corridor(SOURCE_COUNTRY, TARGET_COUNTRY)
            );
            var existingPayment = anInitiatedPayment();
            given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.of(existingPayment));
            given(paymentRepository.save(eqIgnoring(expectedPayment, "paymentId", "createdAt", "updatedAt", "expiresAt")))
                    .willThrow(new DataIntegrityViolationException("duplicate key"));

            // when
            var result = handler.initiatePayment(
                    IDEMPOTENCY_KEY, CORRELATION_ID,
                    SENDER_ID, RECIPIENT_ID, SOURCE_AMOUNT_VALUE,
                    SOURCE_CURRENCY, TARGET_CURRENCY,
                    SOURCE_COUNTRY, TARGET_COUNTRY
            );

            // then
            assertThat(result.replay()).isTrue();
            assertThat(result.payment())
                    .usingRecursiveComparison()
                    .isEqualTo(existingPayment);

            then(eventPublisher).shouldHaveNoInteractions();
            then(workflowClient).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        @DisplayName("should return payment when found")
        void shouldReturnPayment() {
            // given
            var payment = anInitiatedPayment();
            given(paymentRepository.findById(payment.paymentId()))
                    .willReturn(Optional.of(payment));

            // when
            var result = handler.getPayment(payment.paymentId());

            // then
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(payment);
        }

        @Test
        @DisplayName("should throw PaymentNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            // given
            var paymentId = UUID.randomUUID();
            given(paymentRepository.findById(paymentId))
                    .willReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> handler.getPayment(paymentId))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining(paymentId.toString());
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("should send cancel signal to Temporal workflow")
        void shouldSendCancelSignal() {
            // given
            var payment = anInitiatedPayment();
            given(paymentRepository.findById(payment.paymentId()))
                    .willReturn(Optional.of(payment));

            var workflowStub = mock(PaymentWorkflow.class);
            given(workflowClient.newWorkflowStub(
                    PaymentWorkflow.class,
                    "payment-" + payment.paymentId()))
                    .willReturn(workflowStub);

            // when
            var result = handler.cancelPayment(payment.paymentId(), "Customer requested");

            // then
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(payment);

            var expectedCancel = new CancelRequest(payment.paymentId(), "Customer requested", "API");
            then(workflowStub).should().cancelPayment(expectedCancel);
        }

        @Test
        @DisplayName("should throw PaymentNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            // given
            var paymentId = UUID.randomUUID();
            given(paymentRepository.findById(paymentId))
                    .willReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> handler.cancelPayment(paymentId, "reason"))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining(paymentId.toString());
        }

        @Test
        @DisplayName("should throw PaymentNotCancellableException for completed payment")
        void shouldThrowWhenCompletedPayment() {
            // given
            var payment = aCompletedPayment();
            given(paymentRepository.findById(payment.paymentId()))
                    .willReturn(Optional.of(payment));

            // when/then
            assertThatThrownBy(() -> handler.cancelPayment(payment.paymentId(), "reason"))
                    .isInstanceOf(PaymentNotCancellableException.class)
                    .hasMessageContaining(payment.paymentId().toString())
                    .hasMessageContaining(PaymentState.COMPLETED.name());
        }

        @Test
        @DisplayName("should throw PaymentNotCancellableException for failed payment")
        void shouldThrowWhenFailedPayment() {
            // given
            var payment = aFailedPayment();
            given(paymentRepository.findById(payment.paymentId()))
                    .willReturn(Optional.of(payment));

            // when/then
            assertThatThrownBy(() -> handler.cancelPayment(payment.paymentId(), "reason"))
                    .isInstanceOf(PaymentNotCancellableException.class)
                    .hasMessageContaining(payment.paymentId().toString())
                    .hasMessageContaining(PaymentState.FAILED.name());
        }
    }
}
