package fr.entreprise.maintenance.features.identity;

import java.time.Instant;
import fr.entreprise.maintenance.features.identity.infrastructure.security.LibraryTotpEngine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryTotpEngineTest {
    @Test
    void publishedRfc6238VectorWindowAndReplayAreVerifiedByTheLibrary() {
        var engine = new LibraryTotpEngine();
        // RFC 6238 appendix B public SHA1 test key, truncated to six digits as configured.
        String rfcSecret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        assertThat(engine.acceptedStep(rfcSecret, "287082", Instant.ofEpochSecond(59), -1)).isEqualTo(1);
        assertThat(engine.acceptedStep(rfcSecret, "287082", Instant.ofEpochSecond(60), -1)).isEqualTo(1);
        assertThat(engine.acceptedStep(rfcSecret, "287082", Instant.ofEpochSecond(59), 1)).isEqualTo(-1);
        assertThat(engine.acceptedStep(rfcSecret, "287082", Instant.ofEpochSecond(120), -1)).isEqualTo(-1);
        assertThat(engine.acceptedStep(rfcSecret, "bad", Instant.ofEpochSecond(59), -1)).isEqualTo(-1);
    }
}
