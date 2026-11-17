package fr.entreprise.maintenance.features.audit.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcSecurityAudit implements SecurityAudit {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public JdbcSecurityAudit(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }

    @Override
    public void record(Action action, UUID actor, UUID resource) {
        // Never include login input, credentials, tokens or provider diagnostics.
        String trace = org.slf4j.MDC.get("traceId");
        UUID traceId;
        try { traceId = trace == null ? UUID.randomUUID() : UUID.fromString(trace); }
        catch (IllegalArgumentException ignored) { traceId = UUID.randomUUID(); }
        jdbc.update("INSERT INTO app.audit_event(event_id,occurred_at,actor_user_id,action,resource_type,resource_id,trace_id) VALUES (?,?,?,?,?,?,?)",
                UUID.randomUUID(), Timestamp.from(clock.instant()), actor, action.name(),
                action==Action.ACCOUNT_CHANGED ? "client_account" : action==Action.ASSET_CHANGED ? "asset"
                        : action==Action.CONTRACT_CHANGED ? "contract" : action.name().startsWith("TICKET_") ? "ticket"
                        : action.name().startsWith("CLIENT_") ? "client_admission" : action.name().startsWith("TECHNICIAN_") ? "technician_admission" : "identity",resource,traceId);
    }
}
