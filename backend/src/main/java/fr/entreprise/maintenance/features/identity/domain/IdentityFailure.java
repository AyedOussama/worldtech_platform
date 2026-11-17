package fr.entreprise.maintenance.features.identity.domain;

public final class IdentityFailure extends RuntimeException {
    public enum Reason { INVALID_INPUT, BAD_CREDENTIALS, FORBIDDEN, RATE_LIMITED, CONFLICT }
    private final Reason reason;
    private final int retryAfterSeconds;

    public IdentityFailure(Reason reason) {
        this(reason, reason == Reason.RATE_LIMITED ? 1 : 0);
    }

    public IdentityFailure(Reason reason, int retryAfterSeconds) {
        super(reason.name()); // No rejected value, credential, SQL or provider message.
        this.reason = reason;
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public Reason reason() { return reason; }
    public int retryAfterSeconds() { return retryAfterSeconds; }
}
