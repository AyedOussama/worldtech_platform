package fr.entreprise.maintenance.features.notifications.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import fr.entreprise.maintenance.features.notifications.publicapi.MailOutbox;
import fr.entreprise.maintenance.shared.crypto.VersionedSecretCipher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcMailOutbox implements MailOutbox {
    private final JdbcTemplate jdbc;
    private final VersionedSecretCipher cipher;
    private final Clock clock;
    public JdbcMailOutbox(JdbcTemplate jdbc, VersionedSecretCipher cipher, Clock clock) {
        this.jdbc = jdbc; this.cipher = cipher; this.clock = clock;
    }

    @Override
    public void enqueue(String recipient, String subject, String body, Instant expiresAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO app.mail_outbox(message_id,recipient,subject,encrypted_body,state,created_at,expires_at,available_at) "
                        + "VALUES (?,?,?,?,'PENDING',?,?,?)", id, recipient, subject, cipher.encrypt(body, context(id, recipient, subject)),
                Timestamp.from(clock.instant()), Timestamp.from(expiresAt), Timestamp.from(clock.instant()));
    }

    static String context(UUID id, String recipient, String subject) {
        return "mail-outbox/" + id + "/" + recipient.length() + ":" + recipient + "/" + subject;
    }
}
