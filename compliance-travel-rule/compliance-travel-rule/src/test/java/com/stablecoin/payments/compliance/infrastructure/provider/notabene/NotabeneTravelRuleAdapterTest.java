package com.stablecoin.payments.compliance.infrastructure.provider.notabene;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.stablecoin.payments.compliance.domain.model.TransmissionStatus;
import com.stablecoin.payments.compliance.domain.model.TravelRulePackage;
import com.stablecoin.payments.compliance.domain.model.TravelRuleProtocol;
import com.stablecoin.payments.compliance.domain.model.VaspInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotabeneTravelRuleAdapterTest {

    private static final String VASP_DID = "did:ethr:0xtest1234567890";
    private static final String BENEFICIARY_VASP_DID = "did:ethr:0xbeneficiary9876";
    private static final String TEST_TRANSFER_ID = "ntb-transfer-001";
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_AUDIENCE = "https://api.test.notabene.id";
    private static final String TEST_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test-token";

    private static WireMockServer wireMock;
    private NotabeneTravelRuleAdapter adapter;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        var properties = new NotabeneProperties(
                wireMock.baseUrl(),
                wireMock.baseUrl() + "/oauth/token",
                TEST_CLIENT_ID,
                TEST_CLIENT_SECRET,
                TEST_AUDIENCE,
                VASP_DID,
                2
        );
        adapter = new NotabeneTravelRuleAdapter(properties, null);
    }

    private void stubTokenSuccess() {
        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "access_token": "%s",
                              "expires_in": 86400,
                              "token_type": "Bearer"
                            }
                            """.formatted(TEST_ACCESS_TOKEN))));
    }

    private void stubTokenFailure() {
        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "error": "access_denied",
                              "error_description": "Unauthorized"
                            }
                            """)));
    }

    private void stubCreateTransferSuccess(UUID packageId) {
        wireMock.stubFor(post(urlPathEqualTo("/entities/" + VASP_DID + "/tx"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "id": "%s",
                              "status": "SENT",
                              "transactionRef": "%s",
                              "transactionAsset": "USDC",
                              "transactionAmount": "0",
                              "originatorVASPdid": "%s",
                              "beneficiaryVASPdid": "%s",
                              "createdAt": "2026-03-18T10:00:00Z",
                              "updatedAt": "2026-03-18T10:00:00Z"
                            }
                            """.formatted(TEST_TRANSFER_ID, packageId, VASP_DID, BENEFICIARY_VASP_DID))));
    }

    private void stubCreateTransferClientError() {
        wireMock.stubFor(post(urlPathEqualTo("/entities/" + VASP_DID + "/tx"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "message": "Invalid originator data",
                              "code": "VALIDATION_ERROR"
                            }
                            """)));
    }

    private void stubCreateTransferServerError() {
        wireMock.stubFor(post(urlPathEqualTo("/entities/" + VASP_DID + "/tx"))
                .willReturn(aResponse().withStatus(500)));
    }

    private TravelRulePackage buildTravelRulePackage(UUID packageId, UUID checkId) {
        return TravelRulePackage.builder()
                .packageId(packageId)
                .checkId(checkId)
                .originatorVasp(VaspInfo.builder()
                        .vaspId("vasp-originator")
                        .name("StableBridge")
                        .country("US")
                        .did(VASP_DID)
                        .build())
                .beneficiaryVasp(VaspInfo.builder()
                        .vaspId("vasp-beneficiary")
                        .name("BeneficiaryVASP")
                        .country("DE")
                        .did(BENEFICIARY_VASP_DID)
                        .build())
                .originatorData("{}")
                .beneficiaryData("{}")
                .protocol(TravelRuleProtocol.IVMS101)
                .transmissionStatus(TransmissionStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("Successful transmission")
    class SuccessfulTransmission {

        @Test
        @DisplayName("should transmit travel rule package and return transfer ID")
        void transmitSuccess() {
            var packageId = UUID.randomUUID();
            var checkId = UUID.randomUUID();
            stubTokenSuccess();
            stubCreateTransferSuccess(packageId);

            var travelRulePackage = buildTravelRulePackage(packageId, checkId);
            var result = adapter.transmit(travelRulePackage);

            assertThat(result).isEqualTo(TEST_TRANSFER_ID);

            wireMock.verify(1, postRequestedFor(urlEqualTo("/oauth/token")));
            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/entities/" + VASP_DID + "/tx"))
                    .withHeader("Authorization", equalTo("Bearer " + TEST_ACCESS_TOKEN)));
        }

        @Test
        @DisplayName("should send correct IVMS101 originator and beneficiary data")
        void transmitWithIvmsData() {
            var packageId = UUID.randomUUID();
            var checkId = UUID.randomUUID();
            stubTokenSuccess();
            stubCreateTransferSuccess(packageId);

            var travelRulePackage = TravelRulePackage.builder()
                    .packageId(packageId)
                    .checkId(checkId)
                    .originatorVasp(VaspInfo.builder()
                            .vaspId("vasp-originator")
                            .name("StableBridge")
                            .country("US")
                            .did(VASP_DID)
                            .build())
                    .beneficiaryVasp(VaspInfo.builder()
                            .vaspId("vasp-beneficiary")
                            .name("BeneficiaryVASP")
                            .country("DE")
                            .did(BENEFICIARY_VASP_DID)
                            .build())
                    .originatorData("""
                        {"primaryIdentifier":"Doe","secondaryIdentifier":"John","address":"123 Main St","accountNumber":"acc-001","amount":"1000"}""")
                    .beneficiaryData("""
                        {"primaryIdentifier":"Mueller","secondaryIdentifier":"Hans","accountNumber":"acc-002"}""")
                    .protocol(TravelRuleProtocol.IVMS101)
                    .transmissionStatus(TransmissionStatus.PENDING)
                    .build();

            var result = adapter.transmit(travelRulePackage);

            assertThat(result).isEqualTo(TEST_TRANSFER_ID);

            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/entities/" + VASP_DID + "/tx")));
        }
    }

    @Nested
    @DisplayName("Token caching")
    class TokenCaching {

        @Test
        @DisplayName("should reuse cached token for subsequent calls")
        void tokenCaching() {
            var packageId1 = UUID.randomUUID();
            var packageId2 = UUID.randomUUID();
            stubTokenSuccess();
            stubCreateTransferSuccess(packageId1);

            var pkg1 = buildTravelRulePackage(packageId1, UUID.randomUUID());
            adapter.transmit(pkg1);

            stubCreateTransferSuccess(packageId2);
            var pkg2 = buildTravelRulePackage(packageId2, UUID.randomUUID());
            adapter.transmit(pkg2);

            wireMock.verify(1, postRequestedFor(urlEqualTo("/oauth/token")));
            wireMock.verify(2, postRequestedFor(urlPathEqualTo("/entities/" + VASP_DID + "/tx")));
        }
    }

    @Nested
    @DisplayName("Authentication errors")
    class AuthenticationErrors {

        @Test
        @DisplayName("should throw when OAuth2 token request returns 401")
        void tokenUnauthorized() {
            stubTokenFailure();

            var travelRulePackage = buildTravelRulePackage(UUID.randomUUID(), UUID.randomUUID());

            assertThatThrownBy(() -> adapter.transmit(travelRulePackage))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Transfer API errors")
    class TransferApiErrors {

        @Test
        @DisplayName("should throw when create transfer returns 400 client error")
        void clientError() {
            stubTokenSuccess();
            stubCreateTransferClientError();

            var travelRulePackage = buildTravelRulePackage(UUID.randomUUID(), UUID.randomUUID());

            assertThatThrownBy(() -> adapter.transmit(travelRulePackage))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw when create transfer returns 500 server error")
        void serverError() {
            stubTokenSuccess();
            stubCreateTransferServerError();

            var travelRulePackage = buildTravelRulePackage(UUID.randomUUID(), UUID.randomUUID());

            assertThatThrownBy(() -> adapter.transmit(travelRulePackage))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Token request body")
    class TokenRequestBody {

        @Test
        @DisplayName("should send correct OAuth2 client credentials in token request")
        void correctTokenRequest() {
            var packageId = UUID.randomUUID();
            stubTokenSuccess();
            stubCreateTransferSuccess(packageId);

            var travelRulePackage = buildTravelRulePackage(packageId, UUID.randomUUID());
            adapter.transmit(travelRulePackage);

            wireMock.verify(1, postRequestedFor(urlEqualTo("/oauth/token"))
                    .withRequestBody(equalToJson("""
                        {
                          "client_id": "%s",
                          "client_secret": "%s",
                          "grant_type": "client_credentials",
                          "audience": "%s"
                        }
                        """.formatted(TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_AUDIENCE))));
        }
    }
}
