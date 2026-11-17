package fr.entreprise.maintenance.features.audit.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Persistence mapping only: no HTTP endpoint or generic audit editing service. */
@Entity
@Immutable
@Table(name = "audit_event", schema = "app")
public class AuditEventRow {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "trace_id", nullable = false)
    private UUID traceId;

    protected AuditEventRow() {
    }

    public AuditEventRow(UUID eventId, Instant occurredAt, UUID actorUserId, String action,
            String resourceType, UUID resourceId, UUID traceId) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.actorUserId = actorUserId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.traceId = traceId;
    }
}
