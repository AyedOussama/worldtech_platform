package fr.entreprise.maintenance.features.clients.domain;

public final class AccountFailure extends RuntimeException {
    public enum Code { INVALID_ACCOUNT_INPUT, ACCOUNT_NOT_FOUND, ACCOUNT_VERSION_STALE, ACCOUNT_MEMBERSHIP_CONFLICT, CLIENT_NOT_APPROVED }
    private final Code code;
    public AccountFailure(Code code) { super("Account command refused."); this.code=code; }
    public Code code() { return code; }
}
