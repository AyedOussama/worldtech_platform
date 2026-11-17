package fr.entreprise.maintenance.bootstrap.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

class MailSchedulingConfigurationTest {
    @Test
    void schedulerIsEnabledOnlyWhenMailDeliveryIsExplicitlyEnabled() {
        var runner = new ApplicationContextRunner().withUserConfiguration(MailSchedulingConfiguration.class);
        runner.run(context -> assertThat(context.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class)).isEmpty());
        runner.withPropertyValues("app.mail.enabled=true").run(context ->
                assertThat(context.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class)).hasSize(1));
    }
}
