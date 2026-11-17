package fr.entreprise.architecturefixtures.features.clients.publicapi;

import fr.entreprise.architecturefixtures.features.tickets.publicapi.TicketCyclePort;

public interface ClientCyclePort {
    TicketCyclePort cyclicDependency();
}
