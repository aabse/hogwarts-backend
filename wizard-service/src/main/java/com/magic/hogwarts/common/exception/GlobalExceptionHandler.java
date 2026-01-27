package com.magic.hogwarts.common.exception;

import com.magic.hogwarts.common.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handler that returns a consistent {@link
 * com.magic.hogwarts.common.error.ApiError} payload for REST APIs.
 *
 * <p>It also includes {@code traceId} from MDC (see {@link
 * com.magic.hogwarts.common.web.CorrelationIdFilter}).
 *
 * @author Angel Absé
 * @since 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles bean validation errors and returns a structured response including per-field messages.
   *
   * @param ex validation exception raised by Spring MVC
   * @param request current HTTP request
   * @return standardized API error response
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, Object> details = new HashMap<>();
    Map<String, String> fieldErrors = new HashMap<>();

    ex.getBindingResult()
        .getFieldErrors()
        .forEach(
            error -> {
              fieldErrors.put(error.getField(), error.getDefaultMessage());
            });
    details.put("fields", fieldErrors);

    return buildError(
        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, details);
  }

  /**
   * Handles missing resources.
   *
   * @param ex exception indicating the resource is missing
   * @param request current HTTP request
   * @return 404 response payload
   */
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ApiError> handleNotFound(
      NoSuchElementException ex, HttpServletRequest request) {
    return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request, null);
  }

  /**
   * Handles invalid client input (400).
   *
   * @param ex exception describing invalid input
   * @param request current HTTP request
   * @return 400 response payload
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(
      IllegalArgumentException ex, HttpServletRequest request) {
    return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, null);
  }

  /**
   * Handles domain conflicts (409).
   *
   * @param ex conflict exception
   * @param request current HTTP request
   * @return 409 response payload
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> handleConflict(
      IllegalStateException ex, HttpServletRequest request) {
    return buildError(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request, null);
  }

  /**
   * Handles optimistic locking conflicts (409) caused by concurrent updates.
   *
   * @param ex optimistic locking failure
   * @param request current HTTP request
   * @return 409 response payload
   */
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiError> handleConflict(
      ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
    return buildError(
        HttpStatus.CONFLICT,
        "CONFLICT",
        "Concurrency conflict: the entity was modified by another process. Please try again.",
        request,
        null);
  }

  /**
   * Builds a standardized error payload.
   *
   * @param status HTTP status to return
   * @param code machine-readable error code
   * @param message human-readable message
   * @param request current HTTP request
   * @param details optional details map (e.g. field errors)
   * @return response with {@link ApiError} body
   */
  private ResponseEntity<ApiError> buildError(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      Map<String, Object> details) {
    ApiError err = new ApiError();
    err.setCode(code);
    err.setMessage(message);
    err.setTimestamp(OffsetDateTime.now());
    err.setPath(request.getRequestURI());
    err.setTraceId(MDC.get("traceId"));
    err.setDetails(details);
    return new ResponseEntity<>(err, status);
  }
}
