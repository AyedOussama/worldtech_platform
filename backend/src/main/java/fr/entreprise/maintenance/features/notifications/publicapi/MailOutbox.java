package fr.entreprise.maintenance.features.notifications.publicapi;

import java.time.Instant;

/** Enqueue in the caller's transaction. Never perform SMTP while holding its locks. */
public interface MailOutbox {
    void enqueue(String recipient, String subject, String body, Instant expiresAt);
}
