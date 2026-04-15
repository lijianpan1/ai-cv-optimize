package com.luren.aicvoptimize.controller;

import com.luren.aicvoptimize.service.UserQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户配额管理 REST API
 * <p>
 * 提供用户简历优化使用次数的查询和管理接口
 *
 * @author lijianpan
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quota")
public class UserQuotaController {

    private final UserQuotaService userQuotaService;

    /**
     * 获取当前用户剩余次数
     *
     * @return 配额信息
     */
    @GetMapping("/remaining")
    public Map<String, Object> getRemainingQuota() {
        int remaining = userQuotaService.getRemainingQuota();
        boolean hasQuota = userQuotaService.hasRemainingQuota();

        Map<String, Object> result = new HashMap<>();
        result.put("remaining", remaining);
        result.put("hasQuota", hasQuota);
        result.put("message", hasQuota ? "还有剩余次数" : "次数已用完");

        log.debug("查询用户剩余次数: {}", remaining);
        return result;
    }

    /**
     * 增加用户次数（管理员接口）
     *
     * @param count 增加的次数
     * @return 更新后的配额信息
     */
    @PostMapping("/increment")
    public Map<String, Object> incrementQuota(@RequestParam int count) {
        int remaining = userQuotaService.incrementQuota(count);

        Map<String, Object> result = new HashMap<>();
        result.put("remaining", remaining);
        result.put("increment", count);
        result.put("message", "成功增加 " + count + " 次使用次数");

        log.info("管理员增加用户次数: +{}, 剩余: {}", count, remaining);
        return result;
    }

    /**
     * 重置用户次数为默认值（管理员接口）
     *
     * @return 更新后的配额信息
     */
    @PostMapping("/reset")
    public Map<String, Object> resetQuota() {
        int remaining = userQuotaService.resetQuota();

        Map<String, Object> result = new HashMap<>();
        result.put("remaining", remaining);
        result.put("message", "已重置为默认次数");

        log.info("管理员重置用户次数为: {}", remaining);
        return result;
    }

    /**
     * 设置用户次数（管理员接口）
     *
     * @param count 设置的次数
     * @return 更新后的配额信息
     */
    @PostMapping("/set")
    public Map<String, Object> setQuota(@RequestParam int count) {
        int remaining = userQuotaService.setQuota(count);

        Map<String, Object> result = new HashMap<>();
        result.put("remaining", remaining);
        result.put("message", "已设置次数为 " + count);

        log.info("管理员设置用户次数为: {}", remaining);
        return result;
    }
}
