package com.luren.aicvoptimize.dto;

import com.luren.aicvoptimize.workflow.WorkflowExecutor.WorkflowResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流执行结果响应（交互式模式）
 * <p>
 * 封装 {@link WorkflowResult}，区分正常完成和中断暂停两种状态，
 * 便于前端根据 {@code interrupted} 字段决定是展示结果还是弹出职位选择界面。
 */
@Data
@RequiredArgsConstructor
public class WorkflowResultResponse {

    /**
     * 是否被中断暂停（true 则需通过 resume 接口恢复）
     */
    private final boolean interrupted;

    /**
     * 中断发生的节点名称（未中断时为 null）
     */
    private final String interruptedNode;

    /**
     * 当前工作流状态数据（已过滤内部字段）
     */
    private final Map<String, Object> state;

    /**
     * 当前执行/完成的节点名称
     */
    private String currentNode;

    /**
     * 已完成的节点列表
     */
    private List<String> completedNodes;

    /**
     * 是否等待人工输入
     */
    private boolean waitingForInput;

    /**
     * 从 WorkflowResult 构建响应
     */
    public static WorkflowResultResponse from(WorkflowResult result) {
        Map<String, Object> filteredState = result.getState().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        WorkflowResultResponse response = new WorkflowResultResponse(
                result.isInterrupted(),
                result.getInterruptedNode(),
                filteredState
        );
        response.setCurrentNode(result.getCurrentNode());
        response.setCompletedNodes(result.getCompletedNodes());
        response.setWaitingForInput(result.isInterrupted());
        return response;
    }

    /**
     * 从 WorkflowResult 构建响应，并注入 threadId
     */
    public static WorkflowResultResponse from(WorkflowResult result, String threadId) {
        Map<String, Object> filteredState = new HashMap<>(result.getState());
        // 将 threadId 注入到 state 中，方便前端获取
        if (threadId != null) {
            filteredState.put("threadId", threadId);
        }
        WorkflowResultResponse response = new WorkflowResultResponse(
                result.isInterrupted(),
                result.getInterruptedNode(),
                filteredState
        );
        response.setCurrentNode(result.getCurrentNode());
        response.setCompletedNodes(result.getCompletedNodes());
        response.setWaitingForInput(result.isInterrupted());
        return response;
    }
}
