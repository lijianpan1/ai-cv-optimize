package com.luren.aicvoptimize.workflow;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 工作流执行器
 *
 * @author lijianpan
 **/
@Slf4j
@Component
public class WorkflowExecutor {

    /**
     * 执行工作流（无中断、无持久化）
     */
    public Map<String, Object> execute(StateGraph workflow, Map<String, Object> input) {
        return execute(workflow, input, CompileConfig.builder().build());
    }

    /**
     * 执行工作流（指定中断点节点）
     */
    public Map<String, Object> execute(StateGraph workflow, Map<String, Object> input,
                                       Set<String> interruptBeforeNodes, Set<String> interruptAfterNodes) {
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
        return execute(workflow, input, builder.build());
    }

    /**
     * 执行工作流（完全控制编译配置）
     */
    public Map<String, Object> execute(StateGraph workflow, Map<String, Object> input,
                                       CompileConfig compileConfig) {
        CompiledGraph compiledGraph = compile(workflow, compileConfig);
        log.info("开始执行工作流，输入 keys: {}", input.keySet());
        return streamAndCollect(compiledGraph, input);
    }

    /**
     * 执行工作流并返回中断结果（支持中断恢复模式）
     * <p>
     * 如果工作流在配置的中断点暂停，返回 {@link WorkflowResult#isInterrupted()} == true，
     * 调用方可通过 {@link WorkflowResult#getState()} 获取当前状态，
     * 再调用 {@link #resume(CompiledGraph, RunnableConfig, Map)} 恢复执行。
     *
     * @param workflow 工作流定义
     * @param input    初始输入状态
     * @param config   运行时配置（必须包含 threadId 以支持状态持久化）
     * @return 工作流执行结果
     */
    public WorkflowResult executeForResult(StateGraph workflow, Map<String, Object> input,
                                           CompileConfig compileConfig, RunnableConfig config) {
        CompiledGraph compiledGraph = compile(workflow, compileConfig);
        log.info("开始执行工作流（中断恢复模式），threadId: {}", config.threadId().orElse("unknown"));

        NodeOutput lastOutput = null;
        List<String> completedNodes = new CopyOnWriteArrayList<>();
        String[] currentNode = new String[1];

        lastOutput = compiledGraph.stream(input, config)
                .doOnNext(output -> {
                    logOutput(output);
                    // 捕获节点执行信息
                    if (output instanceof StreamingOutput<?> so) {
                        String nodeName = so.node();
                        
                        // 如果这是第一个节点，记录日志
                        if (currentNode[0] == null) {
                            log.info("[节点进度] 第一个节点开始执行: {}", nodeName);
                        }
                        
                        // 如果这是一个新节点，将之前的节点标记为已完成
                        if (currentNode[0] != null && !currentNode[0].equals(nodeName)) {
                            if (!completedNodes.contains(currentNode[0])) {
                                completedNodes.add(currentNode[0]);
                            }
                            log.info("[节点进度] 切换到新节点: {}, 已完成: {}", nodeName, completedNodes);
                        }
                        currentNode[0] = nodeName;
                    }
                })
                .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
                .doOnComplete(() -> System.out.println("流完成"))
                .blockLast();

        if (lastOutput instanceof InterruptionMetadata interruption) {
            log.info("工作流在节点 [{}] 处中断", interruption.node());
            return WorkflowResult.interrupted(interruption.state().data(), interruption.node(), 
                    currentNode[0], new ArrayList<>(completedNodes));
        }

        return WorkflowResult.completed(lastOutput.state().data(), currentNode[0], new ArrayList<>(completedNodes));
    }

