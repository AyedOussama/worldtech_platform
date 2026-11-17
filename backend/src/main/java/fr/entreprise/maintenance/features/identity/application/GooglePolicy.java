package fr.entreprise.maintenance.features.identity.application;

import java.net.URI;
import java.util.Map;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.identity.google")
public record GooglePolicy(boolean enabled, Map<String, URI> mobileDestinations) {
    public GooglePolicy {
        mobileDestinations = mobileDestinations == null ? Map.of() : Map.copyOf(mobileDestinations);
        mobileDestinations.forEach((id, uri) -> {
            if (!id.matches("[A-Za-z0-9_-]{1,40}") || !"https".equals(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null || uri.getPort() != -1
                    || uri.getPath().isBlank() || uri.getPath().contains("..")) {
                throw new IllegalArgumentException("A controlled HTTPS application link is required.");
            }
        });
    }
    public URI destination(String id) {
        URI destination = id == null ? null : mobileDestinations.get(id);
        if (destination == null) { throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT); }
        return destination;
    }
}
