package com.magic.hogwarts.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  @Test
  void whenHeaderPresent_usesItAndSetsMdcAndResponseHeader() throws Exception {
    CorrelationIdFilter filter = new CorrelationIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER_NAME, "abc-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    CapturingChain chain = new CapturingChain();
    filter.doFilter(request, response, chain);

    assertThat(chain.mdcValueSeen).isEqualTo("abc-123");
    assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("abc-123");
    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void whenHeaderMissing_generatesOneAndPropagatesIt() throws Exception {
    CorrelationIdFilter filter = new CorrelationIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    CapturingChain chain = new CapturingChain();
    filter.doFilter(request, response, chain);

    assertThat(chain.mdcValueSeen).isNotBlank();
    assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo(chain.mdcValueSeen);
    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }

  private static final class CapturingChain implements FilterChain {
    /** MDC value captured inside the filter chain. */
    private String mdcValueSeen;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException {
      mdcValueSeen = MDC.get(CorrelationIdFilter.MDC_KEY);
    }
  }
}
