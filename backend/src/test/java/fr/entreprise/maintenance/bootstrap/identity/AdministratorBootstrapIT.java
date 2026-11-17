package fr.entreprise.maintenance.bootstrap.identity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import fr.entreprise.maintenance.features.identity.application.AdministratorBootstrapService;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdministratorBootstrapIT {
    @Container static final GenericContainer<?> DATABASE = PostgisFixture.container();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { PostgisFixture.properties(registry, DATABASE); }
    @Autowired ApplicationContext context;
    @TempDir Path temporary;

    @Test
    void operatorApplicationCanStartWithoutHttpOrScheduledMail() {
        assertThat(context.getBean(AdministratorBootstrapService.class)).isNotNull();
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).isEmpty();
        assertThat(context.getBeansOfType(org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.class)).isEmpty();
        assertThat(context.getClass().getName()).doesNotContain("WebServer");
    }

    @Test
    void packagedEntryPointRefusesNonInteractiveExecutionBeforeStartingAnything() throws Exception {
        refused(java.util.List.of());
    }

    @Test
    void packagedEntryPointDoesNotAcceptOperatorArguments() throws Exception {
        refused(java.util.List.of("--not-an-operator-input"));
    }

    private void refused(java.util.List<String> arguments) throws Exception {
        Path executable = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        var command = new ArrayList<>(java.util.List.of(executable.toString(), "-Xmx128m",
                "-Dloader.main=fr.entreprise.maintenance.bootstrap.identity.AdministratorBootstrapRunner", "-cp",
                Path.of("target/maintenance-api.jar").toAbsolutePath().toString(), "org.springframework.boot.loader.launch.PropertiesLauncher"));
        command.addAll(arguments);
        Path output = temporary.resolve("operator-result.txt");
        Process child = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        child.getOutputStream().close();
        if (!child.waitFor(20, TimeUnit.SECONDS)) { child.destroyForcibly(); throw new AssertionError("Operator process did not stop."); }
        assertThat(child.exitValue()).isEqualTo(1);
        String result = Files.readString(output);
        assertThat(result).contains("Interactive operator console required").doesNotContain("Tomcat", "HikariPool", "Exception", "--not-an-operator-input");
    }
}
