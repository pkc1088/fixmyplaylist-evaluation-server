package evaluation.evaluationService.evaluation.infrastructure.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Tag("unit")
class EvaluationSecretFilterTest {

    private final EvaluationSecretFilter filter = new EvaluationSecretFilter("expected-secret");

    @Test
    void blocksAdminRequestWithMissingOrIncorrectSecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/rag/promote/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertEquals("Invalid Secret Key", response.getContentAsString());
        verifyNoInteractions(chain);
    }

    @Test
    void allowsAdminRequestWithExpectedSecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/rag/promote/1");
        request.addHeader("X-Evaluation-Secret", "expected-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doesNotRequireSecretOutsideAdminApi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-review.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
