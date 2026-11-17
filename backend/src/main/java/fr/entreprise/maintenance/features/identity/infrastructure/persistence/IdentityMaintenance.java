package fr.entreprise.maintenance.features.identity.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Small bounded cleanup; expiration checks in commands remain authoritative if this job stops. */
@Component
public class IdentityMaintenance {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final boolean enabled;
    public IdentityMaintenance(JdbcTemplate jdbc, Clock clock, @Value("${app.identity.maintenance-enabled:false}") boolean enabled) {
        this.jdbc = jdbc; this.clock = clock; this.enabled = enabled;
    }
    @Scheduled(fixedDelayString = "${app.identity.maintenance-millis:60000}", initialDelayString = "${app.identity.maintenance-millis:60000}")
    public void poll() { if (enabled) { sweep(); } }
    public void sweep() {
        Timestamp now = Timestamp.from(clock.instant());
        jdbc.update("DELETE FROM app.auth_rate_bucket WHERE (bucket_key,window_start) IN "
                + "(SELECT bucket_key,window_start FROM app.auth_rate_bucket WHERE expires_at<=? LIMIT 500 FOR UPDATE SKIP LOCKED)", now);
        jdbc.update("UPDATE app.mfa_credential SET pending_secret=NULL,pending_expires_at=NULL,enrollment_id=NULL,pending_session_id=NULL,pending_replacement=false "
                + "WHERE user_id IN (SELECT user_id FROM app.mfa_credential WHERE pending_expires_at<=? LIMIT 500 FOR UPDATE SKIP LOCKED)", now);
        jdbc.update("UPDATE app.google_oauth_transaction SET authorization_encrypted=NULL WHERE transaction_id IN "
                + "(SELECT transaction_id FROM app.google_oauth_transaction WHERE expires_at<=? AND authorization_encrypted IS NOT NULL LIMIT 500 FOR UPDATE SKIP LOCKED)", now);
        // Deliberately do not delete consumed refresh records: replay detection needs the full family history.
    }
}
