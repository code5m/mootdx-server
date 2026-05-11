package com.mootdx.server.controller;

import com.mootdx.server.common.Result;
import com.mootdx.server.dto.ScheduledTaskDTO;
import com.mootdx.server.entity.ScheduledTask;
import com.mootdx.server.entity.TaskExecutionLog;
import com.mootdx.server.service.PersistentSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理Controller
 * 功能：提供定时任务管理API接口，支持任务配置的持久化管理
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SchedulerController {

    private final PersistentSchedulerService schedulerService;

    /**
     * 获取所有定时任务列表
     * GET /api/scheduler/tasks
     *
     * @return 定时任务列表
     */
    @GetMapping("/tasks")
    public Result<List<ScheduledTask>> getAllTasks() {
        log.info("[getAllTasks] 获取所有定时任务");
        List<ScheduledTask> tasks = schedulerService.getAllTasks();
        return Result.success(tasks);
    }

    /**
     * 根据ID获取定时任务
     * GET /api/scheduler/tasks/{taskId}
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    @GetMapping("/tasks/{taskId}")
    public Result<ScheduledTask> getTaskById(@PathVariable String taskId) {
        log.info("[getTaskById] 获取定时任务: taskId={}", taskId);
        ScheduledTask task = schedulerService.getTaskById(taskId);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(task);
    }

    /**
     * 创建定时任务
     * POST /api/scheduler/tasks
     *
     * @param taskDTO 任务信息
     * @return 创建的任务
     */
    @PostMapping("/tasks")
    public Result<ScheduledTask> createTask(@RequestBody ScheduledTaskDTO taskDTO) {
        log.info("[createTask] 创建定时任务: taskId={}", taskDTO.getTaskId());
        try {
            ScheduledTask task = taskDTO.toEntity();
            ScheduledTask created = schedulerService.createTask(task);
            return Result.success(created);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("[createTask] 创建任务失败: {}", e.getMessage(), e);
            return Result.error(500, "创建任务失败: " + e.getMessage());
        }
    }

    /**
     * 更新定时任务
     * PUT /api/scheduler/tasks/{taskId}
     *
     * @param taskId 任务ID
     * @param task   任务信息
     * @return 更新后的任务
     */
    @PutMapping("/tasks/{taskId}")
    public Result<ScheduledTask> updateTask(@PathVariable String taskId, @RequestBody ScheduledTask task) {
        log.info("[updateTask] 更新定时任务: taskId={}", taskId);
        // TODO: 实现更新逻辑
        return Result.error(501, "暂未实现");
    }

    /**
     * 删除定时任务
     * DELETE /api/scheduler/tasks/{taskId}
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @DeleteMapping("/tasks/{taskId}")
    public Result<Void> deleteTask(@PathVariable String taskId) {
        log.info("[deleteTask] 删除定时任务: taskId={}", taskId);
        boolean success = schedulerService.deleteTask(taskId);
        if (!success) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(null);
    }

    /**
     * 启用定时任务
     * POST /api/scheduler/tasks/{taskId}/enable
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @PostMapping("/tasks/{taskId}/enable")
    public Result<Void> enableTask(@PathVariable String taskId) {
        log.info("[enableTask] 启用定时任务: taskId={}", taskId);
        boolean success = schedulerService.enableTask(taskId);
        if (!success) {
            return Result.error(500, "启用任务失败");
        }
        return Result.success(null);
    }

    /**
     * 禁用定时任务
     * POST /api/scheduler/tasks/{taskId}/disable
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @PostMapping("/tasks/{taskId}/disable")
    public Result<Void> disableTask(@PathVariable String taskId) {
        log.info("[disableTask] 禁用定时任务: taskId={}", taskId);
        boolean success = schedulerService.disableTask(taskId);
        if (!success) {
            return Result.error(500, "禁用任务失败");
        }
        return Result.success(null);
    }

    /**
     * 更新任务执行频率
     * PUT /api/scheduler/tasks/{taskId}/rate
     *
     * @param taskId       任务ID
     * @param cronExpression Cron表达式（可选）
     * @param fixedRate    固定频率(毫秒)（可选）
     * @return 操作结果
     */
    @PutMapping("/tasks/{taskId}/config")
    public Result<Void> updateTaskConfig(
            @PathVariable String taskId,
            @RequestParam(required = false) String cronExpression,
            @RequestParam(required = false) Long fixedRate) {
        log.info("[updateTaskConfig] 更新任务配置: taskId={}, cron={}, fixedRate={}", 
                taskId, cronExpression, fixedRate);
        boolean success = schedulerService.updateTaskConfig(taskId, cronExpression, fixedRate);
        if (!success) {
            return Result.error(500, "更新任务配置失败");
        }
        return Result.success(null);
    }

    /**
     * 立即执行定时任务
     * POST /api/scheduler/tasks/{taskId}/execute
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @PostMapping("/tasks/{taskId}/execute")
    public Result<Void> executeTaskImmediately(@PathVariable String taskId) {
        log.info("[executeTaskImmediately] 立即执行任务: taskId={}", taskId);
        boolean success = schedulerService.executeTaskImmediately(taskId);
        if (!success) {
            return Result.error(500, "执行任务失败");
        }
        return Result.success(null);
    }

    /**
     * 获取任务执行日志
     * GET /api/scheduler/tasks/{taskId}/logs
     *
     * @param taskId 任务ID
     * @return 日志列表
     */
    @GetMapping("/tasks/{taskId}/logs")
    public Result<List<TaskExecutionLog>> getTaskLogs(@PathVariable String taskId) {
        log.info("[getTaskLogs] 获取任务日志: taskId={}", taskId);
        List<TaskExecutionLog> logs = schedulerService.getTaskLogs(taskId);
        return Result.success(logs);
    }

    /**
     * 分页获取任务执行日志
     * GET /api/scheduler/tasks/{taskId}/logs/page
     *
     * @param taskId 任务ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页日志
     */
    @GetMapping("/tasks/{taskId}/logs/page")
    public Result<Page<TaskExecutionLog>> getTaskLogsPage(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[getTaskLogsPage] 获取任务日志分页: taskId={}, page={}, size={}", taskId, page, size);
        Page<TaskExecutionLog> logs = schedulerService.getTaskLogs(taskId, page, size);
        return Result.success(logs);
    }

    /**
     * 获取任务统计信息
     * GET /api/scheduler/tasks/{taskId}/statistics
     *
     * @param taskId 任务ID
     * @return 统计信息
     */
    @GetMapping("/tasks/{taskId}/statistics")
    public Result<PersistentSchedulerService.TaskStatistics> getTaskStatistics(@PathVariable String taskId) {
        log.info("[getTaskStatistics] 获取任务统计: taskId={}", taskId);
        PersistentSchedulerService.TaskStatistics stats = schedulerService.getTaskStatistics(taskId);
        if (stats == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(stats);
    }

    /**
     * 获取最近执行日志
     * GET /api/scheduler/logs/latest
     *
     * @param limit 限制条数
     * @return 日志列表
     */
    @GetMapping("/logs/latest")
    public Result<List<TaskExecutionLog>> getLatestLogs(
            @RequestParam(defaultValue = "50") int limit) {
        log.info("[getLatestLogs] 获取最近日志: limit={}", limit);
        List<TaskExecutionLog> logs = schedulerService.getLatestLogs(limit);
        return Result.success(logs);
    }

    /**
     * 清理历史日志
     * POST /api/scheduler/logs/cleanup
     *
     * @param keepDays 保留天数
     * @return 清理的记录数
     */
    @PostMapping("/logs/cleanup")
    public Result<Map<String, Long>> cleanupLogs(@RequestParam(defaultValue = "30") int keepDays) {
        log.info("[cleanupLogs] 清理日志: keepDays={}", keepDays);
        long deleted = schedulerService.cleanupLogs(keepDays);
        Map<String, Long> result = new java.util.HashMap<>();
        result.put("deletedCount", deleted);
        return Result.success(result);
    }
}
