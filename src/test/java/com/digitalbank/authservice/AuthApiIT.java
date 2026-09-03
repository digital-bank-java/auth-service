package com.digitalbank.authservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiIT {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Test
    void loginReturnsSignedTokenWithSessionId() throws Exception {
        var response = postJson(
                "/api/v1/auth/login", "{\"username\":\"alice@example.com\",\"password\":\"correct-password\"}");
        var body = objectMapper.readTree(response.body());
        var payload = decodeJwtPayload(body.path("accessToken").asText());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.path("sessionId").asText()).isNotBlank();
        assertThat(body.path("expiresAt").asText()).isNotBlank();
        assertThat(payload.path("sub").asText()).isEqualTo("alice@example.com");
        assertThat(payload.path("sid").asText())
                .isEqualTo(body.path("sessionId").asText());
        assertThat(payload.path("active").asBoolean()).isTrue();
        assertThat(payload.path("iss").asText()).isEqualTo("digital-bank-auth-test");
        assertThat(payload.path("iat").asLong()).isPositive();
        assertThat(payload.path("exp").asLong())
                .isGreaterThan(payload.path("iat").asLong());
    }

    @Test
    void unknownAndWrongPasswordReturnSameUnauthorizedProblem() throws Exception {
        var unknown = postJson(
                "/api/v1/auth/login", "{\"username\":\"unknown@example.com\",\"password\":\"correct-password\"}");
        var wrongPassword =
                postJson("/api/v1/auth/login", "{\"username\":\"alice@example.com\",\"password\":\"wrong-password\"}");

        assertThat(unknown.statusCode()).isEqualTo(401);
        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(unknown.body()).isEqualTo(wrongPassword.body());
        assertThat(objectMapper.readTree(unknown.body()).path("type").asText())
                .isEqualTo("https://digital-bank-java.local/problems/authentication-failed");
    }

    @Test
    void invalidLoginRequestReturnsValidationProblem() throws Exception {
        var response = postJson("/api/v1/auth/login", "{\"username\":\"\",\"password\":\"short\"}");
        var body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(body.path("type").asText()).isEqualTo("https://digital-bank-java.local/problems/validation-error");
        assertThat(body.path("errors").isArray()).isTrue();
    }

    @Test
    void logoutIsIdempotentForTheSameBearerToken() throws Exception {
        var login = postJson(
                "/api/v1/auth/login", "{\"username\":\"alice@example.com\",\"password\":\"correct-password\"}");
        var token = objectMapper.readTree(login.body()).path("accessToken").asText();

        var firstLogout = postWithBearer("/api/v1/auth/logout", token);
        var secondLogout = postWithBearer("/api/v1/auth/logout", token);

        assertThat(firstLogout.statusCode()).isEqualTo(204);
        assertThat(secondLogout.statusCode()).isEqualTo(204);
        assertThat(firstLogout.body()).isEmpty();
        assertThat(secondLogout.body()).isEmpty();
    }

    @Test
    void logoutRequiresBearerToken() throws Exception {
        var response = postWithBearer("/api/v1/auth/logout", "not-a-token");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(objectMapper.readTree(response.body()).path("type").asText())
                .isEqualTo("https://digital-bank-java.local/problems/invalid-token");
    }

    private HttpResponse<String> postJson(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithBearer(String path, String token) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode decodeJwtPayload(String token) throws Exception {
        var encodedPayload = token.split("\\.", -1)[1];
        return objectMapper.readTree(java.util.Base64.getUrlDecoder().decode(encodedPayload));
    }
}