    /**
     * 从中断点恢复执行
     * <p>
     * 通过 RunnableConfig.builder 的 addStateUpdate 注入用户选择的数据，
     * 使用 resume 标记从断点恢复。
     *
     * @param compiledGraph  已编译的工作流
     * @param config         上次执行时的 RunnableConfig（相同 threadId）
     * @param stateUpdate    要注入的状态更新（如用户选择的 target_job）
     * @return 工作流执行结果
     */
    public WorkflowResult resume(CompiledGraph compiledGraph, RunnableConfig config, Map<String, Object> stateUpdate) {
        RunnableConfig resumeConfig = RunnableConfig.builder(config)
//                .addStateUpdate(stateUpdate)
                .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
                .resume()
                .build();

        log.info("从断点恢复执行，threadId: {}，更新 keys: {}",
                config.threadId().orElse("unknown"), stateUpdate.keySet());

        RunnableConfig updatedConfig = null;
        try {
            updatedConfig = compiledGraph.updateState(config, stateUpdate);
        } catch (Exception e) {
            log.error("更新状态失败", e);
            throw new RuntimeException(e);
        }

        List<String> completedNodes = new CopyOnWriteArrayList<>();
        String[] currentNode = new String[1];

        NodeOutput lastOutput = compiledGraph.stream(Map.of(), updatedConfig)
                .doOnNext(output -> {
                    logOutput(output);
                    // 捕获节点执行信息
                    if (output instanceof StreamingOutput<?> so) {
                        String nodeName = so.node();
                        
                        // 如果这是第一个节点，记录日志
                        if (currentNode[0] == null) {
                            log.info("[节点进度-恢复] 第一个节点开始执行: {}", nodeName);
                        }
                        
                        // 如果这是一个新节点，将之前的节点标记为已完成
                        if (currentNode[0] != null && !currentNode[0].equals(nodeName)) {
                            if (!completedNodes.contains(currentNode[0])) {
                                completedNodes.add(currentNode[0]);
                            }
                            log.info("[节点进度-恢复] 切换到新节点: {}, 已完成: {}", nodeName, completedNodes);
                        }
                        currentNode[0] = nodeName;
                    }
                })
                .blockLast();

        if (lastOutput instanceof InterruptionMetadata interruption) {
            log.info("工作流在节点 [{}] 处再次中断", interruption.node());
            return WorkflowResult.interrupted(interruption.state().data(), interruption.node(),
                    currentNode[0], new ArrayList<>(completedNodes));
        }

        return WorkflowResult.completed(lastOutput.state().data(), currentNode[0], new ArrayList<>(completedNodes));
    }

    /**
     * 构建并返回编译后的工作流（供外部持有以便后续恢复）
     */
    public CompiledGraph compile(StateGraph workflow, CompileConfig compileConfig) {
        try {
            return workflow.compile(compileConfig);
        } catch (GraphStateException e) {
            throw new RuntimeException("工作流编译失败", e);
        }
    }

    private Map<String, Object> streamAndCollect(CompiledGraph compiledGraph, Map<String, Object> input) {
        NodeOutput lastOutput = compiledGraph.stream(input)
                .doOnNext(this::logOutput)
                .blockLast();
        return lastOutput.state().data();
    }

    private void logOutput(NodeOutput output) {
        if (output instanceof StreamingOutput<?>) {
            StreamingOutput<?> streamingOutput = (StreamingOutput<?>) output;
            if (streamingOutput.message() != null) {
                log.info("[流式输出] 节点 {}: {}",
                        streamingOutput.node(), streamingOutput.message().getText());
            } else {
                log.info("[节点输出] 节点 {}: 状态更新",
                        streamingOutput.node());
            }
        }
    }

    /**
     * 工作流执行结果（区分正常完成和中断暂停）
     */
    public static class WorkflowResult {
        private final boolean interrupted;
        private final String interruptedNode;
        private final Map<String, Object> state;
        private final String currentNode;
        private final List<String> completedNodes;

        private WorkflowResult(boolean interrupted, String interruptedNode, Map<String, Object> state,
                               String currentNode, List<String> completedNodes) {
            this.interrupted = interrupted;
            this.interruptedNode = interruptedNode;
            this.state = state;
            this.currentNode = currentNode;
            this.completedNodes = completedNodes != null ? completedNodes : new ArrayList<>();
        }

        public static WorkflowResult completed(Map<String, Object> state) {
            return new WorkflowResult(false, null, state, null, new ArrayList<>());
        }

        public static WorkflowResult completed(Map<String, Object> state, String currentNode, List<String> completedNodes) {
            return new WorkflowResult(false, null, state, currentNode, completedNodes);
        }

        public static WorkflowResult interrupted(Map<String, Object> state, String interruptedNode) {
            return new WorkflowResult(true, interruptedNode, state, interruptedNode, new ArrayList<>());
        }

        public static WorkflowResult interrupted(Map<String, Object> state, String interruptedNode,
                                                  String currentNode, List<String> completedNodes) {
            return new WorkflowResult(true, interruptedNode, state, currentNode, completedNodes);
        }

        public boolean isInterrupted() {
            return interrupted;
        }

        public boolean isCompleted() {
            return !interrupted;
        }

        public String getInterruptedNode() {
            return interruptedNode;
        }

        public Map<String, Object> getState() {
            return state;
        }

        public String getCurrentNode() {
            return currentNode;
        }

        public List<String> getCompletedNodes() {
            return completedNodes;
        }
    }
}
