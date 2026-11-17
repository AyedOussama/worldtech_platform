package fr.entreprise.maintenance.features.notifications.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import fr.entreprise.maintenance.features.notifications.publicapi.BusinessEvents;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class JdbcBusinessEvents implements BusinessEvents {
    private final JdbcTemplate jdbc;
    public JdbcBusinessEvents(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void append(Kind kind,UUID account,UUID site,UUID resource,long version,UUID actor,Instant occurredAt) {
        jdbc.update("INSERT INTO app.business_event_outbox(event_id,account_id,site_id,resource_id,resource_version,event_kind,actor_user_id,occurred_at) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID(),account,site,resource,version,kind.name(),actor,Timestamp.from(occurredAt));
    }
}
