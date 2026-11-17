package fr.entreprise.maintenance.shared.http;

import java.net.URI;
import java.util.Locale;

enum ProblemCode {
    INVALID_REQUEST("La requête est invalide."),
    AUTHENTICATION_REQUIRED("Une authentification valide est nécessaire."),
    ACCESS_DENIED("Cette opération n'est pas autorisée."),
    RESOURCE_NOT_FOUND("La ressource demandée est introuvable."),
    METHOD_NOT_ALLOWED("Cette méthode HTTP n'est pas autorisée."),
    NOT_ACCEPTABLE("Le format de réponse demandé n'est pas disponible."),
    PAYLOAD_TOO_LARGE("La taille de la requête dépasse la limite autorisée."),
    UNSUPPORTED_MEDIA_TYPE("Le type de contenu de la requête n'est pas accepté."),
    TOO_MANY_REQUESTS("La limite de requêtes est atteinte."),
    REQUEST_REJECTED("La requête ne peut pas être traitée."),
    INTERNAL_ERROR("Une erreur interne est survenue.");

    private final String detail;

    ProblemCode(String detail) {
        this.detail = detail;
    }

    String detail() {
        return detail;
    }

    URI type() {
        return URI.create("urn:maintenance:problem:" + name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    static ProblemCode forStatus(int status) {
        return switch (status) {
            case 400 -> INVALID_REQUEST;
            case 401 -> AUTHENTICATION_REQUIRED;
            case 403 -> ACCESS_DENIED;
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 429 -> TOO_MANY_REQUESTS;
            default -> status >= 500 ? INTERNAL_ERROR : REQUEST_REJECTED;
        };
    }
}
