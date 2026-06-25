package org.example.inventory.dto.common;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;

public class ApiResponse<T> {

    private int status;
    private String code;
    private String message;
    private T result;
    private T data;
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

    public static <T> ApiResponse<T> success(T result) {
        return success("OK", result);
    }

    public static <T> ApiResponse<T> success(String message, T result) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatus(HttpStatus.OK.value());
        response.setCode("SUCCESS");
        response.setMessage(message);
        response.setGeneratedAt(OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.setResult(result);
        return response;
    }
}
