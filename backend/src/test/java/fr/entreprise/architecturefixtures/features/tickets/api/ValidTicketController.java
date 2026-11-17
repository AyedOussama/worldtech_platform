package fr.entreprise.architecturefixtures.features.tickets.api;

import fr.entreprise.architecturefixtures.features.tickets.application.ValidTicketUseCase;

public record ValidTicketController(ValidTicketUseCase useCase) {
}
