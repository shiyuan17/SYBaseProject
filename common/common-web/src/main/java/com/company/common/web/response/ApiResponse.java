package com.company.common.web.response;

public record ApiResponse<T>(String code, String message, String traceId, T data) {

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("SUCCESS", "success", traceId, data);
    }

    public static ApiResponse<Void> failure(String code, String message, String traceId) {
        return new ApiResponse<>(code, message, traceId, null);
    }
}
