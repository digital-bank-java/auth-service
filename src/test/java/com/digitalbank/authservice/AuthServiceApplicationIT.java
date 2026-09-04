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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServiceApplicationIT {

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> "false");
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Test
    void healthEndpointReportsUp() throws Exception {
        var response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(value -> {
            assertThat(value).startsWith("application/vnd.spring-boot.actuator");
        });
        assertThat(objectMapper.readTree(response.body()).path("status").asText())
                .isEqualTo("UP");
    }

    @Test
    void openApiEndpointReturnsExplicitMetadata() throws Exception {
        var response = get("/v3/api-docs");
        JsonNode document = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(value -> {
            assertThat(value).startsWith("application/json");
        });
        assertThat(document.path("info").path("title").asText())
                .isEqualTo("Digital Bank Authentication and Session Service API");
        assertThat(document.path("info").path("version").asText()).isEqualTo("1.0.0");
        assertThat(document.path("info").path("description").asText()).contains("Internal authentication");
        assertThat(document.path("paths")
                        .path("/api/v1/auth/login")
                        .path("post")
                        .path("responses")
                        .has("200"))
                .isTrue();
        assertThat(document.path("paths")
                        .path("/api/v1/auth/logout")
                        .path("post")
                        .path("responses")
                        .has("204"))
                .isTrue();
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
