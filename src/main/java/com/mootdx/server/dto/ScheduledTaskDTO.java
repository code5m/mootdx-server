package com.mootdx.server.dto;

import com.mootdx.server.entity.ScheduledTask;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 定时任务创建/更新DTO
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
public class ScheduledTaskDTO {

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    private String description;

    @NotNull(message = "任务类型不能为空")
    private ScheduledTask.TaskType taskType;

    private String cronExpression;

    private Long fixedRate;

    private Long initialDelay;

    private Boolean enabled = true;

    @NotBlank(message = "目标Bean不能为空")
    private String targetBean;

    @NotBlank(message = "目标方法不能为空")
    private String targetMethod;

    private String taskParams;

    private ScheduledTask.ConcurrentPolicy concurrentPolicy;

    private Long timeout;

    private Integer retryCount;

    private Long retryInterval;

    /**
     * 转换为实体
     */
    public ScheduledTask toEntity() {
        ScheduledTask task = new ScheduledTask();
        task.setTaskId(this.taskId);
        task.setTaskName(this.taskName);
        task.setDescription(this.description);
        task.setTaskType(this.taskType);
        task.setCronExpression(this.cronExpression);
        task.setFixedRate(this.fixedRate);
        task.setInitialDelay(this.initialDelay);
        task.setEnabled(this.enabled);
        task.setTargetBean(this.targetBean);
        task.setTargetMethod(this.targetMethod);
        task.setTaskParams(this.taskParams);
        task.setConcurrentPolicy(this.concurrentPolicy);
        task.setTimeout(this.timeout);
        task.setRetryCount(this.retryCount);
        task.setRetryInterval(this.retryInterval);
        return task;
    }
}
