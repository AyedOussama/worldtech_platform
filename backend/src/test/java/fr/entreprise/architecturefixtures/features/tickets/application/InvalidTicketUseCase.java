package fr.entreprise.architecturefixtures.features.tickets.application;

import fr.entreprise.architecturefixtures.features.clients.infrastructure.ClientRepository;

public record InvalidTicketUseCase(ClientRepository repository) {
}
