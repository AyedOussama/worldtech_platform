package fr.entreprise.architecturefixtures.features.tickets.api;

import java.util.List;

import fr.entreprise.architecturefixtures.features.clients.infrastructure.ClientRepository;

public interface InvalidTicketController {
    List<ClientRepository> leak();
}
