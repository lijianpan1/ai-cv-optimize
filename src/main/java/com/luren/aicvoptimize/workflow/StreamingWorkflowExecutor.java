package com.luren.aicvoptimize.workflow;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.luren.aicvoptimize.service.WorkflowProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * 流式工作流执行器
 * <p>
 * 支持实时进度回调的工作流执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingWorkflowExecutor {

    private final WorkflowProgressService progressService;

    /**
     * 执行工作流并实时推送进度（无中断、无持久化）
     *
     * @param workflow 工作流定义
     * @param input    初始输入
     * @param clientId 客户端ID，用于推送进度
     * @return 最终状态
     */
    public Map<String, Object> executeWithStreaming(StateGraph workflow, Map<String, Object> input, String clientId) {
        return executeWithStreaming(workflow, input, null, null, clientId);
    }

    /**
     * 执行工作流并实时推送进度（指定中断点）
     *
     * @param workflow             工作流定义
     * @param input                初始输入
     * @param interruptBeforeNodes 执行前中断的节点
     * @param interruptAfterNodes  执行后中断的节点
     * @param clientId             客户端ID，用于推送进度
     * @return 最终状态
     */
    public Map<String, Object> executeWithStreaming(StateGraph workflow, Map<String, Object> input,
                                                     Set<String> interruptBeforeNodes, Set<String> interruptAfterNodes,
                                                     String clientId) {
        CompileConfig.Builder builder = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(new MemorySaver())
                        .build());
        if (interruptBeforeNodes != null && !interruptBeforeNodes.isEmpty()) {
            builder.interruptBefore(interruptBeforeNodes.toArray(new String[0]));
        }
        if (interruptAfterNodes != null && !interruptAfterNodes.isEmpty()) {
            builder.interruptAfter(interruptAfterNodes.toArray(new String[0]));
        }
        return executeWithStreaming(workflow, input, builder.build(), clientId);
    }

    /**
     * 执行工作流并实时推送进度（完全控制编译配置）
     *
     * @param workflow     工作流定义
     * @param input        初始输入
     * @param compileConfig 编译配置
     * @param clientId     客户端ID，用于推送进度
     * @return 最终状态
     */
    public Map<String, Object> executeWithStreaming(StateGraph workflow, Map<String, Object> input,
                                                     CompileConfig compileConfig, String clientId) {
        CompiledGraph compiledGraph = compile(workflow, compileConfig);
        log.info("开始流式执行工作流，clientId: {}, 输入 keys: {}", clientId, input.keySet());

        // 发送开始事件
        progressService.sendStart(clientId);

        List<String> completedNodes = new CopyOnWriteArrayList<>();
        String[] currentNode = new String[1];

        try {
            NodeOutput lastOutput = compiledGraph.stream(input)
                    .doOnNext(output -> {
                        logOutput(output);
                        // 捕获节点执行信息并推送进度
                        if (output instanceof StreamingOutput<?> so) {
                            String nodeName = so.node();
                            
                            // 检测节点变化（包括第一个节点和后续节点切换）
                            boolean isNodeChanged = currentNode[0] == null || !currentNode[0].equals(nodeName);
                            
                            if (isNodeChanged) {
                                // 如果当前节点不为空，将其标记为已完成
                                if (currentNode[0] != null && !completedNodes.contains(currentNode[0])) {
                                    completedNodes.add(currentNode[0]);
                                    log.info("[流式进度] 节点完成: {}", currentNode[0]);
                                }
                                
                                // 更新当前节点并推送进度
                                currentNode[0] = nodeName;
                                log.info("[流式进度] 开始执行节点: {}, 已完成: {}", nodeName, completedNodes);
                                progressService.sendProgress(clientId, nodeName, new ArrayList<>(completedNodes));
                            }
                        }
                    })
                    .doOnError(error -> {
                        log.error("流式执行错误", error);
                        progressService.sendError(clientId, error.getMessage());
                    })
                    .blockLast();

            if (lastOutput instanceof InterruptionMetadata) {
                InterruptionMetadata interruption = (InterruptionMetadata) lastOutput;
                log.info("工作流在节点 [{}] 处中断", interruption.node());
                progressService.sendWaiting(clientId, interruption.node());
                return interruption.state().data();
            }

            // 执行完成
            Map<String, Object> finalState = lastOutput.state().data();
            log.info("工作流执行完成");
            // 发送完成事件（这里不发送，由调用方决定何时发送）
            return finalState;

        } catch (Exception e) {
            log.error("工作流执行异常", e);
            progressService.sendError(clientId, e.getMessage());
            throw new RuntimeException("工作流执行失败", e);
        }
    }

    /**
     * 执行工作流并返回 Flux 流（用于 WebFlux 响应式编程）
     *
     * @param workflow 工作流定义
     * @param input    初始输入
     * @return Flux 流，包含每个节点的输出
     */
    public Flux<WorkflowProgress> executeAsFlux(StateGraph workflow, Map<String, Object> input) {
        return executeAsFlux(workflow, input, CompileConfig.builder().build());
    }

    /**
     * 执行工作流并返回 Flux 流
     *
     * @param workflow      工作流定义
     * @param input         初始输入
     * @param compileConfig 编译配置
     * @return Flux 流
     */
    public Flux<WorkflowProgress> executeAsFlux(StateGraph workflow, Map<String, Object> input,
                                                 CompileConfig compileConfig) {
        CompiledGraph compiledGraph = compile(workflow, compileConfig);
        Sinks.Many<WorkflowProgress> sink = Sinks.many().unicast().onBackpressureBuffer();

        List<String> completedNodes = new CopyOnWriteArrayList<>();

        compiledGraph.stream(input)
                .doOnNext(output -> {
                    if (output instanceof StreamingOutput<?>) {
                        StreamingOutput<?> so = (StreamingOutput<?>) output;
                        completedNodes.add(so.node());
                        sink.tryEmitNext(new WorkflowProgress(
                                so.node(),
                                new ArrayList<>(completedNodes),
                                so.state().data()
                        ));
                    }
                })
                .doOnComplete(() -> sink.tryEmitComplete())
                .doOnError(sink::tryEmitError)
                .subscribe();

        return sink.asFlux();
    }

    /**
     * 构建并返回编译后的工作流
     */
    public CompiledGraph compile(StateGraph workflow, CompileConfig compileConfig) {
        try {
            return workflow.compile(compileConfig);
        } catch (GraphStateException e) {
            throw new RuntimeException("工作流编译失败", e);
        }
    }

    private void logOutput(NodeOutput output) {
        if (output instanceof StreamingOutput<?>) {
            StreamingOutput<?> streamingOutput = (StreamingOutput<?>) output;
            if (streamingOutput.message() != null) {
                log.info("[流式输出] 节点 {}: {}",
                        streamingOutput.node(), streamingOutput.message().getText());
            } else {
                log.info("[节点输出] 节点 {}: 状态更新", streamingOutput.node());
            }
        }
    }

    /**
     * 工作流进度数据
     */
    public record WorkflowProgress(String currentNode, List<String> completedNodes, Map<String, Object> state) {
    }
}
