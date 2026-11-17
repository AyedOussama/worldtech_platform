import java.util.UUID;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/** Sonde isolée du lot 00 ; ni application, ni benchmark de production. */
public final class Argon2CompatibilityProbe {
    public static void main(String[] args) {
        var encoder = new Argon2PasswordEncoder(16, 32, 1, 19_456, 2);
        String syntheticPassword = UUID.randomUUID() + " é espace";
        long started = System.nanoTime();
        String first = encoder.encode(syntheticPassword);
        String second = encoder.encode(syntheticPassword);

        require(first.startsWith("$argon2id$v=19$m=19456,t=2,p=1$"), "Paramètres inattendus");
        require(encoder.matches(syntheticPassword, first), "Correspondance correcte refusée");
        require(!encoder.matches(syntheticPassword + "!", first), "Mot de passe incorrect accepté");
        require(!first.equals(second), "Deux encodages ont le même sel/résultat");

        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        System.out.println("OK: Argon2id m=19456,t=2,p=1; correspondance positive/négative; sels distincts.");
        System.out.println("Deux encodages et deux vérifications, durée indicative totale: "
                + elapsedMillis + " ms. Ce résultat n'est pas un benchmark.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
