package com.sdkanban.common;

import java.util.Map;

public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String code;
    private final String message;
    private final Map<String, String> fieldErrors;

    private ApiResponse(boolean success, T data, String code, String message, Map<String, String> fieldErrors) {
        this.success = success;
        this.data = data;
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return error(code, message, null);
    }

    public static ApiResponse<Void> error(String code, String message, Map<String, String> fieldErrors) {
        return new ApiResponse<>(false, null, code, message, fieldErrors);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
