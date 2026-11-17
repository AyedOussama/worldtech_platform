package fr.entreprise.maintenance.shared.http;

import fr.entreprise.httpfixtures.ErrorProbeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var mapper = JsonMapper.builder().addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class).build();
        var problems = new ApiProblems(mapper);
        // Test-only controller, deliberately outside the application context/security chain.
        mvc = MockMvcBuilders.standaloneSetup(new ErrorProbeController())
                .setControllerAdvice(new ApiExceptionHandler(problems))
                .addFilters(new RequestTraceFilter())
                .build();
    }

    @Test
    void unexpectedFailureDoesNotExposeExceptionOrLogItsMessage(CapturedOutput output) throws Exception {
        mvc.perform(get("/_test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("internal-diagnostic-marker"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
        assertThat(output.getAll()).contains("exceptionType=java.lang.IllegalStateException")
                .doesNotContain("internal-diagnostic-marker");
    }

    @Test
    void validationReportsFieldAndCodeButNeverRejectedValueOrCustomMessage() throws Exception {
        mvc.perform(post("/_test/validation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"caller-controlled-marker\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("value"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("Size"))
                .andExpect(content().string(not(containsString("caller-controlled-marker"))))
                .andExpect(content().string(not(containsString("unsafe-validation-message"))))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void malformedJsonDoesNotLeakParserDiagnostics() throws Exception {
        mvc.perform(post("/_test/validation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": caller-controlled-marker"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(content().string(not(containsString("caller-controlled-marker"))))
                .andExpect(content().string(not(containsString("JsonParseException"))));
    }

    @Test
    void methodErrorRetainsAllowHeaderWithoutExceptionDetails() throws Exception {
        mvc.perform(get("/_test/validation"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(header().string("Allow", containsString("POST")));
    }

    @Test
    void unsupportedContentTypeIsAProblemResponse() throws Exception {
        mvc.perform(post("/_test/validation").contentType(MediaType.TEXT_PLAIN).content("synthetic-value"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void authorizationExceptionIsNotConvertedToInternalFailure() throws Exception {
        mvc.perform(get("/_test/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(content().string(not(containsString("internal-diagnostic-marker"))));
    }

}
