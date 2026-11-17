package fr.entreprise.maintenance.bootstrap.identity;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import fr.entreprise.maintenance.MaintenanceApplication;
import fr.entreprise.maintenance.bootstrap.database.DatabaseMigrationRunner;
import fr.entreprise.maintenance.features.identity.application.AdministratorBootstrapService;
import org.slf4j.MDC;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.env.MapPropertySource;

/** Local controlled console, no public route, file/argument password, web server, migration or SMTP worker. */
public final class AdministratorBootstrapRunner {
    private AdministratorBootstrapRunner() { }

    public static void main(String[] args) {
        Console console = System.console();
        if (args.length != 0 || console == null) {
            System.err.println("Interactive operator console required; credentials are not accepted as arguments.");
            System.exit(1); return;
        }
        char[] password = null;
        char[] confirmation = null;
        int exit = 0;
        try {
            String email = console.readLine("Email nominatif du nouvel administrateur : ");
            password = console.readPassword("Mot de passe initial (non affiché) : ");
            confirmation = console.readPassword("Confirmer le mot de passe : ");
            if (password == null || confirmation == null || !Arrays.equals(password, confirmation)
                    || !"CREER ADMIN".equals(console.readLine("Confirmation opérateur : saisir CREER ADMIN : "))) {
                throw new IllegalArgumentException();
            }
            String jdbc = required("MAINT_JDBC_URL");
            DatabaseMigrationRunner.validateUrl(jdbc, "dev".equals(System.getenv("MAINT_BOOTSTRAP_ENV")));
            Path secrets = Path.of(required("MAINT_BOOTSTRAP_SECRETS_DIR"));
            String runtime = readSecret(secrets.resolve("runtime_password"));
            String encryption = readSecret(secrets.resolve("mfa_encryption_key"));
            var application = new SpringApplication(MaintenanceApplication.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setBannerMode(Banner.Mode.OFF);
            application.setLogStartupInfo(false);
            application.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("operator-files", Map.of("runtime_password", runtime, "mfa_encryption_key", encryption))));
            String trace = UUID.randomUUID().toString();
            MDC.put("traceId", trace);
            try (var context = application.run("--spring.main.web-application-type=none", "--spring.profiles.active=prod",
                    "--spring.config.import=", "--spring.flyway.enabled=false", "--app.mail.enabled=false",
                    "--spring.datasource.username=maint_app", "--spring.datasource.password=${runtime_password}", "--spring.datasource.url=" + jdbc)) {
                context.getBean(AdministratorBootstrapService.class).create(email, new String(password));
                console.printf("Compte créé ; email à vérifier et MFA à configurer. Aucun privilège avant ces preuves.%nRéférence d'audit : %s%n", trace);
            }
        } catch (Exception failure) {
            System.err.println("Administrator bootstrap refused: " + failure.getClass().getSimpleName());
            exit = 1;
        } finally {
            if (password != null) { Arrays.fill(password, '\0'); }
            if (confirmation != null) { Arrays.fill(confirmation, '\0'); }
            MDC.remove("traceId");
        }
        if (exit != 0) { System.exit(exit); }
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("Missing operator configuration."); }
        return value;
    }
    private static String readSecret(Path path) throws java.io.IOException {
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path) || Files.size(path) > 4096) {
            throw new IllegalArgumentException("Invalid operator secret file.");
        }
        String value = Files.readString(path).strip();
        if (value.isEmpty()) { throw new IllegalArgumentException("Empty operator secret file."); }
        return value;
    }
}
