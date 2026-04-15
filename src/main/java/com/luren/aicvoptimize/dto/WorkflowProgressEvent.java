package com.luren.aicvoptimize.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 工作流进度事件
 * <p>
 * 用于 SSE 实时推送节点执行进度到前端
 */
@Data
public class WorkflowProgressEvent {

    /**
     * 事件类型：start(开始), progress(进度), complete(完成), error(错误)
     */
    private String type;

    /**
     * 当前执行的节点名称
     */
    @JsonProperty("currentNode")
    private String currentNode;

    /**
     * 已完成的节点列表
     */
    @JsonProperty("completedNodes")
    private List<String> completedNodes;

    /**
     * 节点总数
     */
    @JsonProperty("totalNodes")
    private int totalNodes;

    /**
     * 当前节点索引（从1开始）
     */
    @JsonProperty("currentIndex")
    private int currentIndex;

    /**
     * 进度百分比 (0-100)
     */
    private int progress;

    /**
     * 节点显示名称
     */
    @JsonProperty("nodeDisplayName")
    private String nodeDisplayName;

    /**
     * 状态描述文本
     */
    private String message;

    /**
     * 是否等待人工输入
     */
    @JsonProperty("waitingForInput")
    private boolean waitingForInput;

    /**
     * 最终状态数据（仅在 complete 事件中有值）
     */
    private WorkflowState state;

    public WorkflowProgressEvent() {
    }

    public WorkflowProgressEvent(String type, String currentNode, List<String> completedNodes,
                                  int totalNodes, int currentIndex, String nodeDisplayName, String message) {
        this.type = type;
        this.currentNode = currentNode;
        this.completedNodes = completedNodes;
        this.totalNodes = totalNodes;
        this.currentIndex = currentIndex;
        this.nodeDisplayName = nodeDisplayName;
        this.message = message;
        this.progress = totalNodes > 0 ? (currentIndex * 100 / totalNodes) : 0;
    }

    /**
     * 创建开始事件
     */
    public static WorkflowProgressEvent start(int totalNodes) {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        event.setType("start");
        event.setTotalNodes(totalNodes);
        event.setCurrentIndex(0);
        event.setProgress(0);
        event.setMessage("工作流开始执行");
        return event;
    }

    /**
     * 创建进度事件
     */
    public static WorkflowProgressEvent progress(String currentNode, List<String> completedNodes,
                                                  int totalNodes, int currentIndex, String nodeDisplayName) {
        String message = String.format("正在执行：%s...", nodeDisplayName);
        return new WorkflowProgressEvent("progress", currentNode, completedNodes,
                totalNodes, currentIndex, nodeDisplayName, message);
    }

    /**
     * 创建完成事件
     */
    public static WorkflowProgressEvent complete(WorkflowState state) {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        event.setType("complete");
        event.setProgress(100);
        event.setMessage("工作流执行完成");
        event.setState(state);
        return event;
    }

    /**
     * 创建错误事件
     */
    public static WorkflowProgressEvent error(String message) {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        event.setType("error");
        event.setMessage(message);
        return event;
    }

    /**
     * 创建等待输入事件
     */
    public static WorkflowProgressEvent waiting(String currentNode, String nodeDisplayName) {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        event.setType("waiting");
        event.setCurrentNode(currentNode);
        event.setNodeDisplayName(nodeDisplayName);
        event.setWaitingForInput(true);
        event.setMessage("等待人工操作...");
        return event;
    }
}
