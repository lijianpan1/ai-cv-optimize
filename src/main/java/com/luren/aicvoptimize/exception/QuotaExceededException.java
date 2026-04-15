package com.luren.aicvoptimize.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 配额不足异常
 * <p>
 * 当用户使用次数耗尽时抛出
 *
 * @author lijianpan
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException() {
        super("您的免费使用次数已用完，请联系管理员获取更多次数");
    }

    public QuotaExceededException(String message) {
        super(message);
    }

    public QuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
