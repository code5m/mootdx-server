package com.mootdx.server.repository;

import com.mootdx.server.entity.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 定时任务配置Repository
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, String> {

    /**
     * 根据启用状态查询任务列表
     *
     * @param enabled 是否启用
     * @return 任务列表
     */
    List<ScheduledTask> findByEnabled(Boolean enabled);

    /**
     * 查询所有启用的任务
     *
     * @return 启用的任务列表
     */
    List<ScheduledTask> findByEnabledTrue();

    /**
     * 根据任务名称模糊查询
     *
     * @param taskName 任务名称
     * @return 任务列表
     */
    List<ScheduledTask> findByTaskNameContaining(String taskName);

    /**
     * 根据任务ID和启用状态查询
     *
     * @param taskId  任务ID
     * @param enabled 是否启用
     * @return 任务Optional
     */
    Optional<ScheduledTask> findByTaskIdAndEnabled(String taskId, Boolean enabled);

    /**
     * 更新任务启用状态
     *
     * @param taskId  任务ID
     * @param enabled 启用状态
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE ScheduledTask t SET t.enabled = :enabled, t.updatedAt = CURRENT_TIMESTAMP WHERE t.taskId = :taskId")
    int updateEnabledByTaskId(@Param("taskId") String taskId, @Param("enabled") Boolean enabled);

    /**
     * 更新任务执行统计信息
     *
     * @param taskId           任务ID
     * @param lastExecuteTime  最后执行时间
     * @param lastExecuteResult 最后执行结果
     * @param lastExecuteMessage 最后执行消息
     * @param executeCount     执行次数
     * @param successCount     成功次数
     * @param failCount        失败次数
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE ScheduledTask t SET t.lastExecuteTime = :lastExecuteTime, " +
           "t.lastExecuteResult = :lastExecuteResult, t.lastExecuteMessage = :lastExecuteMessage, " +
           "t.executeCount = :executeCount, t.successCount = :successCount, t.failCount = :failCount, " +
           "t.updatedAt = CURRENT_TIMESTAMP WHERE t.taskId = :taskId")
    int updateExecutionStats(@Param("taskId") String taskId,
                             @Param("lastExecuteTime") LocalDateTime lastExecuteTime,
                             @Param("lastExecuteResult") ScheduledTask.ExecuteResult lastExecuteResult,
                             @Param("lastExecuteMessage") String lastExecuteMessage,
                             @Param("executeCount") Integer executeCount,
                             @Param("successCount") Integer successCount,
                             @Param("failCount") Integer failCount);

    /**
     * 更新任务配置
     *
     * @param taskId         任务ID
     * @param cronExpression Cron表达式
     * @param fixedRate      固定频率
     * @param enabled        启用状态
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE ScheduledTask t SET t.cronExpression = :cronExpression, " +
           "t.fixedRate = :fixedRate, t.enabled = :enabled, t.updatedAt = CURRENT_TIMESTAMP WHERE t.taskId = :taskId")
    int updateTaskConfig(@Param("taskId") String taskId,
                         @Param("cronExpression") String cronExpression,
                         @Param("fixedRate") Long fixedRate,
                         @Param("enabled") Boolean enabled);

    /**
     * 检查任务ID是否存在
     *
     * @param taskId 任务ID
     * @return 是否存在
     */
    boolean existsByTaskId(String taskId);

    /**
     * 根据目标Bean和方法查询任务
     *
     * @param targetBean   目标Bean
     * @param targetMethod 目标方法
     * @return 任务列表
     */
    List<ScheduledTask> findByTargetBeanAndTargetMethod(String targetBean, String targetMethod);
}
