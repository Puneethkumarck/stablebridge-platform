package com.stablecoin.payments.fx.application.controller;

import com.stablecoin.payments.fx.api.request.FxQuoteRequest;
import com.stablecoin.payments.fx.api.request.FxRateLockRequest;
import com.stablecoin.payments.fx.api.response.FxQuoteResponse;
import com.stablecoin.payments.fx.api.response.FxRateLockResponse;
import com.stablecoin.payments.fx.application.mapper.FxResponseMapper;
import com.stablecoin.payments.fx.domain.exception.LockNotFoundException;
import com.stablecoin.payments.fx.domain.exception.QuoteAlreadyLockedException;
import com.stablecoin.payments.fx.domain.exception.QuoteExpiredException;
import com.stablecoin.payments.fx.domain.exception.QuoteNotFoundException;
import com.stablecoin.payments.fx.domain.exception.RateUnavailableException;
import com.stablecoin.payments.fx.domain.service.FxQuoteCommandHandler;
import com.stablecoin.payments.fx.domain.service.FxRateLockCommandHandler;
import com.stablecoin.payments.fx.domain.service.FxRateLockCommandHandler.LockRateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.stablecoin.payments.fx.fixtures.FxQuoteFixtures.anActiveQuote;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.CORRELATION_ID;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.PAYMENT_ID;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.SOURCE_COUNTRY;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.TARGET_COUNTRY;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.anActiveLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("FxQuoteController")
class FxQuoteControllerTest {

    @Mock
    private FxQuoteCommandHandler quoteCommandHandler;

    @Mock
    private FxRateLockCommandHandler rateLockCommandHandler;

    @Mock
    private FxResponseMapper responseMapper;

    @InjectMocks
    private FxQuoteController controller;

    @Nested
    @DisplayName("GET /v1/fx/quote")
    class GetQuote {

        @Test
        @DisplayName("should delegate to command handler and map response")
        void shouldGetQuote() {
            var request = new FxQuoteRequest("USD", "EUR", new BigDecimal("10000.00"));
            var quote = anActiveQuote();
            var now = Instant.now();
            var expectedResponse = new FxQuoteResponse(
                    quote.quoteId(), "USD", "EUR",
                    new BigDecimal("10000.00"), new BigDecimal("9200.00"),
                    new BigDecimal("0.92"), new BigDecimal("1.087"),
                    30, new BigDecimal("30.00"), "REFINITIV",
                    now, now.plusSeconds(300));

            given(quoteCommandHandler.getQuote("USD", "EUR", new BigDecimal("10000.00")))
                    .willReturn(quote);
            given(responseMapper.toResponse(quote)).willReturn(expectedResponse);

            var result = controller.getQuote(request);

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            then(quoteCommandHandler).should().getQuote("USD", "EUR", new BigDecimal("10000.00"));
            then(responseMapper).should().toResponse(quote);
        }

        @Test
        @DisplayName("should propagate RateUnavailableException")
        void shouldPropagateRateUnavailable() {
            var request = new FxQuoteRequest("JPY", "BRL", new BigDecimal("10000.00"));
            given(quoteCommandHandler.getQuote("JPY", "BRL", new BigDecimal("10000.00")))
                    .willThrow(RateUnavailableException.forCorridor("JPY", "BRL"));

            assertThatThrownBy(() -> controller.getQuote(request))
                    .isInstanceOf(RateUnavailableException.class)
                    .hasMessageContaining("JPY")
                    .hasMessageContaining("BRL");
        }
    }

    @Nested
    @DisplayName("GET /v1/fx/quote/{quoteId}")
    class GetQuoteById {

        @Test
        @DisplayName("should delegate to command handler and map response")
        void shouldGetQuoteById() {
            var quoteId = UUID.randomUUID();
            var quote = anActiveQuote();
            var now = Instant.now();
            var expectedResponse = new FxQuoteResponse(
                    quoteId, "USD", "EUR",
                    new BigDecimal("10000.00"), new BigDecimal("9200.00"),
                    new BigDecimal("0.92"), new BigDecimal("1.087"),
                    30, new BigDecimal("30.00"), "REFINITIV",
                    now, now.plusSeconds(300));

            given(quoteCommandHandler.getQuoteById(quoteId)).willReturn(quote);
            given(responseMapper.toResponse(quote)).willReturn(expectedResponse);

            var result = controller.getQuoteById(quoteId);

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            then(quoteCommandHandler).should().getQuoteById(quoteId);
            then(responseMapper).should().toResponse(quote);
        }

        @Test
        @DisplayName("should propagate QuoteNotFoundException")
        void shouldPropagateQuoteNotFound() {
            var quoteId = UUID.randomUUID();
            given(quoteCommandHandler.getQuoteById(quoteId))
                    .willThrow(QuoteNotFoundException.withId(quoteId));

            assertThatThrownBy(() -> controller.getQuoteById(quoteId))
                    .isInstanceOf(QuoteNotFoundException.class)
                    .hasMessageContaining(quoteId.toString());
        }
    }

    @Nested
    @DisplayName("POST /v1/fx/lock/{quoteId}")
    class LockRate {

