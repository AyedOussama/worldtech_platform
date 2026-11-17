package fr.entreprise.maintenance.features.notifications.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import fr.entreprise.maintenance.shared.crypto.VersionedSecretCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class MailDeliveryWorker {
    private record Delivery(UUID id, UUID lease, String recipient, String subject, String encrypted, int attempts, Instant expires) {
        @Override public String toString() { return "Delivery[redacted]"; }
    }
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final VersionedSecretCipher cipher;
    private final ObjectProvider<JavaMailSender> mail;
    private final Clock clock;
    private final boolean enabled;
    private final String from;

    public MailDeliveryWorker(JdbcTemplate jdbc, PlatformTransactionManager manager, VersionedSecretCipher cipher,
            ObjectProvider<JavaMailSender> mail, Clock clock, @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:maintenance@localhost}") String from) {
        this.jdbc = jdbc; this.tx = new TransactionTemplate(manager); this.cipher = cipher;
        this.mail = mail; this.clock = clock; this.enabled = enabled; this.from = from;
    }

    @Scheduled(fixedDelayString = "${app.mail.poll-millis:5000}")
    public void poll() {
        if (enabled) { for (int i = 0; i < 10 && deliverOne(); i++) { /* bounded batch */ } }
    }

    /** Internal operation, also exercised against a local SMTP test server. At-least-once delivery. */
    public boolean deliverOne() {
        Instant now = clock.instant();
        Delivery delivery = tx.execute(status -> {
            var rows = jdbc.query("SELECT * FROM app.mail_outbox WHERE (state='PENDING' AND available_at<=?) "
                            + "OR (state='IN_FLIGHT' AND lease_until<=?) ORDER BY available_at LIMIT 1 FOR UPDATE SKIP LOCKED",
                    (row, index) -> new Delivery(row.getObject("message_id", UUID.class), UUID.randomUUID(), row.getString("recipient"),
                            row.getString("subject"), row.getString("encrypted_body"), row.getInt("attempts") + 1,
                            row.getTimestamp("expires_at").toInstant()), ts(now), ts(now));
            if (rows.isEmpty()) { return null; }
            Delivery selected = rows.getFirst();
            jdbc.update("UPDATE app.mail_outbox SET state='IN_FLIGHT',lease_id=?,lease_until=?,attempts=? WHERE message_id=?",
                    selected.lease(), ts(now.plusSeconds(60)), selected.attempts(), selected.id());
            return selected;
        });
        if (delivery == null) { return false; }
        if (!delivery.expires().isAfter(now)) {
            jdbc.update("UPDATE app.mail_outbox SET state='EXPIRED',encrypted_body=NULL,lease_until=NULL WHERE message_id=? AND lease_id=?",
                    delivery.id(), delivery.lease());
            return true;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(delivery.recipient());
            message.setSubject(delivery.subject());
            message.setText(cipher.decrypt(delivery.encrypted(), JdbcMailOutbox.context(delivery.id(), delivery.recipient(), delivery.subject())));
            JavaMailSender sender = mail.getIfAvailable();
            if (sender == null) { throw new IllegalStateException("Mail transport unavailable."); }
            sender.send(message); // No database transaction or user lock here.
            jdbc.update("UPDATE app.mail_outbox SET state='DELIVERED',encrypted_body=NULL,delivered_at=?,lease_until=NULL "
                    + "WHERE message_id=? AND lease_id=?", ts(clock.instant()), delivery.id(), delivery.lease());
        } catch (RuntimeException failure) {
            // Provider exception messages can contain the recipient or entire mail; never persist/log them.
            jdbc.update("UPDATE app.mail_outbox SET state=?,available_at=?,lease_until=NULL, "
                            + "encrypted_body=CASE WHEN attempts>=5 THEN NULL ELSE encrypted_body END WHERE message_id=? AND lease_id=?",
                    delivery.attempts() >= 5 ? "FAILED" : "PENDING", ts(clock.instant().plusSeconds(Math.min(3600, 30L << Math.min(7, delivery.attempts())))),
                    delivery.id(), delivery.lease());
        }
        return true;
    }

    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
}
