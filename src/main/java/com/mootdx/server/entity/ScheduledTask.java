package com.mootdx.server.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 定时任务配置实体
 * 存储定时任务的基本配置信息，支持持久化到数据库
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "scheduled_task")
public class ScheduledTask {

    @Id
    @Column(name = "task_id", length = 64, nullable = false)
    private String taskId;

    @Column(name = "task_name", length = 128, nullable = false)
    private String taskName;

    @Column(name = "description", length = 512)
    private String description;

    /**
     * 任务类型：CRON - Cron表达式, FIXED_RATE - 固定频率, FIXED_DELAY - 固定延迟
     */
    @Column(name = "task_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskType taskType = TaskType.FIXED_RATE;

    /**
     * Cron表达式（当taskType=CRON时使用）
     */
    @Column(name = "cron_expression", length = 128)
    private String cronExpression;

    /**
     * 固定频率/延迟的毫秒数
     */
    @Column(name = "fixed_rate")
    private Long fixedRate;

    /**
     * 初始延迟（毫秒）
     */
    @Column(name = "initial_delay")
    private Long initialDelay = 0L;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 任务执行的目标类名
     */
    @Column(name = "target_bean", length = 128, nullable = false)
    private String targetBean;

    /**
     * 任务执行的方法名
     */
    @Column(name = "target_method", length = 128, nullable = false)
    private String targetMethod;

    /**
     * 任务参数（JSON格式）
     */
    @Column(name = "task_params", length = 2048)
    private String taskParams;

    /**
     * 最后一次执行时间
     */
    @Column(name = "last_execute_time")
    private LocalDateTime lastExecuteTime;

    /**
     * 最后一次执行结果：SUCCESS - 成功, FAILED - 失败
     */
    @Column(name = "last_execute_result", length = 20)
    @Enumerated(EnumType.STRING)
    private ExecuteResult lastExecuteResult;

    /**
     * 最后一次执行结果消息
     */
    @Column(name = "last_execute_message", length = 1024)
    private String lastExecuteMessage;

    /**
     * 执行次数统计
     */
    @Column(name = "execute_count", nullable = false)
    private Integer executeCount = 0;

    /**
     * 成功次数统计
     */
    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0;

    /**
     * 失败次数统计
     */
    @Column(name = "fail_count", nullable = false)
    private Integer failCount = 0;

    /**
     * 并发执行策略：
     * - CONCURRENT: 允许并发执行（默认）
     * - DISCARD: 如果有正在执行的任务，丢弃新的执行
     * - BLOCK: 如果有正在执行的任务，阻塞等待
     * - REPLACE: 如果有正在执行的任务，终止旧任务并执行新任务
     */
    @Column(name = "concurrent_policy", length = 20)
    @Enumerated(EnumType.STRING)
    private ConcurrentPolicy concurrentPolicy = ConcurrentPolicy.CONCURRENT;

    /**
     * 超时时间（毫秒），0表示不超时
     */
    @Column(name = "timeout")
    private Long timeout = 0L;

    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * 重试间隔（毫秒）
     */
    @Column(name = "retry_interval")
    private Long retryInterval = 5000L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 任务类型枚举
     */
    public enum TaskType {
        CRON,
        FIXED_RATE,
        FIXED_DELAY
    }

    /**
     * 执行结果枚举
     */
    public enum ExecuteResult {
        SUCCESS,
        FAILED
    }

    /**
     * 并发策略枚举
     */
    public enum ConcurrentPolicy {
        CONCURRENT,
        DISCARD,
        BLOCK,
        REPLACE
    }
}
