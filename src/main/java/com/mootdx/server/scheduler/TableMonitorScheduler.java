package com.mootdx.server.scheduler;

import com.mootdx.server.service.DbManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据库表监控定时任务
 * 定期检测新增表
 */
@Slf4j
@Component("tableMonitorScheduler")
public class TableMonitorScheduler {

    @Autowired
    private DbManagerService dbManagerService;

    // 任务是否启用
    private volatile boolean enabled = true;

    /**
     * 监控数据表
     * 由PersistentSchedulerService调用
     */
    public void monitorTables() {
        if (!enabled) {
            log.debug("[TableMonitorScheduler] 任务已禁用，跳过执行");
            return;
        }

        try {
            List<String> newTables = dbManagerService.getNewTables();
            if (!newTables.isEmpty()) {
                log.info("[表监控] 发现 {} 个新表: {}", newTables.size(), newTables);
            }
        } catch (Exception e) {
            log.error("[表监控] 检测新表失败", e);
        }
    }

    /**
     * 启用任务
     */
    public void enable() {
        this.enabled = true;
        log.info("[TableMonitorScheduler] 任务已启用");
    }

    /**
     * 禁用任务
     */
    public void disable() {
        this.enabled = false;
        log.info("[TableMonitorScheduler] 任务已禁用");
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
     * 立即执行（绕过enabled检查）
     */
    public void executeImmediately() {
        log.info("[TableMonitorScheduler] 手动触发表监控");
        try {
            List<String> newTables = dbManagerService.getNewTables();
            if (!newTables.isEmpty()) {
                log.info("[表监控] 发现 {} 个新表: {}", newTables.size(), newTables);
            }
            log.info("[TableMonitorScheduler] 手动触发表监控完成");
        } catch (Exception e) {
            log.error("[TableMonitorScheduler] 手动触发表监控失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
