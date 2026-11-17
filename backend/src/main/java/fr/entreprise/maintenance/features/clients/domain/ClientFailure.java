package fr.entreprise.maintenance.features.clients.domain;

public final class ClientFailure extends RuntimeException {
    public enum Code { INVALID_CLIENT_INPUT, INVALID_INVITATION, CLIENT_DOSSIER_NOT_FOUND, CLIENT_STATE_CONFLICT, CLIENT_VERSION_STALE, CLIENT_PROFILE_INCOMPLETE }
    private final Code code;
    public ClientFailure(Code code) { super("Client command refused."); this.code = code; }
    public Code code() { return code; }
}
