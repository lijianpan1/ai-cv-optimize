package com.luren.aicvoptimize.config;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.luren.aicvoptimize.dto.ResumeParseResponse;
import com.luren.aicvoptimize.interceptor.ToolMonitoringInterceptor;
import com.luren.aicvoptimize.prompt.PromptManager;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对话客户端配置
 *
 * @author lijianpan
 **/
@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    private final ChatModel chatModel;

    /**
     * 基础智能体配置
     *
     */
    @Bean
    public Builder baseBuilder() {
        return ReactAgent.builder()
                .model(chatModel)
                .saver(new MemorySaver());
    }
}
