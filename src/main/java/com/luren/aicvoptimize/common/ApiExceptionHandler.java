package com.luren.aicvoptimize.common;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一异常到 JSON 的映射，避免默认 HTML error page 影响前端/调用方解析。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toDetail)
                .toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error", "VALIDATION_ERROR");
        resp.put("details", details);
        return resp;
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, Object> handleIllegalState(IllegalStateException ex) {
        // 业务侧把“上游模型输出不可解析”等情况统一映射为 502，便于快速区分是输入校验问题还是模型侧问题。
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error", "UPSTREAM_MODEL_ERROR");
        resp.put("message", ex.getMessage());
        return resp;
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error", "AUTH_ERROR");
        resp.put("message", ex.getMessage());
        return resp;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGenericException(Exception ex) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error", "INTERNAL_ERROR");
        resp.put("message", ex.getMessage());
        return resp;
    }

    private Map<String, Object> toDetail(FieldError fe) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("field", fe.getField());
        d.put("message", fe.getDefaultMessage());
        return d;
    }
}

