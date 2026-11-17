package fr.entreprise.maintenance.bootstrap.notifications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
@EnableScheduling
public class MailSchedulingConfiguration {
}
