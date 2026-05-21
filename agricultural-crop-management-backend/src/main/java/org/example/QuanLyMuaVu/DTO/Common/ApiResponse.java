package org.example.QuanLyMuaVu.DTO.Common;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;

/**
 * Standard API response wrapper for all endpoints.
 *
 * @param <T> type of the actual payload
 */
public class ApiResponse<T> {

    /**
     * HTTP status code (e.g. 200, 400, 404).
     */
    private int status;

    /**
     * Application-level code (e.g. "SUCCESS", "ERR_NOT_FOUND").
     */
    private String code;

    /**
     * Human-readable message.
     */
    private String message;

    /**
     * Actual data payload.
     */
    private T result;

    /**
     * Canonical payload field for dashboard/data contracts.
     * Kept in sync with {@link #result} for backward compatibility.
     */
    private T data;

    /**
     * UTC timestamp indicating when the response was generated.
     */
    private String generatedAt;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
        this.data = result;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
        this.result = data;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    /**
     * Build a successful response with HTTP 200 and default message "OK".
     */
    public static <T> ApiResponse<T> success(T result) {
        return success("OK", result);
    }

    /**
     * Build a successful response with HTTP 200 and custom message.
     */
    public static <T> ApiResponse<T> success(String message, T result) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatus(HttpStatus.OK.value());
        response.setCode("SUCCESS");
        response.setMessage(message);
        response.setGeneratedAt(OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.setResult(result);
        return response;
    }

    /**
     * Build an error response with the given HTTP status and application code.
     */
    public static <T> ApiResponse<T> error(HttpStatus status, String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatus(status.value());
        response.setCode(code);
        response.setMessage(message);
        response.setGeneratedAt(OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.setResult(null);
        return response;
    }
}
