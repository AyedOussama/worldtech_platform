package fr.entreprise.maintenance.features.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Initial policy: ASCII mailbox, case-insensitive login, no provider-specific rewriting. */
public record EmailAddress(String display, String normalized) {
    private static final Pattern MAILBOX = Pattern.compile(
            "[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]{1,64}@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,63}");

    public static EmailAddress parse(String raw) {
        String display = raw == null ? "" : raw.strip();
        if (display.length() > 254 || !MAILBOX.matcher(display).matches()
                || display.contains("..") || display.startsWith(".") || display.contains(".@")) {
            throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT);
        }
        for (String label : display.substring(display.indexOf('@') + 1).split("\\.", -1)) {
            if (label.length() > 63 || !label.matches("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?")) {
                throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT);
            }
        }
        return new EmailAddress(display, display.toLowerCase(Locale.ROOT));
    }
}
