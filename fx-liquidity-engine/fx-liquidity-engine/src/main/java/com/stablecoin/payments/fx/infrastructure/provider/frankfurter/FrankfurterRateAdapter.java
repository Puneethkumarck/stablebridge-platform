package com.stablecoin.payments.fx.infrastructure.provider.frankfurter;

import com.stablecoin.payments.fx.domain.model.CorridorRate;
import com.stablecoin.payments.fx.domain.port.RateProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.fx.rate-provider", havingValue = "frankfurter")
@EnableConfigurationProperties(FrankfurterProperties.class)
public class FrankfurterRateAdapter implements RateProvider {

    private static final int DEFAULT_SPREAD_BPS = 30;
    private static final int DEFAULT_FEE_BPS = 30;

    private final RestClient restClient;

    public FrankfurterRateAdapter(FrankfurterProperties properties) {
        var httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.readTimeoutMs()))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @Retry(name = "frankfurter", fallbackMethod = "frankfurterFallback")
    @CircuitBreaker(name = "frankfurter", fallbackMethod = "frankfurterFallback")
    public Optional<CorridorRate> getRate(String fromCurrency, String toCurrency) {
        log.info("[FRANKFURTER] Fetching rate for {}:{}", fromCurrency, toCurrency);

        var response = restClient.get()
                .uri("/latest?from={from}&to={to}", fromCurrency, toCurrency)
                .retrieve()
                .body(FrankfurterRateResponse.class);

        if (response == null || response.rates() == null || response.rates().isEmpty()) {
            log.warn("[FRANKFURTER] No rate returned for {}:{}", fromCurrency, toCurrency);
            return Optional.empty();
        }

        var rate = response.rates().get(toCurrency);
        if (rate == null) {
            log.warn("[FRANKFURTER] Rate for {} not found in response for {}:{}", toCurrency, fromCurrency, toCurrency);
            return Optional.empty();
        }

        var corridorRate = CorridorRate.builder()
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(rate)
                .spreadBps(DEFAULT_SPREAD_BPS)
                .feeBps(DEFAULT_FEE_BPS)
                .provider("frankfurter")
                .ageMs(0)
                .build();

        log.info("[FRANKFURTER] Rate fetched {}:{}={}", fromCurrency, toCurrency, rate);
        return Optional.of(corridorRate);
    }

    @Override
    public String providerName() {
        return "frankfurter";
    }

    @SuppressWarnings("unused")
    private Optional<CorridorRate> frankfurterFallback(String fromCurrency, String toCurrency, Exception ex) {
        log.error("[FRANKFURTER] Resilience fallback for {}:{} due to {}",
                fromCurrency, toCurrency, ex.getClass().getSimpleName(), ex);
        return Optional.empty();
    }
}