        @Test
        @DisplayName("should return 201 Created for new lock")
        void shouldReturn201ForNewLock() {
            var quoteId = UUID.randomUUID();
            var request = new FxRateLockRequest(PAYMENT_ID, CORRELATION_ID, SOURCE_COUNTRY, TARGET_COUNTRY);
            var lock = anActiveLock(quoteId, PAYMENT_ID);
            var now = Instant.now();
            var lockResponse = new FxRateLockResponse(
                    lock.lockId(), quoteId, PAYMENT_ID,
                    "USD", "EUR",
                    new BigDecimal("10000.00"), new BigDecimal("9200.00"),
                    new BigDecimal("0.92"), 30, new BigDecimal("30.00"),
                    now, now.plusSeconds(30));

            given(rateLockCommandHandler.lockRate(
                    quoteId, PAYMENT_ID, CORRELATION_ID, SOURCE_COUNTRY, TARGET_COUNTRY))
                    .willReturn(new LockRateResult(lock, true));
            given(responseMapper.toResponse(lock)).willReturn(lockResponse);

            var result = controller.lockRate(quoteId, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody())
                    .usingRecursiveComparison()
                    .isEqualTo(lockResponse);
        }

        @Test
        @DisplayName("should return 200 OK for idempotent lock")
        void shouldReturn200ForIdempotentLock() {
            var quoteId = UUID.randomUUID();
            var request = new FxRateLockRequest(PAYMENT_ID, CORRELATION_ID, SOURCE_COUNTRY, TARGET_COUNTRY);
            var lock = anActiveLock(quoteId, PAYMENT_ID);
            var now = Instant.now();
            var lockResponse = new FxRateLockResponse(
                    lock.lockId(), quoteId, PAYMENT_ID,
                    "USD", "EUR",
                    new BigDecimal("10000.00"), new BigDecimal("9200.00"),
                    new BigDecimal("0.92"), 30, new BigDecimal("30.00"),
                    now, now.plusSeconds(30));

            given(rateLockCommandHandler.lockRate(
                    quoteId, PAYMENT_ID, CORRELATION_ID, SOURCE_COUNTRY, TARGET_COUNTRY))
                    .willReturn(new LockRateResult(lock, false));
            given(responseMapper.toResponse(lock)).willReturn(lockResponse);

            var result = controller.lockRate(quoteId, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody())
                    .usingRecursiveComparison()
                    .isEqualTo(lockResponse);
        }

        @Test
        @DisplayName("should propagate QuoteNotFoundException")
        void shouldPropagateQuoteNotFound() {
            var quoteId = UUID.randomUUID();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();
            var request = new FxRateLockRequest(paymentId, correlationId, "US", "DE");
            given(rateLockCommandHandler.lockRate(quoteId, paymentId, correlationId, "US", "DE"))
                    .willThrow(QuoteNotFoundException.withId(quoteId));

            assertThatThrownBy(() -> controller.lockRate(quoteId, request))
                    .isInstanceOf(QuoteNotFoundException.class)
                    .hasMessageContaining(quoteId.toString());
        }

        @Test
        @DisplayName("should propagate QuoteExpiredException")
        void shouldPropagateQuoteExpired() {
            var quoteId = UUID.randomUUID();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();
            var request = new FxRateLockRequest(paymentId, correlationId, "US", "DE");
            given(rateLockCommandHandler.lockRate(quoteId, paymentId, correlationId, "US", "DE"))
                    .willThrow(QuoteExpiredException.withId(quoteId));

            assertThatThrownBy(() -> controller.lockRate(quoteId, request))
                    .isInstanceOf(QuoteExpiredException.class)
                    .hasMessageContaining(quoteId.toString());
        }

        @Test
        @DisplayName("should propagate QuoteAlreadyLockedException")
        void shouldPropagateQuoteAlreadyLocked() {
            var quoteId = UUID.randomUUID();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();
            var request = new FxRateLockRequest(paymentId, correlationId, "US", "DE");
            given(rateLockCommandHandler.lockRate(quoteId, paymentId, correlationId, "US", "DE"))
                    .willThrow(QuoteAlreadyLockedException.withId(quoteId));

            assertThatThrownBy(() -> controller.lockRate(quoteId, request))
                    .isInstanceOf(QuoteAlreadyLockedException.class)
                    .hasMessageContaining(quoteId.toString());
        }
    }

    @Nested
    @DisplayName("DELETE /v1/fx/lock/{lockId}")
    class ReleaseLock {

        @Test
        @DisplayName("should return 204 No Content when release succeeds")
        void shouldReturnNoContentWhenReleaseSucceeds() {
            var lockId = UUID.randomUUID();

            var result = controller.releaseLock(lockId);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            then(rateLockCommandHandler).should().releaseLock(lockId);
        }

        @Test
        @DisplayName("should propagate LockNotFoundException")
        void shouldPropagateExceptionWhenLockNotFound() {
            var lockId = UUID.randomUUID();
            willThrow(LockNotFoundException.withId(lockId))
                    .given(rateLockCommandHandler).releaseLock(lockId);

            assertThatThrownBy(() -> controller.releaseLock(lockId))
                    .isInstanceOf(LockNotFoundException.class)
                    .hasMessageContaining(lockId.toString());
        }
    }
}
