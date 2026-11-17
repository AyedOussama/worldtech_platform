package fr.entreprise.maintenance.features.clients.domain;

import java.time.Instant;
import java.util.UUID;

public record ClientAccount(UUID accountId, Type accountType, String displayName, UUID founderUserId, long version, Instant createdAt) {
    public enum Type { BUSINESS, INDIVIDUAL }
    public void expect(long expected) { if(version!=expected) { throw new AccountFailure(AccountFailure.Code.ACCOUNT_VERSION_STALE); } }
    public void allowMember(UUID user) {
        if(accountType==Type.INDIVIDUAL && !founderUserId.equals(user)) { throw new AccountFailure(AccountFailure.Code.ACCOUNT_MEMBERSHIP_CONFLICT); }
    }
    public static String name(String value) {
        if(value==null || value.isBlank() || value.length()>240 || value.chars().anyMatch(Character::isISOControl)) { throw new AccountFailure(AccountFailure.Code.INVALID_ACCOUNT_INPUT); }
        return value.strip();
    }
    @Override public String toString() { return "ClientAccount[redacted]"; }
}
