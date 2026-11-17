package fr.entreprise.maintenance.testinfra;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class MailpitFixture {
    private MailpitFixture() { }
    public static GenericContainer<?> container() {
        Properties lock = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("infra/images.lock.env"))) { lock.load(reader); }
        catch (java.io.IOException failure) { throw new IllegalStateException("Real image lock required for SMTP tests."); }
        String image = lock.getProperty("MAILPIT_IMAGE", "");
        if (!image.matches("axllent/mailpit@sha256:[0-9a-f]{64}")) { throw new IllegalStateException("Mailpit digest required."); }
        return new GenericContainer<>(DockerImageName.parse(image)).withExposedPorts(1025, 8025)
                .withCreateContainerCmdModifier(command -> command.getHostConfig().withPortBindings(
                        new PortBinding(Ports.Binding.bindIp("127.0.0.1"), new ExposedPort(1025)),
                        new PortBinding(Ports.Binding.bindIp("127.0.0.1"), new ExposedPort(8025))))
                .waitingFor(Wait.forHttp("/livez").forPort(8025));
    }
}
