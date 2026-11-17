package fr.entreprise.architecturefixtures.features.tickets.publicapi;

import fr.entreprise.architecturefixtures.features.clients.publicapi.ClientCyclePort;

public interface TicketCyclePort {
    ClientCyclePort cyclicDependency();
}
