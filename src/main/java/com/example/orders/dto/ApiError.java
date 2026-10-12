package com.example.orders.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

// Standardized API error response body shared across all error responses
// Non-null inclusion ensures fieldErrors is omitted from the JSON output when absent
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    // Timestamp when the error occurred on the server
    private LocalDateTime timestamp;

    // HTTP status code (e.g. 400, 404, 409, 500)
    private int status;

    // HTTP status reason phrase (e.g. "Bad Request", "Not Found", "Conflict")
    private String error;

    // Human-readable summary message describing the error
    private String message;

    // The request URI that triggered the error
    private String path;

    // Specific validation failures for individual fields, omitted when null
    private List<FieldErrorDetail> fieldErrors;

    // Required for serialization frameworks and JSON binding
    public ApiError() {
    }

    // Convenience constructor for error responses without field-level errors
    public ApiError(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // Convenience constructor for validation failure responses containing field-level details
    public ApiError(int status, String error, String message, String path, List<FieldErrorDetail> fieldErrors) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<FieldErrorDetail> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldErrorDetail> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    // Encapsulates field-specific validation failure detail
    public static class FieldErrorDetail {

        // The property or parameter path that failed validation
        private String field;

        // The validation violation description
        private String message;

        // Required for JSON deserialization frameworks
        public FieldErrorDetail() {
        }

        public FieldErrorDetail(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
