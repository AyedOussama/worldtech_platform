package fr.entreprise.maintenance.features.identity.domain;

import java.util.Locale;
import java.util.Set;

/** User-approved policy; preserve whitespace/Unicode, never truncate or normalize passwords. */
public final class PasswordPolicy {
    private final Set<String> blocked;
    public PasswordPolicy(Set<String> blocked) { this.blocked = Set.copyOf(blocked); }

    public void validate(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 15
                || password.codePointCount(0, password.length()) > 128
                || blocked.contains(password.toLowerCase(Locale.ROOT))) {
            throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT);
        }
        for (int i = 0; i < password.length(); i++) {
            char value = password.charAt(i);
            if (Character.isHighSurrogate(value)) {
                if (++i >= password.length() || !Character.isLowSurrogate(password.charAt(i))) {
                    throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT);
                }
            } else if (Character.isLowSurrogate(value)) {
                throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT);
            }
        }
    }
}
