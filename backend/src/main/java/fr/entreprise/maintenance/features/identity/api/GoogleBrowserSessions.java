package fr.entreprise.maintenance.features.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class GoogleBrowserSessions {
    private static final String BINDING = "maintenance.google.browser-binding";
    public String binding(HttpServletRequest request, boolean create) {
        var session = request.getSession(create);
        if (session == null) { return null; }
        Object stored = session.getAttribute(BINDING);
        if (stored instanceof String value) { return value; }
        if (!create) { return null; }
        String value = OpaqueTokens.generate(); session.setAttribute(BINDING, value); return value;
    }
    public LoginIdentity identity(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null && session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) instanceof SecurityContext context
                && context.getAuthentication() != null && context.getAuthentication().getPrincipal() instanceof LoginIdentity identity) {
            return identity;
        }
        return null;
    }
}
