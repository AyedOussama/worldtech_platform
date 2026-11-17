package fr.entreprise.maintenance.shared.http;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestTrace {

    public static final String HEADER = "X-Trace-Id";
    private static final String ATTRIBUTE = RequestTrace.class.getName() + ".id";

    private RequestTrace() {
    }

    public static String id(HttpServletRequest request) {
        if (request.getAttribute(ATTRIBUTE) instanceof String existing) {
            return existing;
        }
        // Never trust or echo an arbitrary trace identifier supplied by the caller.
        String id = UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, id);
        return id;
    }
}
