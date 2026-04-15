package com.luren.aicvoptimize.agents;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.config.OutputSchemaManager;
import com.luren.aicvoptimize.prompt.PromptKey;
import com.luren.aicvoptimize.prompt.PromptManager;
import com.luren.aicvoptimize.tools.DocumentSearchTool;
import com.luren.aicvoptimize.tools.SystemTimeTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能体工厂
 * <p>
 * 统一管理所有智能体的创建逻辑，支持模板变量渲染。
 * 各智能体组件通过本工厂创建 ReactAgent 实例，避免在各节点中重复构建。
 *
 * @author lijianpan
 */
@Component
@RequiredArgsConstructor
public class AgentFactory {

    private final ChatModel chatModel;
    private final PromptManager promptManager;
    private final OutputSchemaManager schemaManager;
    private final SystemTimeTool systemTimeTool;
    private final DocumentSearchTool documentSearchTool;

    /**
     * 创建智能体（带模板变量渲染，有输出Schema）
     *
     * @param name             智能体名称
     * @param promptKey        Prompt模板Key
     * @param variables        模板变量（为null或空时使用原始模板）
     * @param outputSchemaType 输出Schema类型（为null时不设置Schema）
     * @param outputKey        输出Key
     * @param toolCallback     工具回调
     * @return ReactAgent实例
     */
    public ReactAgent createAgent(String name, PromptKey promptKey,
                                  Map<String, Object> variables,
                                  Class<?> outputSchemaType, String outputKey,
                                  List<ToolCallback> toolCallback) {
        String instruction = (variables != null && !variables.isEmpty())
                ? promptManager.render(promptKey, variables)
                : promptManager.get(promptKey);

        Builder builder = ReactAgent.builder()
                .name(name)
                .model(chatModel)
                .instruction(instruction)
                .outputKey(outputKey);

        if (outputSchemaType != null) {
            builder.outputSchema(schemaManager.getSchema(outputSchemaType));
        }

        // 合并系统工具和用户工具
        List<ToolCallback> allTools = new ArrayList<>();
        // 添加系统时间工具（所有智能体都可用）
        allTools.add(FunctionToolCallback
                .builder("get_current_date", systemTimeTool::getCurrentDate)
                .description("获取当前系统日期（yyyy-MM-dd格式）。用于判断简历中的项目时间、工作经历等日期是否合理。")
                .inputType(Void.class)
                .build());
//        allTools.add(FunctionToolCallback
//                .builder("get_current_time", systemTimeTool::getCurrentTime)
//                .description("获取当前系统时间（包含日期和时间）。用于判断简历中的日期是否合理，如项目时间是否在未来等。")
//                .inputType(Void.class)
//                .build());
        allTools.add(FunctionToolCallback
                .builder("search_optimization_document", documentSearchTool::searchOptimizationDocument)
                .description("深入检索简历优化的专业技巧、行业最佳实践及最新趋势。该工具旨在通过挖掘资深HR和招聘专家的独家建议，为用户提供一套系统性的简历优化方案。内容涵盖：从基础的关键词布局、排版美学，到进阶的成就量化（STAR法则）、个人品牌故事构建，以及如何针对不同岗位描述（JD）进行精准定制，帮助用户将一份普通的简历打磨成高竞争力的求职利器。")
                .inputType(DocumentSearchTool.OptimizationDocumentRequest.class)
                .build());
        allTools.add(FunctionToolCallback
                .builder("search_faqs", documentSearchTool::searchFAQs)
                .description("深入检索简历制作与投递过程中常见的高频痛点、典型误区及“致命”错误。该工具旨在通过挖掘招聘经理和HR的真实反馈，为用户提供一份详尽的“简历避雷清单”。内容涵盖：从基础的信息错漏、格式混乱，到深层的逻辑矛盾、自我评价空泛、工作经历流水账等问题，并提供针对性的修正方案与避坑策略，帮助用户消除简历中的减分项。")
                .inputType(DocumentSearchTool.FAQsRequest.class)
                .build());
        // 添加用户自定义工具
        if (toolCallback != null && !toolCallback.isEmpty()) {
            allTools.addAll(toolCallback);
        }
        
        if (!allTools.isEmpty()) {
            builder.tools(allTools);
        }

        return builder.build();
    }

    /**
     * 创建智能体（无模板变量，有输出Schema）
     */
    public ReactAgent createAgent(String name, PromptKey promptKey,
                                  Class<?> outputSchemaType, String outputKey) {
        return createAgent(name, promptKey, null, outputSchemaType, outputKey, null);
    }

    /**
     * 创建智能体（带模板变量，无输出Schema）
     */
    public ReactAgent createAgent(String name, PromptKey promptKey,
                                  Map<String, Object> variables, String outputKey) {
        return createAgent(name, promptKey, variables, null, outputKey, null);
    }

    /**
     * 创建智能体（带模板变量，有输出Schema）
     */
    public ReactAgent createAgent(String name, PromptKey promptKey,
                                  Map<String, Object> variables, Class<?> outputSchemaType, String outputKey) {
        return createAgent(name, promptKey, variables, outputSchemaType, outputKey, null);
    }
}
