package fr.entreprise.maintenance.bootstrap.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import fr.entreprise.maintenance.shared.http.ApiProblems;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthBodyLimitFilter extends OncePerRequestFilter {
    private final ApiProblems problems;
    public AuthBodyLimitFilter(ApiProblems problems) { this.problems = problems; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean onboarding=path.startsWith("/api/web/v1/onboarding/") || path.startsWith("/api/mobile/v1/onboarding/") || path.startsWith("/api/web/v1/admin/") || path.startsWith("/api/web/v1/client/accounts/");
        if (!(path.startsWith("/api/web/v1/auth/") || path.startsWith("/api/mobile/v1/auth/") || onboarding)
                || java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
            chain.doFilter(request, response); return;
        }
        int limit=onboarding ? 16384 : 4096;
        if (request.getContentLengthLong() > limit) { problems.write(413, request, response); return; }
        byte[] bytes = request.getInputStream().readNBytes(limit+1);
        if (bytes.length > limit) { problems.write(413, request, response); return; }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var stream = new ByteArrayInputStream(bytes);
                return new ServletInputStream() {
                    @Override public boolean isFinished() { return stream.available() == 0; }
                    @Override public boolean isReady() { return true; }
                    @Override public void setReadListener(ReadListener listener) { throw new IllegalStateException("Synchronous authentication body only."); }
                    @Override public int read() { return stream.read(); }
                    @Override public int read(byte[] value, int offset, int length) { return stream.read(value, offset, length); }
                };
            }
            @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
            @Override public int getContentLength() { return bytes.length; }
            @Override public long getContentLengthLong() { return bytes.length; }
        }, response);
    }
}
