package fr.entreprise.maintenance.bootstrap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import fr.entreprise.maintenance.shared.http.RequestTrace;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SkeletonServerIT {

    @Container
    static final GenericContainer<?> DATABASE = PostgisFixture.container();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgisFixture.properties(registry, DATABASE);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper mapper;

    @Test
    void realServerServesOnlyTheMinimalHealthResponse() throws Exception {
        HttpResponse<String> response = send("/health", "application/json");
        assertThat(response.statusCode()).isEqualTo(200);
        var contract = mapper.readTree(Path.of("openapi", "maintenance-api.json").toFile());
        assertThat(contract.path("openapi").asString()).isEqualTo("3.1.0");
        assertThat(contract.path("paths").has("/api/web/v1/auth/login")).isTrue();
        assertThat(contract.path("paths").has("/api/mobile/v1/auth/refresh")).isTrue();
        var example = contract.path("paths").path("/health").path("get").path("responses")
                .path("200").path("content").path("application/json").path("example");
        assertThat(example.isObject()).isTrue();
        assertThat(mapper.readTree(response.body())).isEqualTo(example);
        assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void realServerRejectsPrivateReadsWithoutHtmlOrRedirect() throws Exception {
        HttpResponse<String> response = send("/api/web/v1/client/tickets?diagnostic=caller-marker", "text/html");
        assertProblem(response, 401, "AUTHENTICATION_REQUIRED");
        assertThat(response.body()).doesNotContain("caller-marker", "<html", "Exception");
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void realServerRejectsMutationsWithoutCsrf() throws Exception {
        try (HttpClient client = client()) {
            var request = HttpRequest.newBuilder(uri("/health")).timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .header("Content-Type", "application/json").build();
            assertProblem(client.send(request, HttpResponse.BodyHandlers.ofString()), 403, "ACCESS_DENIED");
        }
    }

    @Test
    void realServerNegotiationFailureIsSanitizedJson() throws Exception {
        assertProblem(send("/health", "text/html"), 406, "NOT_ACCEPTABLE");
    }

    @Test
    void firewallRejectionUsesProblemJsonRatherThanContainerHtml() throws Exception {
        assertProblem(send("/health;untrusted=marker", "application/json"), 400, "INVALID_REQUEST");
    }

    private HttpResponse<String> send(String path, String accept) throws Exception {
        try (HttpClient client = client()) {
            var request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                    .header("Accept", accept).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private void assertProblem(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/problem+json");
        var body = mapper.readTree(response.body());
        var contract = mapper.readTree(Path.of("openapi", "maintenance-api.json").toFile());
        for (var field : contract.path("components").path("schemas").path("ProblemDetail").path("required")) {
            assertThat(body.has(field.asString())).as("Required problem field %s", field.asString()).isTrue();
        }
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("code").asString()).isEqualTo(code);
        String trace = response.headers().firstValue(RequestTrace.HEADER).orElseThrow();
        assertThat(UUID.fromString(trace)).isNotNull();
        assertThat(body.path("traceId").asString()).isEqualTo(trace);
        assertThat(body.has("exception")).isFalse();
        assertThat(body.has("trace")).isFalse();
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static HttpClient client() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }
}
