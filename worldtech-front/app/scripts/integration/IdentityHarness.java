import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import fr.entreprise.maintenance.MaintenanceApplication;
import fr.entreprise.maintenance.features.identity.application.AdministratorBootstrapService;
import fr.entreprise.maintenance.features.identity.application.LoginAttempts;
import fr.entreprise.maintenance.features.identity.domain.ActionTokens;
import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.IdentityRole;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.PasswordHasher;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Frontend test fixture: real Spring/Tomcat/PostGIS, no HTTP test backdoor. */
public final class IdentityHarness {
  public static void main(String[] args) throws Exception {
    var input = new BufferedReader(new InputStreamReader(System.in, java.nio.charset.StandardCharsets.UTF_8));
    var config = JsonMapper.builder().build().readTree(input.readLine());
    try (var database = PostgisFixture.container()) {
      database.start();
      Map<String, Object> properties = new HashMap<>();
      PostgisFixture.properties((name, supplier) -> properties.put(name, supplier.get()), database);
      properties.put("server.port", "18083"); properties.put("server.address", "127.0.0.1");
      properties.put("app.mail.enabled", "false"); properties.put("app.identity.google.enabled", "false");
      properties.put("app.identity.frontend-origin", "http://localhost:4311");
      properties.put("logging.level.root", "ERROR"); properties.put("spring.main.banner-mode", "off");
      var application = new SpringApplication(MaintenanceApplication.class);
      application.setAdditionalProfiles("test");
      application.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("frontend-isolated-fixture", properties)));
      try (var context = application.run()) {
        var store = context.getBean(IdentityStore.class); var hasher = context.getBean(PasswordHasher.class);
        var jdbc = context.getBean(JdbcTemplate.class);
        var address = EmailAddress.parse(config.get("clientEmail").asText());
        UUID client = store.createPending(address.normalized(), address.display(), hasher.encode(config.get("clientPassword").asText()), IdentityRole.CLIENT, Instant.now()).userId();
        jdbc.update("UPDATE app.app_user SET status='ACTIVE' WHERE user_id=?", client);
        jdbc.update("UPDATE app.local_credential SET email_verified_at=now() WHERE user_id=?", client);
        UUID admin = context.getBean(AdministratorBootstrapService.class).create(config.get("adminEmail").asText(), config.get("adminPassword").asText());
        jdbc.update("UPDATE app.local_credential SET email_verified_at=now() WHERE user_id=?", admin);
        UUID replacementAdmin = context.getBean(AdministratorBootstrapService.class).create("replacement-admin-fe03@example.test", config.get("adminPassword").asText());
        jdbc.update("UPDATE app.local_credential SET email_verified_at=now() WHERE user_id=?", replacementAdmin);
        store.createPending("pending-fe03@example.test", "pending-fe03@example.test", hasher.encode(config.get("clientPassword").asText()), IdentityRole.CLIENT, Instant.now());
        var actions = context.getBean(ActionTokens.class);
        var now = Instant.now();
        String reset = config.get("resetToken").asText();
        String verification = config.get("verificationToken").asText();
        // Issue through the real domain port, not SQL or an invented test endpoint.
        actions.issue(address.normalized(), ActionTokens.Purpose.PASSWORD_RESET, LoginAttempts.digest(reset), reset, now, now.plusSeconds(1800));
        actions.issue("pending-fe03@example.test", ActionTokens.Purpose.EMAIL_VERIFICATION, LoginAttempts.digest(verification), verification, now, now.plusSeconds(86400));
        System.out.println("WT_IDENTITY_READY"); System.out.flush();
        while (input.readLine() != null) { /* Parent owns fixture lifetime, no commands exposed over HTTP. */ }
      }
    }
  }
}
