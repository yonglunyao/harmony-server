package org.example.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回标准格式的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 400 - 请求参数错误
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            IllegalArgumentException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest request) {
        String message = "请求参数错误";

        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            message = getValidationErrorMessage(ex.getBindingResult().getFieldErrors());
        } else if (e instanceof IllegalArgumentException) {
            message = e.getMessage();
        } else if (e instanceof org.springframework.http.converter.HttpMessageNotReadableException) {
            message = "请求体格式错误，请检查 JSON 格式是否正确";
        }

        log.warn("Bad Request: {} - {}", request.getRequestURI(), message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, request.getRequestURI()));
    }

    /**
     * 处理 404 - 资源不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException e, HttpServletRequest request) {
        String message = "接口不存在: " + e.getRequestURL();
        log.warn("Not Found: {}", message);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", message, request.getRequestURI()));
    }

    /**
     * 处理 403 - 禁止访问
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(SecurityException e, HttpServletRequest request) {
        String message = "访问被拒绝: " + e.getMessage();
        log.warn("Forbidden: {} - {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden", message, request.getRequestURI()));
    }

    /**
     * 处理 415 - 不支持的媒体类型
     */
    @ExceptionHandler({
            org.springframework.web.HttpMediaTypeNotSupportedException.class,
            org.springframework.http.converter.HttpMessageNotWritableException.class
    })
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(Exception e, HttpServletRequest request) {
        String message = "不支持的媒体类型，请使用 Content-Type: application/json";
        log.warn("Unsupported Media Type: {} - {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse(415, "Unsupported Media Type", message, request.getRequestURI()));
    }

    /**
     * 处理文件大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e, HttpServletRequest request) {
        String message = "上传文件大小超过限制（最大 2GB）";
        log.warn("Max Upload Size Exceeded: {} - {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, request.getRequestURI()));
    }

    /**
     * 处理 500 - 内部服务器错误
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleServerError(Exception e, HttpServletRequest request) {
        // 获取异常堆栈信息
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        String message = "服务器内部错误: " + e.getMessage() + "\n" + stackTrace;
        log.error("Internal Server Error: {} - {}", request.getRequestURI(), e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error", message, request.getRequestURI()));
    }

    /**
     * 提取验证错误信息
     */
    private String getValidationErrorMessage(java.util.List<FieldError> fieldErrors) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : fieldErrors) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return "参数验证失败: " + errors.toString();
    }
}
