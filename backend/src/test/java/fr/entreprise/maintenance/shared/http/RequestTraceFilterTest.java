package fr.entreprise.maintenance.shared.http;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestTraceFilterTest {

    @Test
    void traceContextIsRestoredEvenWhenTheChainFails() {
        String original = MDC.get("traceId");
        MDC.put("traceId", "outer-context");
        try {
            assertThatThrownBy(() -> new RequestTraceFilter().doFilter(
                    new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
                        assertThat(MDC.get("traceId")).isNotEqualTo("outer-context");
                        throw new ServletException("synthetic-failure");
                    })).isInstanceOf(ServletException.class);
            assertThat(MDC.get("traceId")).isEqualTo("outer-context");
        } finally {
            if (original == null) {
                MDC.remove("traceId");
            } else {
                MDC.put("traceId", original);
            }
        }
    }
}
