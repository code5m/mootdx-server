package com.mootdx.server.service;

import com.mootdx.server.entity.ScheduledTask;
import com.mootdx.server.entity.TaskExecutionLog;
import com.mootdx.server.repository.ScheduledTaskRepository;
import com.mootdx.server.repository.TaskExecutionLogRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 持久化定时任务调度服务
 * 功能：管理定时任务的持久化配置和执行
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersistentSchedulerService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final ScheduledTaskRepository taskRepository;
    private final TaskExecutionLogRepository executionLogRepository;
    private final ApplicationContext applicationContext;

    // 存储正在运行的任务
    private final Map<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

    /**
     * 任务统计信息
     */
    @Data
    public static class TaskStatistics {
        private String taskId;
        private String taskName;
        private int totalExecutions;
        private int successfulExecutions;
        private int failedExecutions;
        private String lastExecuteTime;
        private String lastExecuteResult;
    }

    /**
     * 初始化：加载并启动所有启用的任务
     */
    @PostConstruct
    public void init() {
        log.info("[PersistentSchedulerService] 初始化定时任务调度器...");
        List<ScheduledTask> enabledTasks = taskRepository.findByEnabledTrue();
        for (ScheduledTask task : enabledTasks) {
            try {
                scheduleTask(task);
                log.info("[PersistentSchedulerService] 已启动任务: {}", task.getTaskId());
            } catch (Exception e) {
                log.error("[PersistentSchedulerService] 启动任务失败: {}", task.getTaskId(), e);
            }
        }
        log.info("[PersistentSchedulerService] 初始化完成，共启动 {} 个任务", enabledTasks.size());
    }

    /**
     * 获取所有任务
     */
    public List<ScheduledTask> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * 根据ID获取任务
     */
    public ScheduledTask getTaskById(String taskId) {
        return taskRepository.findById(taskId).orElse(null);
    }

    /**
     * 创建任务
     */
    public ScheduledTask createTask(ScheduledTask task) {
        if (taskRepository.existsById(task.getTaskId())) {
            throw new IllegalArgumentException("任务ID已存在: " + task.getTaskId());
        }
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        ScheduledTask saved = taskRepository.save(task);
        if (task.getEnabled()) {
            scheduleTask(saved);
        }
        return saved;
    }

    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        ScheduledTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return false;
        }
        // 取消正在运行的任务
        ScheduledFuture<?> future = runningTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
        taskRepository.deleteById(taskId);
        return true;
    }

    /**
     * 启用任务
     */
    public boolean enableTask(String taskId) {
        ScheduledTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return false;
        }
        task.setEnabled(true);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        scheduleTask(task);
        return true;
    }

    /**
     * 禁用任务
     */
    public boolean disableTask(String taskId) {
        ScheduledTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return false;
        }
        task.setEnabled(false);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        // 取消正在运行的任务
        ScheduledFuture<?> future = runningTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
        return true;
    }

    /**
     * 更新任务配置
     */
    public boolean updateTaskConfig(String taskId, String cronExpression, Long fixedRate) {
        ScheduledTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return false;
        }
        if (cronExpression != null) {
            task.setCronExpression(cronExpression);
            task.setTaskType(ScheduledTask.TaskType.CRON);
        }
        if (fixedRate != null) {
            task.setFixedRate(fixedRate);
            task.setTaskType(ScheduledTask.TaskType.FIXED_RATE);
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        // 重新调度
        ScheduledFuture<?> future = runningTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
        if (task.getEnabled()) {
            scheduleTask(task);
        }
        return true;
    }

    /**
     * 立即执行任务
     */
    public boolean executeTaskImmediately(String taskId) {
        ScheduledTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return false;
        }
        executeTask(task);
        return true;
    }

    /**
     * 获取任务日志
     */
    public List<TaskExecutionLog> getTaskLogs(String taskId) {
        return executionLogRepository.findByTaskIdOrderByExecuteTimeDesc(taskId);
    }

    /**
     * 分页获取任务日志
     */
    public Page<TaskExecutionLog> getTaskLogs(String taskId, int page, int size) {
        return executionLogRepository.findByTaskIdOrderByExecuteTimeDesc(
            taskId,
            PageRequest.of(page, size)
        );
    }

    /**
     * 获取最新的日志
     */
    public List<TaskExecutionLog> getLatestLogs(int limit) {
        return executionLogRepository.findLatestLogs(limit);
    }

    /**
     * 清理历史日志
     */
    public long cleanupLogs(int keepDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(keepDays);
        // 这里假设有一个按时间删除的方法，如果没有需要自行实现
        log.info("[cleanupLogs] 清理 {} 天前的日志", keepDays);
        return 0;
    }

    /**
     * 获取任务统计
     */
    public TaskStatistics getTaskStatistics(String taskId) {
        ScheduledTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return null;
        }
        TaskStatistics stats = new TaskStatistics();
        stats.setTaskId(task.getTaskId());
        stats.setTaskName(task.getTaskName());
        // 统计执行次数
        List<TaskExecutionLog> logs = executionLogRepository.findByTaskIdOrderByExecuteTimeDesc(taskId);
        stats.setTotalExecutions(logs.size());
        stats.setSuccessfulExecutions((int) logs.stream()
            .filter(l -> TaskExecutionLog.ExecutionStatus.SUCCESS.equals(l.getStatus())).count());
        stats.setFailedExecutions((int) logs.stream()
            .filter(l -> TaskExecutionLog.ExecutionStatus.FAILED.equals(l.getStatus())).count());
        if (!logs.isEmpty()) {
            TaskExecutionLog latest = logs.get(0);
            stats.setLastExecuteTime(latest.getExecuteTime().toString());
            stats.setLastExecuteResult(latest.getResultMessage());
        }
        return stats;
    }

    /**
     * 调度任务
     */
    private void scheduleTask(ScheduledTask task) {
        // 先取消已存在的任务
        ScheduledFuture<?> existing = runningTasks.remove(task.getTaskId());
        if (existing != null) {
            existing.cancel(false);
        }

        Runnable taskRunnable = () -> executeTask(task);

        ScheduledFuture<?> future;
        switch (task.getTaskType()) {
            case CRON:
                if (task.getCronExpression() != null) {
                    future = taskScheduler.schedule(taskRunnable, new CronTrigger(task.getCronExpression()));
                } else {
                    log.warn("[PersistentSchedulerService] 任务 {} 的Cron表达式为空", task.getTaskId());
                    return;
                }
                break;
            case FIXED_RATE:
                long fixedRate = task.getFixedRate() != null ? task.getFixedRate() : 60000L;
                long initialDelay = task.getInitialDelay() != null ? task.getInitialDelay() : 0L;
                future = taskScheduler.scheduleAtFixedRate(taskRunnable, new Date(System.currentTimeMillis() + initialDelay), fixedRate);
                break;
            case FIXED_DELAY:
                long fixedDelay = task.getFixedRate() != null ? task.getFixedRate() : 60000L;
                long initDelay = task.getInitialDelay() != null ? task.getInitialDelay() : 0L;
                future = taskScheduler.scheduleWithFixedDelay(taskRunnable, new Date(System.currentTimeMillis() + initDelay), fixedDelay);
                break;
            default:
                log.warn("[PersistentSchedulerService] 未知的任务类型: {}", task.getTaskType());
                return;
        }

        runningTasks.put(task.getTaskId(), future);
        log.info("[PersistentSchedulerService] 任务 {} 已调度, 类型: {}", task.getTaskId(), task.getTaskType());
    }

    /**
     * 执行任务
     */
    private void executeTask(ScheduledTask task) {
        log.info("[PersistentSchedulerService] 开始执行任务: {}", task.getTaskId());

        TaskExecutionLog logEntry = new TaskExecutionLog();
        logEntry.setTaskId(task.getTaskId());
        logEntry.setTaskName(task.getTaskName());
        logEntry.setTriggerType(TaskExecutionLog.TriggerType.SCHEDULED);
        logEntry.setExecuteTime(LocalDateTime.now());
        logEntry.setStartTime(LocalDateTime.now());
        logEntry.setStatus(TaskExecutionLog.ExecutionStatus.RUNNING);
        logEntry = executionLogRepository.save(logEntry);

        try {
            // 使用反射调用目标方法
            Object bean = getBeanByName(task.getTargetBean());
            if (bean == null) {
                throw new RuntimeException("找不到目标Bean: " + task.getTargetBean());
            }

            java.lang.reflect.Method method = findMethod(bean, task.getTargetMethod());
            if (method == null) {
                throw new RuntimeException("找不到目标方法: " + task.getTargetMethod());
            }

            method.invoke(bean);

            // 更新执行成功状态
            logEntry.setEndTime(LocalDateTime.now());
            logEntry.setStatus(TaskExecutionLog.ExecutionStatus.SUCCESS);
            logEntry.setResultMessage("执行成功");
            long duration = java.time.Duration.between(logEntry.getStartTime(), logEntry.getEndTime()).toMillis();
            logEntry.setDurationMs(duration);
            executionLogRepository.save(logEntry);

            log.info("[PersistentSchedulerService] 任务 {} 执行完成, 耗时: {}ms", task.getTaskId(), duration);

        } catch (Exception e) {
            // 更新执行失败状态
            logEntry.setEndTime(LocalDateTime.now());
            logEntry.setStatus(TaskExecutionLog.ExecutionStatus.FAILED);
            logEntry.setResultMessage(e.getMessage());

            // 获取异常堆栈
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logEntry.setExceptionInfo(sw.toString());

            long duration = java.time.Duration.between(logEntry.getStartTime(), logEntry.getEndTime()).toMillis();
            logEntry.setDurationMs(duration);
            logEntry.setRetryTimes(logEntry.getRetryTimes() + 1);
            executionLogRepository.save(logEntry);

            log.error("[PersistentSchedulerService] 任务 {} 执行失败", task.getTaskId(), e);
        }
    }

    /**
     * 根据名称获取Bean
     */
    private Object getBeanByName(String beanName) {
        try {
            if (applicationContext == null) {
                log.error("[PersistentSchedulerService] ApplicationContext为空");
                return null;
            }
            return applicationContext.getBean(beanName);
        } catch (Exception e) {
            log.error("[PersistentSchedulerService] 获取Bean失败: {}", beanName, e);
            return null;
        }
    }

    /**
     * 查找方法
     */
    private java.lang.reflect.Method findMethod(Object bean, String methodName) {
        for (java.lang.reflect.Method method : bean.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                return method;
            }
        }
        return null;
    }
}
