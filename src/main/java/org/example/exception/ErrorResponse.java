package org.example.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一错误响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private long timestamp;

    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = System.currentTimeMillis();
    }

    public static ErrorResponse badRequest(String message, String path) {
        return new ErrorResponse(400, "Bad Request", message, path);
    }

    public static ErrorResponse notFound(String message, String path) {
        return new ErrorResponse(404, "Not Found", message, path);
    }

    public static ErrorResponse forbidden(String message, String path) {
        return new ErrorResponse(403, "Forbidden", message, path);
    }

    public static ErrorResponse internalServerError(String message, String path) {
        return new ErrorResponse(500, "Internal Server Error", message, path);
    }

    public static ErrorResponse unsupportedMediaType(String message, String path) {
        return new ErrorResponse(415, "Unsupported Media Type", message, path);
    }
}
