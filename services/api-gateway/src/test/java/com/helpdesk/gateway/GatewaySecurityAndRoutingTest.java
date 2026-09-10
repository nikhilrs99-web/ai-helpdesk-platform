package com.helpdesk.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * SecurityConfig's authorizeExchange rules and the route predicates are both declarative -
 * there's no Java branching to unit-test directly, so this proves the whole gateway (Spring
 * context, security filter chain, routing) actually behaves as configured. src/test/resources
 * /application.yml replaces the main config for this test with a single route pointed at
 * localhost:18089, where a tiny JDK HttpServer stands in for a real backend service - this
 * never needs a running ticket-service. (An indexed-property override via
 * @DynamicPropertySource looked simpler but silently zeroed out the route's other fields,
 * e.g. its predicates - Spring Cloud Gateway's route list binding doesn't merge partial
 * overrides across property sources the way plain @ConfigurationProperties does.)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityAndRoutingTest {

    private static HttpServer stubBackend;

    @Autowired
    private WebTestClient webTestClient;

    // Replaces the auto-configured decoder that would otherwise try to reach the real
    // Keycloak (jwk-set-uri) at request time - mockJwt() injects an authenticated principal
    // directly into the security context and never calls this, but the bean still has to
    // exist for the context to start without a reachable issuer.
    @MockBean
    private org.springframework.security.oauth2.jwt.ReactiveJwtDecoder jwtDecoder;

    private static final int STUB_PORT = 18089;

    @BeforeAll
    static void startStubBackend() throws IOException {
        stubBackend = HttpServer.create(new InetSocketAddress("localhost", STUB_PORT), 0);
        stubBackend.createContext("/api/tickets/hello", exchange -> {
            byte[] body = "{\"from\":\"stub-ticket-service\"}".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        stubBackend.start();
    }

    @AfterAll
    static void stopStubBackend() {
        stubBackend.stop(0);
    }

    @Test
    void protectedRouteRejectsRequestsWithNoToken() {
        webTestClient.get().uri("/api/tickets/hello")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedRouteForwardsAnAuthenticatedRequestToTheBackend() {
        webTestClient.mutateWith(mockJwt())
                .get().uri("/api/tickets/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.from").isEqualTo("stub-ticket-service");
    }

    @Test
    void actuatorHealthIsReachableWithoutAuthentication() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unknownPathWithNoMatchingRouteAndNoTokenIsStillRejectedAsUnauthorized() {
        // authorizeExchange runs before routing decides there's nowhere to send this -
        // an anonymous caller must never learn whether a path maps to a real route.
        webTestClient.get().uri("/api/does-not-exist")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
