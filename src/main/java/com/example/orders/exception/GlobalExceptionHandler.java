package com.example.orders.exception;

import com.example.orders.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Global exception interceptor translating exceptions into a uniform ApiError structure
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Shared comparator providing deterministic field error ordering by field name, then message
    private static final Comparator<ApiError.FieldErrorDetail> FIELD_ERROR_COMPARATOR = new Comparator<ApiError.FieldErrorDetail>() {
        @Override
        public int compare(ApiError.FieldErrorDetail a, ApiError.FieldErrorDetail b) {
            int cmp = a.getField().compareTo(b.getField());
            if (cmp != 0) {
                return cmp;
            }
            return a.getMessage().compareTo(b.getMessage());
        }
    };

    // Maps domain resource lookup failures to HTTP 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // Maps stock deficiency during order placement to HTTP 409 Conflict
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // Maps illegal order state transitions to HTTP 409 Conflict
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidStatusTransition(InvalidStatusTransitionException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // Maps cancellation attempts on non-PLACED orders to HTTP 409 Conflict
    @ExceptionHandler(OrderNotCancellableException.class)
    public ResponseEntity<ApiError> handleOrderNotCancellable(OrderNotCancellableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // ConcurrencyFailureException is the common parent of optimistic-lock failures and database lock failures (deadlock, lock wait timeout), so one handler covers both
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> handleConcurrencyFailure(ConcurrencyFailureException ex, HttpServletRequest request) {
        logger.warn("Concurrency conflict ({}) on {}: {}", ex.getClass().getSimpleName(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "The request conflicted with another update to the same data. Please retry.", request);
    }

    // Maps semantic order validation errors like duplicate product IDs to HTTP 400 Bad Request
    @ExceptionHandler(InvalidOrderRequestException.class)
    public ResponseEntity<ApiError> handleInvalidOrderRequest(InvalidOrderRequestException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // Maps request body validation failures to HTTP 400 Bad Request with field-level details
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new ApiError.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        // Sorting guarantees a deterministic order across test runs and environments
        fieldErrors.sort(FIELD_ERROR_COMPARATOR);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    // Maps Spring 6.1 method parameter validation constraints on query params to HTTP 400 Bad Request
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> fieldErrors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getAllValidationResults()) {
            String paramName = result.getMethodParameter().getParameterName();
            String fieldName = paramName != null ? paramName : "null";
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                fieldErrors.add(new ApiError.FieldErrorDetail(fieldName, error.getDefaultMessage()));
            }
        }
        // Sorting guarantees a deterministic order across test runs and environments
        fieldErrors.sort(FIELD_ERROR_COMPARATOR);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    // Jackson messages leak internal class names, so a sanitized message is returned instead
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request or invalid field value", request);
    }

    // Maps path variable or request parameter type mismatches to HTTP 400 Bad Request
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    // Maps unsupported HTTP request methods to HTTP 405 Method Not Allowed
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    // Maps unsupported Content-Type header formats to HTTP 415 Unsupported Media Type
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request);
    }

    // Maps missing endpoint routes and static resource requests to HTTP 404 Not Found
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "No endpoint found for this path", request);
    }

    // Spring picks the most specific matching handler, so order does not change behaviour but readers expect it at the bottom
    // clients never see stack traces or exception class names; the log keeps them for us
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnhandledException(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception on {}", request.getRequestURI(), ex);
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Internal server error",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    // Shared response builder for standard client errors logging at warning level without stack traces
    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        logger.warn("Client error on {}: {}", request.getRequestURI(), message);
        ApiError apiError = new ApiError(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(apiError);
    }

    // Shared response builder for validation errors including field-level failure lists
    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, HttpServletRequest request, List<ApiError.FieldErrorDetail> fieldErrors) {
        logger.warn("Validation error on {}: {}", request.getRequestURI(), message);
        ApiError apiError = new ApiError(status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(apiError);
    }
}
