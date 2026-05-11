package com.mootdx.server.service;

import com.mootdx.server.scheduler.ServerStatusCheckScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时任务管理服务
 * 功能：管理所有定时任务的启停、配置和状态监控
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Slf4j
@Service
public class SchedulerService {

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ServerStatusCheckScheduler serverStatusCheckScheduler;

    // 定时任务配置存储
    private final Map<String, ScheduledTaskInfo> taskConfigs = new ConcurrentHashMap<>();

    // 正在运行的任务
    private final Map<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

    /**
     * 定时任务信息
     */
    public static class ScheduledTaskInfo {
        private String taskId;
        private String taskName;
        private String description;
        private String cronExpression;
        private long fixedRate; // 毫秒
        private boolean enabled;
        private String lastExecuteTime;
        private String lastExecuteResult;
        private int executeCount;

        public ScheduledTaskInfo(String taskId, String taskName, String description) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.description = description;
            this.enabled = true;
            this.executeCount = 0;
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
        public long getFixedRate() { return fixedRate; }
        public void setFixedRate(long fixedRate) { this.fixedRate = fixedRate; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getLastExecuteTime() { return lastExecuteTime; }
        public void setLastExecuteTime(String lastExecuteTime) { this.lastExecuteTime = lastExecuteTime; }
        public String getLastExecuteResult() { return lastExecuteResult; }
        public void setLastExecuteResult(String lastExecuteResult) { this.lastExecuteResult = lastExecuteResult; }
        public int getExecuteCount() { return executeCount; }
        public void setExecuteCount(int executeCount) { this.executeCount = executeCount; }
    }

    /**
     * 初始化定时任务配置
     */
    @PostConstruct
    public void init() {
        log.info("[SchedulerService] 初始化定时任务管理...");

        // 注册服务器状态检查任务
        ScheduledTaskInfo serverCheckTask = new ScheduledTaskInfo(
                "serverStatusCheck",
                "服务器状态检查",
                "定时检查所有TDX服务器的状态，更新服务器可用性"
        );
        serverCheckTask.setFixedRate(60000); // 默认60秒
        serverCheckTask.setEnabled(true); // 默认启用
        taskConfigs.put(serverCheckTask.getTaskId(), serverCheckTask);

        // 同步状态到实际的调度器
        syncTaskStatusToScheduler(serverCheckTask);

        log.info("[SchedulerService] 已注册 {} 个定时任务", taskConfigs.size());
    }

    /**
     * 同步任务状态到实际调度器
     *
     * @param task 任务信息
     */
    private void syncTaskStatusToScheduler(ScheduledTaskInfo task) {
        if ("serverStatusCheck".equals(task.getTaskId())) {
            try {
                ServerStatusCheckScheduler scheduler = applicationContext.getBean(ServerStatusCheckScheduler.class);
                if (task.isEnabled()) {
                    scheduler.enable();
                    log.info("[SchedulerService] 同步状态: serverStatusCheck 已启用");
                } else {
                    scheduler.disable();
                    log.info("[SchedulerService] 同步状态: serverStatusCheck 已禁用");
                }
            } catch (Exception e) {
                log.error("[SchedulerService] 同步任务状态失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 获取所有定时任务列表
     *
     * @return 任务列表
     */
    public Map<String, ScheduledTaskInfo> getAllTasks() {
        return new ConcurrentHashMap<>(taskConfigs);
    }

    /**
     * 根据ID获取定时任务
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    public ScheduledTaskInfo getTaskById(String taskId) {
        return taskConfigs.get(taskId);
    }

    /**
     * 启用定时任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    public boolean enableTask(String taskId) {
        log.info("[SchedulerService] 启用定时任务: {}", taskId);
        ScheduledTaskInfo task = taskConfigs.get(taskId);
        if (task == null) {
            log.warn("[SchedulerService] 任务不存在: {}", taskId);
            return false;
        }

        task.setEnabled(true);

        // 调用实际的调度器启用任务
        if ("serverStatusCheck".equals(taskId)) {
            ServerStatusCheckScheduler scheduler = applicationContext.getBean(ServerStatusCheckScheduler.class);
            scheduler.enable();
            // 手动触发一次
            try {
                scheduler.checkAllServersStatus();
                log.info("[SchedulerService] 手动触发服务器状态检查完成");
            } catch (Exception e) {
                log.error("[SchedulerService] 手动触发任务失败: {}", e.getMessage(), e);
            }
        }

        return true;
    }

    /**
     * 禁用定时任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    public boolean disableTask(String taskId) {
        log.info("[SchedulerService] 禁用定时任务: {}", taskId);
        ScheduledTaskInfo task = taskConfigs.get(taskId);
        if (task == null) {
            log.warn("[SchedulerService] 任务不存在: {}", taskId);
            return false;
        }

        task.setEnabled(false);

        // 调用实际的调度器禁用任务
        if ("serverStatusCheck".equals(taskId)) {
            ServerStatusCheckScheduler scheduler = applicationContext.getBean(ServerStatusCheckScheduler.class);
            scheduler.disable();
        }

        return true;
    }

    /**
     * 更新任务执行频率
     *
     * @param taskId    任务ID
     * @param fixedRate 固定频率(毫秒)
     * @return 是否成功
     */
    public boolean updateTaskRate(String taskId, long fixedRate) {
        log.info("[SchedulerService] 更新任务频率: taskId={}, fixedRate={}ms", taskId, fixedRate);
        ScheduledTaskInfo task = taskConfigs.get(taskId);
        if (task == null) {
            log.warn("[SchedulerService] 任务不存在: {}", taskId);
            return false;
        }

        if (fixedRate < 1000) {
            log.warn("[SchedulerService] 频率不能小于1000ms");
            return false;
        }

        task.setFixedRate(fixedRate);
        return true;
    }

    /**
     * 立即执行定时任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    public boolean executeTaskImmediately(String taskId) {
        log.info("[SchedulerService] 立即执行任务: {}", taskId);
        ScheduledTaskInfo task = taskConfigs.get(taskId);
        if (task == null) {
            log.warn("[SchedulerService] 任务不存在: {}", taskId);
            return false;
        }

        try {
            task.setLastExecuteTime(java.time.LocalDateTime.now().toString());

            if ("serverStatusCheck".equals(taskId)) {
                serverStatusCheckScheduler.executeImmediately();
                task.setLastExecuteResult("成功");
                task.setExecuteCount(task.getExecuteCount() + 1);
                log.info("[SchedulerService] 任务执行成功: {}", taskId);
            }

            return true;
        } catch (Exception e) {
            task.setLastExecuteResult("失败: " + e.getMessage());
            log.error("[SchedulerService] 任务执行失败: {}", taskId, e);
            return false;
        }
    }

    /**
     * 记录任务执行结果
     *
     * @param taskId 任务ID
     * @param result 执行结果
     */
    public void recordTaskExecution(String taskId, String result) {
        ScheduledTaskInfo task = taskConfigs.get(taskId);
        if (task != null) {
            task.setLastExecuteTime(java.time.LocalDateTime.now().toString());
            task.setLastExecuteResult(result);
            task.setExecuteCount(task.getExecuteCount() + 1);
        }
    }
}
