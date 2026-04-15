package com.luren.aicvoptimize.workflow.process;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.luren.aicvoptimize.dto.ResumeFileInfo;
import com.luren.aicvoptimize.service.WorkflowProgressService;
import com.luren.aicvoptimize.workflow.StreamingWorkflowExecutor;
import com.luren.aicvoptimize.workflow.WorkflowExecutor;
import com.luren.aicvoptimize.workflow.node.*;
import com.luren.aicvoptimize.workflow.node.parser.DocumentParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 简历优化工作流
 * <p>
 * 七节点线性工作流：
 * <pre>
 *     START -> 简历解析 -> 职位提取 -> 目标职位选择 -> 简历诊断 -> 简历优化 -> Markdown转换 -> 文件生成 -> END
 * </pre>
 *
 * @author lijianpan
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ResumeOptimizWorkflow {

    /**
     * 工作流节点定义（名称 + 默认中断配置）
     * <p>
     * 扩展节点时，在此枚举中新增一项并标记中断配置即可，
     * 后续在 {@link #PROCESSING_NODES} 和 {@link #resolveNodeAction(ResumeNode)} 中补充对应条目。
     */
    public enum ResumeNode {
        RESUME_PARSER("resume_parser", false, false),
        JOB_EXTRACTION("job_extraction", false, true),  // 在职位提取后中断，让用户选择目标职位
        JOB_SELECTION("job_selection", false, false),
        RESUME_DIAGNOSIS("resume_diagnosis", false, false),
        RESUME_OPTIMIZATION("resume_optimization", false, false),
        ADVERSARIAL_OPTIMIZATION("adversarial_optimization", false, false),  // 对抗性优化
        RESUME_MARKDOWN("resume_markdown", false, false),
        FILE_GENERATION("file_generation", false, false),
        INTERVIEW_REHEARSAL("interview_rehearsal", false, false);  // 面试预演

        private final String nodeName;
        private final boolean interruptBeforeDefault;
        private final boolean interruptAfterDefault;

        ResumeNode(String nodeName, boolean interruptBeforeDefault, boolean interruptAfterDefault) {
            this.nodeName = nodeName;
            this.interruptBeforeDefault = interruptBeforeDefault;
            this.interruptAfterDefault = interruptAfterDefault;
        }

        public String getNodeName() {
            return nodeName;
        }

        /** 是否为默认中断点（节点执行前暂停） */
        public boolean isInterruptBeforeDefault() {
            return interruptBeforeDefault;
        }

        /** 是否为默认中断点（节点执行后暂停） */
        public boolean isInterruptAfterDefault() {
            return interruptAfterDefault;
        }

        /** 收集所有标记为执行前默认中断的节点名称 */
        public static Set<String> defaultInterruptBefore() {
            return Arrays.stream(values())
                    .filter(ResumeNode::isInterruptBeforeDefault)
                    .map(ResumeNode::getNodeName)
                    .collect(Collectors.toUnmodifiableSet());
        }

        /** 收集所有标记为执行后默认中断的节点名称 */
        public static Set<String> defaultInterruptAfter() {
            return Arrays.stream(values())
                    .filter(ResumeNode::isInterruptAfterDefault)
                    .map(ResumeNode::getNodeName)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * 通用处理节点执行顺序（不含 file_parser，file_parser 按模式条件添加）
     * <p>
     * 扩展节点时，在此列表中按顺序追加即可，边定义由列表顺序自动推导。
     */
    private static final List<ResumeNode> PROCESSING_NODES = List.of(
            ResumeNode.RESUME_PARSER,
            ResumeNode.JOB_EXTRACTION,
            ResumeNode.JOB_SELECTION,
            ResumeNode.RESUME_DIAGNOSIS,
            ResumeNode.RESUME_OPTIMIZATION,
            ResumeNode.ADVERSARIAL_OPTIMIZATION,
            ResumeNode.RESUME_MARKDOWN,
            ResumeNode.FILE_GENERATION,
            ResumeNode.INTERVIEW_REHEARSAL
    );

    private static final Set<String> DEFAULT_INTERRUPT_BEFORE = ResumeNode.defaultInterruptBefore();
    private static final Set<String> DEFAULT_INTERRUPT_AFTER = ResumeNode.defaultInterruptAfter();

    private final WorkflowExecutor workflowExecutor;
    private final StreamingWorkflowExecutor streamingExecutor;
    private final WorkflowProgressService progressService;
    private final DocumentParserRegistry parserRegistry;
    private final ResumeParserNode resumeParserNode;
    private final JobExtractionNode jobExtractionNode;
    private final JobSelectionNode jobSelectionNode;
    private final ResumeDiagnosisNode resumeDiagnosisNode;
    private final ResumeOptimizationNode resumeOptimizationNode;
    private final AdversarialOptimizationNode adversarialOptimizationNode;
    private final ResumeMarkdownNode resumeMarkdownNode;
    private final FileGenerationNode fileGenerationNode;
    private final InterviewRehearsalNode interviewRehearsalNode;

    /**
     * 交互式工作流共享的检查点存储器
     * <p>
     * start() 和 resume() 必须使用同一个 MemorySaver 实例，
     * 否则 resume 时会因找不到 checkpoint 而抛出
     * {@code IllegalStateException: Resume request without a valid checkpoint!}
     */
    private final MemorySaver memorySaver = new MemorySaver();

    // ==================== 交互式执行（中断-恢复模式） ====================

    /**
     * 启动工作流（基于文件上传），在默认中断点暂停返回当前状态
     * <p>
     * 先将文件解析为文本，再委托给纯文本模式执行，避免 MultipartFile 进入工作流状态。
     *
     * @param file 简历文件
     * @return 工作流执行结果（包含 threadId）
     */
    public StartResult start(MultipartFile file) {
        ResumeFileInfo fileInfo = parseFile(file);
        return start(fileInfo.getText(), fileInfo.getBaseName());
    }

    /**
     * 启动工作流（基于纯文本输入），在默认中断点暂停返回当前状态
     *
     * @param resumeText 原始简历文本
     * @param baseName   文件基础名（不含扩展名），传 null 则自动生成
     * @return 工作流执行结果（包含 threadId）
     */
    public StartResult start(String resumeText, String baseName) {
        log.info("启动交互式简历优化工作流（纯文本模式），简历文本长度: {}", resumeText.length());
        try {
            StateGraph workflow = buildWorkflowWithText();
            CompileConfig compileConfig = buildInterruptCompileConfig();
            String threadId = UUID.randomUUID().toString();
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();
            WorkflowExecutor.WorkflowResult result = workflowExecutor.executeForResult(workflow, buildInputMap(resumeText, baseName), compileConfig, config);
            return new StartResult(result, threadId);
        } catch (GraphStateException e) {
            throw new RuntimeException("工作流构建失败", e);
        }
    }

    /**
     * 启动结果，包含工作流执行结果和 threadId
     */
    public record StartResult(WorkflowExecutor.WorkflowResult workflowResult, String threadId) {

    }

    /**
     * 用户选择目标职位后恢复工作流执行
     * <p>
     * 将用户选择的职位 JSON 注入到状态中的 {@code target_job} 字段，
     * 然后从断点恢复执行后续节点（简历诊断 -> 简历优化）。
     * <p>
     * 如果用户不选择（传 {@code null}），{@link JobSelectionNode} 会自动
     * 选择匹配度最高的职位作为 fallback。
     *
     * @param threadId       会话 ID（由 {@link #start} 时的 RunnableConfig 产生，需外部持有）
     * @param targetJobJson  用户选择的目标职位 JSON，传 {@code null} 表示使用默认最高分职位
     * @return 工作流执行结果（可能再次中断或已完成）
     */
    public WorkflowExecutor.WorkflowResult resumeWithJobSelection(String threadId, String targetJobJson) {
        log.info("恢复工作流，threadId: {}，用户选择: {}",
                threadId, targetJobJson != null ? "已选择" : "使用默认最高分职位");
        try {
            StateGraph workflow = buildWorkflowWithText();
            CompileConfig compileConfig = buildInterruptCompileConfig();
            CompiledGraph compiledGraph = workflowExecutor.compile(workflow, compileConfig);

            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();

            Map<String, Object> stateUpdate = targetJobJson != null
                    ? Map.of("target_job", (Object) targetJobJson)
                    : Map.of();

            return workflowExecutor.resume(compiledGraph, config, stateUpdate);
        } catch (GraphStateException e) {
            throw new RuntimeException("工作流构建失败", e);
        }
    }

    // ==================== 交付式执行（一键完成，无需交互） ====================

    /**
     * 交付式执行（基于文件上传）
     * <p>
     * 先将文件解析为文本，再委托给纯文本模式执行，避免 MultipartFile 进入工作流状态。
     *
     * @param file 简历文件
     * @return 完整的工作流状态（含简历诊断、优化结果）
     */
    public Map<String, Object> deliver(MultipartFile file) {
        ResumeFileInfo fileInfo = parseFile(file);
        return deliver(fileInfo.getText(), fileInfo.getBaseName());
    }

    /**
     * 交付式执行（基于纯文本输入）
     * <p>
     * 无中断、无持久化，工作流从简历解析到简历优化一键跑完。
     * 目标职位由 {@link JobSelectionNode} 自动选择匹配度最高的职位。
     *
     * @param resumeText 原始简历文本
     * @param baseName   文件基础名（不含扩展名），传 null 则自动生成
     * @return 完整的工作流状态（含简历诊断、优化结果）
     */
    public Map<String, Object> deliver(String resumeText, String baseName) {
        log.info("启动交付式简历优化工作流（纯文本模式），简历文本长度: {}", resumeText.length());
        try {
            StateGraph workflow = buildWorkflowWithText();
            Map<String, Object> input = buildInputMap(resumeText, baseName);
            return workflowExecutor.execute(workflow, input);
        } catch (GraphStateException e) {
            throw new RuntimeException("工作流构建失败", e);
        }
    }

    // ==================== 工作流构建 ====================

    /**
     * 将上传文件解析为文本及基本信息（在创建工作流状态之前调用，避免 MultipartFile 进入图状态）
     */
    private ResumeFileInfo parseFile(MultipartFile file) {
        log.info("解析文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
        String text = parserRegistry.parse(file);
        log.info("文件解析完成，提取文本长度: {}", text.length());
        return new ResumeFileInfo(file.getOriginalFilename(), text, file.getSize());
    }

    /**
     * 解析文件获取文本内容（公开方法，供控制器使用）
     */
    public String parseFileForText(MultipartFile file) {
        return parseFile(file).getText();
    }

    /**
     * 解析文件获取基础名称（公开方法，供控制器使用）
     */
    public String parseFileForBaseName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        }
        return originalFilename;
    }

    // ==================== 流式执行（带实时进度推送） ====================

    /**
     * 交付式执行（基于文件上传）- 带实时进度推送
     */
    public Map<String, Object> deliverWithStreaming(MultipartFile file, String clientId) {
        ResumeFileInfo fileInfo = parseFile(file);
        return deliverWithStreaming(fileInfo.getText(), fileInfo.getBaseName(), clientId);
    }

    /**
     * 交付式执行（基于纯文本输入）- 带实时进度推送
     */
    public Map<String, Object> deliverWithStreaming(String resumeText, String baseName, String clientId) {
        log.info("启动交付式简历优化工作流（流式模式），clientId: {}, 简历文本长度: {}", clientId, resumeText.length());
        try {
            StateGraph workflow = buildWorkflowWithText();
            Map<String, Object> input = buildInputMap(resumeText, baseName);
            return streamingExecutor.executeWithStreaming(workflow, input, clientId);
        } catch (GraphStateException e) {
            throw new RuntimeException("工作流构建失败", e);
        }
    }

    /**
     * 构建工作流输入 Map，包含 resume_text 和可选的 file_name
     */
    private Map<String, Object> buildInputMap(String resumeText, String baseName) {
        if(resumeText.length()>100||baseName.length()>50){
            throw new IllegalArgumentException("输入文本长度超过限制");
        }

        if (baseName != null && !baseName.isBlank()) {
            return Map.of("resume_text", (Object) resumeText, "file_name", baseName);
        }
        return Map.of("resume_text", resumeText);
    }

    /**
     * 构建带默认中断点和内存持久化的编译配置（使用共享的 MemorySaver）
     */
    private CompileConfig buildInterruptCompileConfig() {
        return CompileConfig.builder()
                .interruptBefore(DEFAULT_INTERRUPT_BEFORE.toArray(new String[0]))
                .interruptAfter(DEFAULT_INTERRUPT_AFTER.toArray(new String[0]))
                .saverConfig(SaverConfig.builder()
                        .register(memorySaver)
                        .build())
                .build();
    }

    /**
     * 构建纯文本模式工作流图
     */
    private StateGraph buildWorkflowWithText() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();
        StateGraph workflow = new StateGraph(keyStrategyFactory);

        addProcessingNodes(workflow);

        workflow.addEdge(StateGraph.START, PROCESSING_NODES.getFirst().getNodeName());
        addProcessingEdges(workflow);

        return workflow;
    }

    /**
     * 按 {@link #PROCESSING_NODES} 顺序注册所有通用处理节点
     */
    private void addProcessingNodes(StateGraph workflow) throws GraphStateException {
        for (ResumeNode node : PROCESSING_NODES) {
            workflow.addNode(node.getNodeName(), node_async(resolveNodeAction(node)));
        }
    }

    /**
     * 按 {@link #PROCESSING_NODES} 顺序自动推导相邻节点间的边，末尾节点连接 END
     */
    private void addProcessingEdges(StateGraph workflow) throws GraphStateException {
        for (int i = 0; i < PROCESSING_NODES.size(); i++) {
            String from = PROCESSING_NODES.get(i).getNodeName();
            String to = (i + 1 < PROCESSING_NODES.size())
                    ? PROCESSING_NODES.get(i + 1).getNodeName()
                    : StateGraph.END;
            workflow.addEdge(from, to);
        }
    }

    /**
     * 枚举值到 Spring Bean 的映射
     * <p>
     * 扩展节点时，在此 switch 中补充新的 case 即可。
     */
    private NodeAction resolveNodeAction(ResumeNode node) {
        switch (node) {
            case RESUME_PARSER:
                return resumeParserNode;
            case JOB_EXTRACTION:
                return jobExtractionNode;
            case JOB_SELECTION:
                return jobSelectionNode;
            case RESUME_DIAGNOSIS:
                return resumeDiagnosisNode;
            case RESUME_OPTIMIZATION:
                return resumeOptimizationNode;
            case ADVERSARIAL_OPTIMIZATION:
                return adversarialOptimizationNode;
            case RESUME_MARKDOWN:
                return resumeMarkdownNode;
            case FILE_GENERATION:
                return fileGenerationNode;
            case INTERVIEW_REHEARSAL:
                return interviewRehearsalNode;
            default:
                throw new IllegalArgumentException("未知的节点类型: " + node);
        }
    }

    /**
     * 创建状态管理策略工厂
     *
     */
    private KeyStrategyFactory createKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("file_name", new ReplaceStrategy());
            putCommonKeyStrategies(strategies);
            return strategies;
        };
    }

    /**
     * 注册通用的状态 key 策略
     */
    private void putCommonKeyStrategies(HashMap<String, KeyStrategy> strategies) {
        strategies.put("resume_text", new ReplaceStrategy());
        strategies.put("resume_json", new ReplaceStrategy());
        strategies.put("job_info", new ReplaceStrategy());
        strategies.put("target_job", new ReplaceStrategy());
        strategies.put("diagnosis_result", new ReplaceStrategy());
        strategies.put("optimized_resume_json", new ReplaceStrategy());
        strategies.put("adversarial_optimization_result", new ReplaceStrategy());
        strategies.put("optimized_resume", new ReplaceStrategy());
        strategies.put("pdf_path", new ReplaceStrategy());
        strategies.put("word_path", new ReplaceStrategy());
        strategies.put("pdf_download_url", new ReplaceStrategy());
        strategies.put("word_download_url", new ReplaceStrategy());
        strategies.put("interview_session", new ReplaceStrategy());
    }
}
