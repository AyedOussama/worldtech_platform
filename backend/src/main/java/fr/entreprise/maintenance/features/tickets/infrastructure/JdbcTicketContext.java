package fr.entreprise.maintenance.features.tickets.infrastructure;

import java.util.Optional;
import java.util.UUID;
import fr.entreprise.maintenance.features.tickets.publicapi.TicketContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTicketContext implements TicketContext {
    private final JdbcTemplate jdbc;
    public JdbcTicketContext(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public Optional<LinkedTicket> findSiteTicket(UUID accountId,UUID siteId,UUID ticketId) {
        return jdbc.query("SELECT ticket_id,equipment_id,qualified_at FROM app.ticket WHERE account_id=? AND site_id=? AND ticket_id=?",
                (r,i)->new LinkedTicket(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getTimestamp(3)!=null),accountId,siteId,ticketId).stream().findFirst();
    }
}
