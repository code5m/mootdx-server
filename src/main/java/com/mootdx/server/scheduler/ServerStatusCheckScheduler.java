package com.mootdx.server.scheduler;

import com.mootdx.server.service.TdxServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 服务器状态检查定时任务
 * 功能：定时检查所有TDX服务器的状态
 * 注：由PersistentSchedulerService调用，不再使用@Scheduled注解
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component("serverStatusCheckScheduler")
@RequiredArgsConstructor
public class ServerStatusCheckScheduler {

    private final TdxServerService serverService;

    // 任务是否启用
    private volatile boolean enabled = true;

    /**
     * 检查所有服务器状态
     * 由PersistentSchedulerService调用
     */
    public void checkAllServersStatus() {
        // 检查任务是否启用
        if (!enabled) {
            log.debug("[ServerStatusCheckScheduler] 任务已禁用，跳过执行");
            return;
        }

        log.info("[ServerStatusCheckScheduler] 开始检查所有服务器状态");
        try {
            serverService.checkAllServers();
            log.info("[ServerStatusCheckScheduler] 服务器状态检查完成");
        } catch (Exception e) {
            log.error("[ServerStatusCheckScheduler] 服务器状态检查失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 启用任务
     */
    public void enable() {
        this.enabled = true;
        log.info("[ServerStatusCheckScheduler] 任务已启用");
    }

    /**
     * 禁用任务
     */
    public void disable() {
        this.enabled = false;
        log.info("[ServerStatusCheckScheduler] 任务已禁用");
    }

    /**
     * 获取任务状态
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * 立即执行服务器状态检查（绕过enabled检查）
     * 用于手动触发任务执行
     */
    public void executeImmediately() {
        log.info("[定时任务] 手动触发服务器状态检查");
        try {
            serverService.checkAllServers();
            log.info("[定时任务] 手动触发服务器状态检查完成");
        } catch (Exception e) {
            log.error("[定时任务] 手动触发服务器状态检查失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
