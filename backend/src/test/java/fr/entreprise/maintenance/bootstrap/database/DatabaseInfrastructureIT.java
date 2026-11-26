package fr.entreprise.maintenance.bootstrap.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;
import fr.entreprise.maintenance.features.audit.infrastructure.persistence.AuditEventRow;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DatabaseInfrastructureIT {

    @Container
    static final GenericContainer<?> DATABASE = PostgisFixture.container();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgisFixture.properties(registry, DATABASE);
    }

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private Flyway flyway;

    @Autowired
    private ApplicationContext context;

    @Test
    void realPostgresAndPostgisRunWithRuntimeRatherThanBootstrapIdentity() throws Exception {
        try (Connection connection = PostgisFixture.runtime(DATABASE)) {
            assertThat(scalar(connection, "SHOW server_version")).startsWith("18.");
            assertThat(scalar(connection, "SELECT public.PostGIS_Lib_Version() ")).startsWith("3.6.");
            assertThat(scalar(connection, "SELECT current_user")).isEqualTo("maint_app");
            assertThat(scalar(connection, "SELECT public.ST_DWithin(public.ST_GeogFromText('POINT(2 48)'), "
                    + "public.ST_GeogFromText('POINT(2 48)'), 1)::text")).isEqualTo("true");
            assertThat(scalar(connection, "SELECT (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication "
                    + "OR rolbypassrls)::text FROM pg_roles WHERE rolname = current_user")).isEqualTo("false");
        }
        assertThat(DATABASE.getContainerInfo().getNetworkSettings().getPorts()
                .getBindings().values()).allSatisfy(bindings -> {
                    assertThat(bindings).allSatisfy(binding -> assertThat(binding.getHostIp()).isEqualTo("127.0.0.1"));
                });
    }

    @Test
    void secondMigrationRunDoesNotRewriteHistory() throws Exception {
        String before;
        try (Connection connection = PostgisFixture.migrator(DATABASE)) {
            before = scalar(connection, "SELECT version || ':' || checksum || ':' || installed_on::text "
                    + "FROM app.flyway_schema_history WHERE success AND version = '1'");
        }
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try (Connection connection = PostgisFixture.migrator(DATABASE)) {
            assertThat(scalar(connection, "SELECT version || ':' || checksum || ':' || installed_on::text "
                    + "FROM app.flyway_schema_history WHERE success AND version = '1'")).isEqualTo(before);
            assertThat(scalar(connection, "SELECT count(*)::text FROM app.flyway_schema_history")).isEqualTo("18");
        }
    }

    @Test
    void runtimeCanInsertAndReadAuditThroughRealJpaWithoutDdl() throws Exception {
        UUID id = UUID.randomUUID();
        try (var entityManager = entityManagerFactory.createEntityManager()) {
            var transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.persist(new AuditEventRow(id, Instant.now(), null,
                    "INFRA_PROBE", "infrastructure", null, UUID.randomUUID()));
            transaction.commit();
        }
        try (Connection connection = PostgisFixture.runtime(DATABASE);
                var statement = connection.prepareStatement("SELECT action FROM app.audit_event WHERE event_id = ?")) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("INFRA_PROBE");
            }
        }
        assertThat(entityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto")).isEqualTo("validate");
        assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE TABLE app.forbidden_probe (id uuid)",
            "CREATE TABLE public.forbidden_probe (id uuid)",
            "CREATE TEMP TABLE forbidden_probe (id uuid)",
            "CREATE SCHEMA forbidden_probe",
            "ALTER TABLE app.audit_event ADD COLUMN forbidden_probe text",
            "SET ROLE maint_migrator",
            "SET ROLE postgres",
            "SELECT * FROM app.flyway_schema_history",
            "UPDATE app.flyway_schema_history SET checksum = checksum",
            "DELETE FROM app.flyway_schema_history WHERE false",
            "DROP TABLE app.flyway_schema_history"
    })
    void runtimeCannotCreateObjectsEscalateOrTouchMigrationHistory(String sql) throws Exception {
        assertPermissionDenied(sql);
    }

    @Test
    void auditCannotBeUpdatedDeletedOrTruncatedEvenWhenAnExistingRowIsTargeted() throws Exception {
        UUID id = UUID.randomUUID();
        try (Connection connection = PostgisFixture.runtime(DATABASE);
                var statement = connection.prepareStatement("INSERT INTO app.audit_event "
                        + "(event_id, occurred_at, action, resource_type, trace_id) VALUES (?, now(), ?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setString(2, "IMMUTABLE_INFRA_PROBE");
            statement.setString(3, "infrastructure");
            statement.setObject(4, UUID.randomUUID());
            statement.executeUpdate();
        }
        assertPermissionDenied("UPDATE app.audit_event SET action = 'FORBIDDEN'");
        assertPermissionDenied("DELETE FROM app.audit_event");
        assertPermissionDenied("TRUNCATE app.audit_event");
        try (Connection connection = PostgisFixture.runtime(DATABASE);
                var statement = connection.prepareStatement("SELECT action FROM app.audit_event WHERE event_id = ?")) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("IMMUTABLE_INFRA_PROBE");
            }
        }
    }

    @Test
    void newlyCreatedTablesDoNotReceiveAnAutomaticRuntimeGrant() throws Exception {
        try (Connection connection = PostgisFixture.migrator(DATABASE);
                var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE app.explicit_grant_probe (id uuid)");
            try {
                assertPermissionDenied("SELECT * FROM app.explicit_grant_probe");
            } finally {
                statement.execute("DROP TABLE app.explicit_grant_probe");
            }
        }
    }

    @Test
    void modifyingAnAppliedMigrationIsDetectedWithoutChangingTheRealFile(@TempDir Path directory) throws Exception {
        String original = Files.readString(Path.of("src/main/resources/db/migration/V1__initial_audit_schema.sql"));
        Files.writeString(directory.resolve("V1__initial_audit_schema.sql"), original + "\nSELECT 1;\n");
        var result = PostgisFixture.migration(DATABASE).locations("filesystem:" + directory.toAbsolutePath())
                .load().validateWithResult();
        assertThat(result.validationSuccessful).isFalse();
        assertThat(result.invalidMigrations.stream().anyMatch(invalid ->
                invalid.errorDetails.errorCode.toString().equals("CHECKSUM_MISMATCH"))).isTrue();
    }

    private static void assertPermissionDenied(String sql) throws Exception {
        try (Connection connection = PostgisFixture.runtime(DATABASE);
                var statement = connection.createStatement()) {
            // Each denied statement has its own connection/autocommit transaction.
            assertThatThrownBy(() -> statement.execute(sql)).isInstanceOf(SQLException.class)
                    .satisfies(exception -> assertThat(((SQLException) exception).getSQLState()).isEqualTo("42501"));
        }
    }

    private static String scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertThat(rows.next()).isTrue();
            return rows.getString(1);
        }
    }
}
