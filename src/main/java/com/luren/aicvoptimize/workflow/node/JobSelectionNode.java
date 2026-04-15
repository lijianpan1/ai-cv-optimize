package com.luren.aicvoptimize.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luren.aicvoptimize.dto.JobRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 目标职位选择节点
 * <p>
 * 从职位提取节点输出的 job_info 中解析出推荐职位列表，
 * 供人工介入选择目标职位后，将选中的职位信息写入状态。
 * <p>
 * 当状态中已有 target_job 时（即人工已选择），直接透传；
 * 否则默认选择匹配度最高的职位（matchScore 最高的一项）作为 fallback。
 *
 * @author lijianpan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobSelectionNode implements NodeAction {

    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("=== 开始执行：目标职位选择节点 ===");

        // 如果上游已经设置了 target_job（人工选择），直接透传
        if (state.value("target_job").isPresent()) {
            String targetJob = (String) state.value("target_job").get();
            log.info("已存在人工选择的目标职位，直接透传");
            return Map.of("target_job", targetJob);
        }

        // 从 job_info 中解析推荐职位列表
        JobRecommendationResponse jobInfo = (JobRecommendationResponse) state.value("job_info")
                .orElseThrow(() -> new IllegalArgumentException("缺少 job_info，请先执行职位提取节点"));

        if (jobInfo == null || jobInfo.getRecommendedJobs().isEmpty()) {
            throw new IllegalStateException("职位提取结果中没有推荐职位列表");
        }

        // 默认选择匹配度最高的职位
        JobRecommendationResponse.RecommendedJob topJob = jobInfo.getRecommendedJobs().stream()
                .max(Comparator.comparingInt(JobRecommendationResponse.RecommendedJob::getMatchScore))
                .orElse(jobInfo.getRecommendedJobs().getFirst());

        String targetJobJson = objectMapper.writeValueAsString(topJob);
        log.info("默认选择匹配度最高的目标职位: {}", topJob.getJobTitle());

        return Map.of("target_job", targetJobJson);
    }
}
