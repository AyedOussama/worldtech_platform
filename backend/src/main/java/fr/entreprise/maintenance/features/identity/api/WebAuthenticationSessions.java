package fr.entreprise.maintenance.features.identity.api;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import fr.entreprise.maintenance.features.identity.application.WebIdentityService;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Component;

@Component
public class WebAuthenticationSessions {
    private final WebIdentityService identity;
    public WebAuthenticationSessions(WebIdentityService identity) { this.identity = identity; }

    public void rotate(LoginIdentity principal, HttpServletRequest request, HttpServletResponse response) {
        var current = identity.current(principal).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                identity.authorities(current).stream().map(SimpleGrantedAuthority::new).toList());
        new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository()))).onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    }

    public void clear(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) { session.invalidate(); }
        SecurityContextHolder.clearContext();
    }
}
