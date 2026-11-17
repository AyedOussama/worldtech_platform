package fr.entreprise.maintenance.features.identity;

import fr.entreprise.maintenance.features.identity.domain.AccountStatus;
import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityDomainTest {
    @Test
    void passwordBoundsCountCodePointsPreserveSpacesAndRejectKnownCommonOrMalformedUnicode() {
        PasswordPolicy policy = new PasswordPolicy(java.util.Set.of("this is a common password"));
        policy.validate(" a long passphrase with spaces ");
        policy.validate("\uD83D\uDD12".repeat(15));
        policy.validate("a".repeat(128));
        for (String rejected : java.util.List.of("a".repeat(14), "a".repeat(129),
                "THIS IS A COMMON PASSWORD", "this is a long password\uD800")) {
            assertThatThrownBy(() -> policy.validate(rejected)).isInstanceOf(IdentityFailure.class);
        }
    }

    @Test
    void emailPolicyPreservesProviderMeaningAndUsesStableLocaleIndependentNormalization() {
        var email = EmailAddress.parse("  First.Last+label@Example.INVALID ");
        assertThat(email.display()).isEqualTo("First.Last+label@Example.INVALID");
        assertThat(email.normalized()).isEqualTo("first.last+label@example.invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad", ".first@example.invalid", "a..b@example.invalid", "x@localhost", "a\n@example.invalid", "a@-bad.invalid", "a@good.-bad.invalid", "a@good.bad-.invalid"})
    void invalidEmailDoesNotProduceADifferentIdentity(String email) {
        assertThatThrownBy(() -> EmailAddress.parse(email)).isInstanceOf(IdentityFailure.class);
    }

    @Test
    void rejectedAndSuspendedAreNotAuthenticatable() {
        assertThat(AccountStatus.ACTIVE.canAuthenticate()).isTrue();
        assertThat(AccountStatus.PENDING.canAuthenticate()).isTrue();
        assertThat(AccountStatus.SUSPENDED.canAuthenticate()).isFalse();
        assertThat(AccountStatus.REJECTED.canAuthenticate()).isFalse();
    }
}
