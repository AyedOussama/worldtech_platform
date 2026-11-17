package fr.entreprise.maintenance.bootstrap.database;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseMigrationRunnerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://db:5432/maintenance?sslmode=verify-full",
            "jdbc:postgresql://db/maintenance?sslmode=verify-full&sslrootcert=/run/certs/root.crt&connectTimeout=3"
    })
    void productionRequiresExplicitCertificateAndHostnameVerification(String url) {
        assertThatCode(() -> DatabaseMigrationRunner.validateUrl(url, false)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://127.0.0.1:5432/maintenance",
            "jdbc:postgresql://db:5432/maintenance?connectTimeout=3&socketTimeout=10"
    })
    void explicitlySelectedDevelopmentCanUseTheLocalNonTlsDatabase(String url) {
        assertThatCode(() -> DatabaseMigrationRunner.validateUrl(url, true)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://db/maintenance",
            "jdbc:postgresql://db/maintenance?sslmode=require",
            "jdbc:postgresql://db/maintenance?sslmode=disable",
            "jdbc:postgresql://db/maintenance?%73slmode=verify-full",
            "jdbc:postgresql://db/maintenance?sslmode=verify-full&sslmode=disable",
            "jdbc:postgresql://db/maintenance?sslmode=disable&sslmode=verify-full",
            "jdbc:postgresql://db/maintenance?sslmode=verify-full&sslfactory=org.postgresql.ssl.NonValidatingFactory",
            "jdbc:postgresql://db/maintenance?sslmode=verify-full&sslhostnameverifier=custom.Verifier",
            "jdbc:postgresql://db/maintenance?sslmode=verify-full&password=synthetic-not-a-secret",
            "jdbc:postgresql://db/maintenance?sslmode=verify-full&%75ser=postgres",
            "jdbc:postgresql://db/maintenance?sslmode=verify-full&service=override",
            "jdbc:postgresql://user@db/maintenance?sslmode=verify-full",
            "jdbc:postgresql://db/maintenance?sslmode=verify-full#fragment",
            "jdbc:h2:mem:maintenance"
    })
    void invalidOrOverridingProductionUrlsAreRejected(String url) {
        assertThatThrownBy(() -> DatabaseMigrationRunner.validateUrl(url, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
