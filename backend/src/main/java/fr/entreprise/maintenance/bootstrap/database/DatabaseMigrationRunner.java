package fr.entreprise.maintenance.bootstrap.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import org.flywaydb.core.Flyway;

/** One-shot entry point: no web server, Spring context, runtime secret or superuser. */
public final class DatabaseMigrationRunner {

    private DatabaseMigrationRunner() {
    }

    public static void main(String[] args) {
        try {
            if (args.length != 0) {
                throw new IllegalArgumentException("Migration arguments are not accepted.");
            }
            String url = required("MAINT_JDBC_URL");
            boolean development = "dev".equals(System.getenv("MAINT_MIGRATION_ENV"));
            validateUrl(url, development);
            Path file = Path.of(required("MAINT_MIGRATOR_PASSWORD_FILE"));
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("Missing migrator secret file.");
            }
            String password = Files.readString(file).strip();
            if (password.isEmpty()) {
                throw new IllegalArgumentException("Empty migrator secret file.");
            }
            var result = Flyway.configure().dataSource(url, "maint_migrator", password)
                    .defaultSchema("app").schemas("app").createSchemas(false)
                    .locations("classpath:db/migration").validateMigrationNaming(true)
                    .validateOnMigrate(true).baselineOnMigrate(false).cleanDisabled(true)
                    .load().migrate();
            System.out.println("Migration complete; applied=" + result.migrationsExecuted);
        } catch (Exception exception) {
            // Do not echo JDBC URLs, credential values or provider exception messages.
            System.err.println("Migration failed: " + exception.getClass().getSimpleName());
            System.exit(1);
        }
    }

    public static void validateUrl(String url, boolean development) {
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("A PostgreSQL network URL is required.");
        }
        URI parsed = URI.create(url.substring("jdbc:".length()));
        if (parsed.getHost() == null || parsed.getRawUserInfo() != null || parsed.getRawFragment() != null) {
            throw new IllegalArgumentException("Invalid PostgreSQL network URL.");
        }
        // Deliberately narrow: no service files, custom TLS factories/verifiers or URL credentials.
        Set<String> allowed = Set.of("sslmode", "sslrootcert", "connectTimeout", "socketTimeout", "ApplicationName");
        var parameters = new HashMap<String, String>();
        if (parsed.getRawQuery() != null) {
            for (String pair : parsed.getRawQuery().split("&", -1)) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                // pgjdbc decodes values but not property names: do not validate a different key.
                if (!parts[0].equals(key) || !allowed.contains(key) || value.isBlank()
                        || parameters.putIfAbsent(key.toLowerCase(Locale.ROOT), value) != null) {
                    throw new IllegalArgumentException("Unsupported or duplicate JDBC parameter.");
                }
            }
        }
        if (!development && !"verify-full".equals(parameters.get("sslmode"))) {
            throw new IllegalArgumentException("Production migration requires verified TLS.");
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing migration configuration.");
        }
        return value;
    }
}
