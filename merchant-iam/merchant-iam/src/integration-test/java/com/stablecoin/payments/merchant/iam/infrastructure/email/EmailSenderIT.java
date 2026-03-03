package com.stablecoin.payments.merchant.iam.infrastructure.email;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.iam.domain.EmailSenderProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSenderIT extends AbstractIntegrationTest {

    @Autowired
    private EmailSenderProvider emailSenderProvider;

    private String mailpitApiBase;
    private HttpClient http;

    @BeforeEach
    void setUpMailpit() throws Exception {
        mailpitApiBase = "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025);
        http = HttpClient.newHttpClient();
        // Delete all messages before each test so assertions are isolated
        var deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(mailpitApiBase + "/api/v1/messages"))
                .DELETE()
                .build();
        http.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void sends_invitation_email_to_mailpit() throws Exception {
        var expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        emailSenderProvider.sendInvitationEmail(
                "alice@example.com",
                "Alice Smith",
                "ACME Corp",
                "token-abc123",
                expiresAt);

        var response = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(mailpitApiBase + "/api/v1/messages"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        var body = response.body();
        assertThat(body).contains("alice@example.com");
        assertThat(body).contains("ACME Corp");
    }

    @Test
    void invitation_email_subject_contains_merchant_name() throws Exception {
        var expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        emailSenderProvider.sendInvitationEmail(
                "bob@example.com",
                "Bob Jones",
                "Global Trade Ltd",
                "token-xyz789",
                expiresAt);

        var response = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(mailpitApiBase + "/api/v1/messages"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Global Trade Ltd");
    }

    @Test
    void invitation_email_contains_accept_url_with_token() throws Exception {
        var expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        emailSenderProvider.sendInvitationEmail(
                "carol@example.com",
                "Carol White",
                "Payments Co",
                "unique-token-999",
                expiresAt);

        // List to get the message ID
        var listResponse = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(mailpitApiBase + "/api/v1/messages"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(listResponse.statusCode()).isEqualTo(200);

        // Extract the first message ID from the JSON (simple parse without Jackson dependency)
        var listBody = listResponse.body();
        var idStart = listBody.indexOf("\"ID\":\"") + 6;
        var idEnd = listBody.indexOf("\"", idStart);
        var messageId = listBody.substring(idStart, idEnd);

        // Fetch the full message body via the text/plain endpoint
        var bodyResponse = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(mailpitApiBase + "/api/v1/message/" + messageId))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(bodyResponse.statusCode()).isEqualTo(200);
        assertThat(bodyResponse.body()).contains("unique-token-999");
    }
}
