package com.mootdx.server.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 定时任务执行日志实体
 * 记录每次定时任务的执行详情，用于审计和故障排查
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "task_execution_log", indexes = {
    @Index(name = "idx_task_id", columnList = "task_id"),
    @Index(name = "idx_execute_time", columnList = "execute_time"),
    @Index(name = "idx_status", columnList = "status")
})
public class TaskExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 关联的任务ID
     */
    @Column(name = "task_id", length = 64, nullable = false)
    private String taskId;

    /**
     * 任务名称（冗余存储，方便查询）
     */
    @Column(name = "task_name", length = 128)
    private String taskName;

    /**
     * 执行状态：RUNNING - 运行中, SUCCESS - 成功, FAILED - 失败, TIMEOUT - 超时
     */
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    /**
     * 开始执行时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 结束执行时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * 执行结果消息
     */
    @Column(name = "result_message", length = 2048)
    private String resultMessage;

    /**
     * 异常信息（如果执行失败）
     */
    @Column(name = "exception_info", length = 4000)
    private String exceptionInfo;

    /**
     * 任务参数（JSON格式）
     */
    @Column(name = "task_params", length = 2048)
    private String taskParams;

    /**
     * 执行结果数据（JSON格式）
     */
    @Column(name = "result_data", length = 4000)
    private String resultData;

    /**
     * 服务器主机名
     */
    @Column(name = "server_host", length = 128)
    private String serverHost;

    /**
     * 服务器IP地址
     */
    @Column(name = "server_ip", length = 64)
    private String serverIp;

    /**
     * 触发类型：SCHEDULED - 定时触发, MANUAL - 手动触发, RETRY - 重试触发
     */
    @Column(name = "trigger_type", length = 20)
    @Enumerated(EnumType.STRING)
    private TriggerType triggerType = TriggerType.SCHEDULED;

    /**
     * 重试次数
     */
    @Column(name = "retry_times")
    private Integer retryTimes = 0;

    @CreationTimestamp
    @Column(name = "execute_time", nullable = false, updatable = false)
    private LocalDateTime executeTime;

    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT
    }

    /**
     * 触发类型枚举
     */
    public enum TriggerType {
        SCHEDULED,
        MANUAL,
        RETRY
    }
}
