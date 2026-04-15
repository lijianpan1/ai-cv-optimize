package com.luren.aicvoptimize.service;

import com.luren.aicvoptimize.constant.RoleConstant;
import com.luren.aicvoptimize.entity.CvUser;
import com.luren.aicvoptimize.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户配额服务
 * <p>
 * 管理用户的简历优化使用次数配额
 *
 * @author lijianpan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQuotaService {

    /**
     * 默认免费使用次数
     */
    private static final int DEFAULT_FREE_QUOTA = 3;

    private final UserRepository userRepository;

    /**
     * 获取用户剩余次数
     *
     * @return 剩余次数
     */
    public int getRemainingQuota() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CvUser cvUser = userRepository.findByUsername(username).orElseThrow(
                () -> new RuntimeException("用户不存在: " + username)
        );


        if (RoleConstant.ADMIN.name().equals(cvUser.getRole())) {
            log.info("用户 {} 是管理员, 无限配额", username);
            return 9999;
        }

        log.info("用户 {} 剩余配额: {}", username, cvUser.getQuota());

        return cvUser.getQuota();
    }

    /**
     * 检查用户是否有剩余次数
     *
     * @return true 有剩余次数
     */
    public boolean hasRemainingQuota() {
        return getRemainingQuota() > 0;
    }

    /**
     * 减少用户次数
     *
     * @return 减少后的剩余次数
     */
    public int decrementQuota() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        CvUser cvUser = userRepository.findByUsername(username).orElseThrow(
                () -> new RuntimeException("用户不存在: " + username)
        );

        int lessenRemainNum = Math.max(0, cvUser.getQuota() - 1);

        userRepository.updateQuotaByUsername(lessenRemainNum, username);

        log.info("用户 {} 使用次数减少，剩余: {}", username, lessenRemainNum);
        return lessenRemainNum;
    }

    /**
     * 增加用户次数（用于管理员补充次数）
     *
     * @param count 增加的次数
     * @return 增加后的剩余次数
     */
    public int incrementQuota(int count) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        CvUser cvUser = userRepository.findByUsername(username).orElseThrow(
                () -> new RuntimeException("用户不存在: " + username)
        );

        int addRemainNum = count + cvUser.getQuota();

        userRepository.updateQuotaByUsername(addRemainNum, username);

        log.info("用户 {} 使用次数增加 {}，剩余: {}", username, count, addRemainNum);
        return addRemainNum;
    }

    /**
     * 重置用户次数为默认值
     *
     * @return 重置后的次数
     */
    public int resetQuota() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        userRepository.updateQuotaByUsername(DEFAULT_FREE_QUOTA, username);
        log.info("重置用户 {} 配额为: {}", username, DEFAULT_FREE_QUOTA);
        return DEFAULT_FREE_QUOTA;
    }

    /**
     * 设置用户次数
     *
     * @param count 次数
     * @return 设置后的次数
     */
    public int setQuota(int count) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        userRepository.updateQuotaByUsername(count, username);
        log.info("用户 {} 配额为: {}", username, count);
        return count;
    }
}
