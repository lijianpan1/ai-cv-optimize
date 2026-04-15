package com.luren.aicvoptimize.service;

import com.luren.aicvoptimize.dto.WorkflowProgressEvent;
import com.luren.aicvoptimize.dto.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流进度服务
 * <p>
 * 管理 SSE 连接和进度推送
 */
@Slf4j
@Service
public class WorkflowProgressService {

    /**
     * 存储所有活跃的 SSE 连接
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 节点名称到显示名称的映射
     */
    private static final Map<String, String> NODE_DISPLAY_NAMES = Map.of(
            "resume_parser", "解析简历",
            "job_extraction", "职位匹配",
            "job_selection", "确认职位",
            "resume_diagnosis", "简历诊断",
            "resume_optimization", "智能优化",
            "adversarial_optimization", "对抗优化",
            "resume_markdown", "格式转换",
            "file_generation", "生成文件",
            "interview_rehearsal", "面试预演"
    );

    /**
     * 节点执行顺序
     */
    private static final List<String> NODE_ORDER = List.of(
            "resume_parser",
            "job_extraction",
            "job_selection",
            "resume_diagnosis",
            "resume_optimization",
            "adversarial_optimization",
            "resume_markdown",
            "file_generation",
            "interview_rehearsal"
    );

    /**
     * 创建 SSE 连接
     *
     * @param clientId 客户端唯一标识
     * @return SseEmitter 实例
     */
    public SseEmitter createEmitter(String clientId) {
        // 如果已存在，先移除旧的
        removeEmitter(clientId);

        // 创建新的 emitter，设置超时时间为10分钟
        SseEmitter emitter = new SseEmitter(600000L);

        emitter.onCompletion(() -> {
            log.info("SSE 连接完成: {}", clientId);
            emitters.remove(clientId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时: {}", clientId);
            emitters.remove(clientId);
        });

        emitter.onError((e) -> {
            log.error("SSE 连接错误: {}", clientId, e);
            emitters.remove(clientId);
        });

        emitters.put(clientId, emitter);
        log.info("创建 SSE 连接: {}", clientId);

        // 发送连接成功事件
        sendEvent(clientId, Map.of("type", "connected", "clientId", clientId));

        return emitter;
    }

    /**
     * 移除 SSE 连接
     */
    public void removeEmitter(String clientId) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("关闭旧 SSE 连接时出错: {}", clientId);
            }
        }
    }

    /**
     * 发送进度事件
     */
    public void sendProgress(String clientId, String currentNode, List<String> completedNodes) {
        int currentIndex = NODE_ORDER.indexOf(currentNode) + 1;
        if (currentIndex <= 0) {
            currentIndex = completedNodes.size();
        }

        // 根据 currentNode 推断所有应该完成的节点
        // 确保中间没有遗漏的节点
        List<String> inferredCompletedNodes = new ArrayList<>(completedNodes);
        if (currentIndex > 1) {
            for (int i = 0; i < currentIndex - 1; i++) {
                String node = NODE_ORDER.get(i);
                if (!inferredCompletedNodes.contains(node)) {
                    inferredCompletedNodes.add(node);
                    log.info("[进度推断] 添加遗漏的节点: {}", node);
                }
            }
        }

        String displayName = NODE_DISPLAY_NAMES.getOrDefault(currentNode, currentNode);
        WorkflowProgressEvent event = WorkflowProgressEvent.progress(
                currentNode, inferredCompletedNodes, NODE_ORDER.size(), currentIndex, displayName
        );

        sendEvent(clientId, event);
    }

    /**
     * 发送开始事件
     */
    public void sendStart(String clientId) {
        WorkflowProgressEvent event = WorkflowProgressEvent.start(NODE_ORDER.size());
        sendEvent(clientId, event);
    }

    /**
     * 发送完成事件
     */
    public void sendComplete(String clientId, WorkflowState state) {
        WorkflowProgressEvent event = WorkflowProgressEvent.complete(state);
        sendEvent(clientId, event);
        // 完成后关闭连接
        removeEmitter(clientId);
    }

    /**
     * 发送错误事件
     */
    public void sendError(String clientId, String message) {
        WorkflowProgressEvent event = WorkflowProgressEvent.error(message);
        sendEvent(clientId, event);
        removeEmitter(clientId);
    }

    /**
     * 发送等待输入事件
     */
    public void sendWaiting(String clientId, String currentNode) {
        String displayName = NODE_DISPLAY_NAMES.getOrDefault(currentNode, currentNode);
        WorkflowProgressEvent event = WorkflowProgressEvent.waiting(currentNode, displayName);
        sendEvent(clientId, event);
    }

    /**
     * 发送通用事件
     */
    public void sendEvent(String clientId, Object event) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            log.warn("未找到 SSE 连接: {}", clientId);
            return;
        }

        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                    .name("workflow-progress")
                    .data(event);
            emitter.send(eventBuilder);
            log.debug("发送事件到 {}: {}", clientId, event);
        } catch (IOException e) {
            log.error("发送 SSE 事件失败: {}", clientId, e);
            removeEmitter(clientId);
        }
    }

    /**
     * 获取节点显示名称
     */
    public String getNodeDisplayName(String nodeName) {
        return NODE_DISPLAY_NAMES.getOrDefault(nodeName, nodeName);
    }

    /**
     * 获取节点顺序列表
     */
    public List<String> getNodeOrder() {
        return NODE_ORDER;
    }
}
