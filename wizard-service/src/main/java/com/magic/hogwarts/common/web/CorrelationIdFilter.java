package com.magic.hogwarts.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that ensures every request has a correlation id.
 *
 * <p>The value is taken from {@code X-Correlation-Id} header when present or generated otherwise.
 * The same value is placed into SLF4J MDC under {@code traceId} and also returned as response
 * header.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
  /** HTTP header name used to propagate correlation ids. */
  public static final String HEADER_NAME = "X-Correlation-Id";

  /** MDC key used across the codebase and error payloads. */
  public static final String MDC_KEY = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    /*
     * Correlation id extracted from the request header or generated.
     * It is stored in MDC for logging and returned in the response header.
     */
    String correlationId = request.getHeader(HEADER_NAME);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }

    MDC.put(MDC_KEY, correlationId);
    response.setHeader(HEADER_NAME, correlationId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
