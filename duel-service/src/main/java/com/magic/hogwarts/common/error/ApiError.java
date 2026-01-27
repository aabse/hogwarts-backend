package com.magic.hogwarts.common.error;

import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Data;

/**
 * Standard API error response payload used by the microservices.
 *
 * <p>This object is returned by {@code GlobalExceptionHandler} and includes a correlation/trace id
 * that can be used to follow a request across logs.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class ApiError {
  /** Machine-readable error code (e.g. {@code VALIDATION_ERROR}, {@code NOT_FOUND}). */
  private String code;

  /** Human-readable error message. */
  private String message;

  /** Timestamp when the error response was created. */
  private OffsetDateTime timestamp;

  /** Request path that caused the error (e.g. {@code /api/duels}). */
  private String path;

  /** Correlation id for the request (propagated via {@code X-Correlation-Id}). */
  private String traceId;

  /** Optional details for the client (e.g. field validation errors). */
  private Map<String, Object> details;
}
