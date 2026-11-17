package fr.entreprise.maintenance.testinfra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/** Real, isolated DB. No embedded substitute, public host binding or password in container ENV. */
public final class PostgisFixture {

    private static final String BOOTSTRAP = randomSecret();
    private static final String MIGRATOR = randomSecret();
    private static final String RUNTIME = randomSecret();
    private static final String ENCRYPTION_KEY = randomSecret();

    private PostgisFixture() {
    }

    public static GenericContainer<?> container() {
        return new GenericContainer<>(DockerImageName.parse(lockedImage()))
                .withEnv("POSTGRES_DB", "maintenance")
                .withEnv("POSTGRES_USER", "postgres")
                .withEnv("POSTGRES_PASSWORD_FILE", "/run/secrets/postgres_password")
                .withEnv("POSTGRES_INITDB_ARGS", "--auth-host=scram-sha-256")
                .withEnv("TZ", "UTC")
                .withCopyToContainer(Transferable.of(BOOTSTRAP.getBytes(java.nio.charset.StandardCharsets.UTF_8), 0444),
                        "/run/secrets/postgres_password")
                // Regression: existing Windows secret files may use CRLF; bootstrap must strip it.
                .withCopyToContainer(Transferable.of((MIGRATOR + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8), 0444),
                        "/run/secrets/migrator_password")
                .withCopyToContainer(Transferable.of((RUNTIME + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8), 0444),
                        "/run/secrets/runtime_password")
                .withCopyFileToContainer(MountableFile.forHostPath(
                        Path.of("infra/postgres/init.sh").toAbsolutePath(), 0444),
                        "/docker-entrypoint-initdb.d/10-maintenance.sh")
                .withExposedPorts(5432)
                .withCreateContainerCmdModifier(command -> command.getHostConfig().withPortBindings(
                        new PortBinding(Ports.Binding.bindIp("127.0.0.1"), new ExposedPort(5432))))
                // A bound port can precede PostgreSQL accepting queries (57P03 during startup).
                // Probe the final TCP server with the real migrator, not the temporary init socket.
                // Only the ephemeral psql process reads its mounted secret; no value in Docker config/arguments.
                .waitingFor(new WaitAllStrategy().withStrategy(Wait.forListeningPort())
                        .withStrategy(Wait.forSuccessfulCommand("PGPASSWORD=\"$(tr -d '\\r\\n' </run/secrets/migrator_password)\" "
                                + "psql -h 127.0.0.1 -U maint_migrator -d maintenance --no-password --tuples-only --command='SELECT 1' >/dev/null")))
                .withStartupTimeout(Duration.ofSeconds(120));
    }

    public static void properties(DynamicPropertyRegistry registry, GenericContainer<?> database) {
        registry.add("app.identity.crypto.keys.v1", () -> ENCRYPTION_KEY);
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> "1"); // No real SMTP destination in DB/HTTP fixtures.
        registry.add("spring.datasource.url", () -> url(database));
        registry.add("spring.datasource.username", () -> "maint_app");
        registry.add("spring.datasource.password", () -> RUNTIME);
        registry.add("spring.flyway.url", () -> url(database));
        registry.add("spring.flyway.user", () -> "maint_migrator");
        registry.add("spring.flyway.password", () -> MIGRATOR);
    }

    public static String url(GenericContainer<?> database) {
        return "jdbc:postgresql://127.0.0.1:" + database.getMappedPort(5432)
                + "/maintenance?connectTimeout=3&socketTimeout=10";
    }

    public static Connection runtime(GenericContainer<?> database) throws SQLException {
        return DriverManager.getConnection(url(database), "maint_app", RUNTIME);
    }

    public static Connection migrator(GenericContainer<?> database) throws SQLException {
        return DriverManager.getConnection(url(database), "maint_migrator", MIGRATOR);
    }

    public static org.flywaydb.core.api.configuration.FluentConfiguration migration(GenericContainer<?> database) {
        return org.flywaydb.core.Flyway.configure().dataSource(url(database), "maint_migrator", MIGRATOR)
                .defaultSchema("app").schemas("app").createSchemas(false).cleanDisabled(true);
    }

    private static String lockedImage() {
        Properties lock = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("infra/images.lock.env"))) {
            lock.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Missing image lock; run scripts/lock-images.py before integration tests.");
        }
        String image = lock.getProperty("DB_IMAGE", "");
        if (!image.matches("postgis/postgis@sha256:[0-9a-f]{64}")) {
            throw new IllegalStateException("Integration tests require a real locked PostGIS digest.");
        }
        return image;
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
