package com.luren.aicvoptimize.hooks;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 日志Agent钩子
 */
@Component
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class LogAgentHoook extends AgentHook {
    @Override
    public String getName() {
        return "log_agent";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 开始执行");

        Optional<String> resumeJson = state.value("resume_json");
        resumeJson.ifPresent(System.out::println);

        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 执行完成");

        Optional<String> resumeJson = state.value("resume_json");
        resumeJson.ifPresent(System.out::println);

        return CompletableFuture.completedFuture(Map.of());
    }
}