package com.luren.aicvoptimize.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.stereotype.Component;

/**
 * 日志拦截器
 */
@Component
public class LoggingInterceptor extends ModelInterceptor {

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {

        System.out.println("模型调用开始");

        // 执行实际调用
        ModelResponse response = handler.call(request);

        return response;
    }

    @Override
    public String getName() {
        return "logging_interceptor";
    }
}