package com.luren.aicvoptimize.hooks;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 日志模型钩子
 *
 */
@Component
@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class LogModelHook extends ModelHook {

    @Override
    public String getName() {
        return "log_model";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 在模型调用前执行
        System.out.println("准备调用模型...");

        // 可以修改状态
        // 例如：添加额外的上下文
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        // 在模型调用后执行
        System.out.println("模型调用完成");

        // 可以记录响应信息
        return CompletableFuture.completedFuture(Map.of());
    }
}