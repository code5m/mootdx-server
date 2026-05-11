package com.mootdx.server.repository;

import com.mootdx.server.entity.TaskExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务执行日志Repository
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Repository
public interface TaskExecutionLogRepository extends JpaRepository<TaskExecutionLog, Long> {

    /**
     * 根据任务ID查询执行日志
     *
     * @param taskId 任务ID
     * @return 日志列表
     */
    List<TaskExecutionLog> findByTaskIdOrderByExecuteTimeDesc(String taskId);

    /**
     * 分页查询任务执行日志
     *
     * @param taskId   任务ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<TaskExecutionLog> findByTaskIdOrderByExecuteTimeDesc(String taskId, Pageable pageable);

    /**
     * 根据执行状态查询日志
     *
     * @param status 执行状态
     * @return 日志列表
     */
    List<TaskExecutionLog> findByStatusOrderByExecuteTimeDesc(TaskExecutionLog.ExecutionStatus status);

    /**
     * 根据任务ID和执行状态查询日志
     *
     * @param taskId 任务ID
     * @param status 执行状态
     * @return 日志列表
     */
    List<TaskExecutionLog> findByTaskIdAndStatusOrderByExecuteTimeDesc(String taskId, TaskExecutionLog.ExecutionStatus status);

    /**
     * 查询时间范围内的日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<TaskExecutionLog> findByExecuteTimeBetweenOrderByExecuteTimeDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询最新的N条日志
     *
     * @param limit 限制条数
     * @return 日志列表
     */
    @Query(value = "SELECT * FROM task_execution_log ORDER BY execute_time DESC LIMIT :limit", nativeQuery = true)
    List<TaskExecutionLog> findLatestLogs(@Param("limit") int limit);

    /**
     * 查询任务的最新一条日志
     *
     * @param taskId 任务ID
     * @return 最新日志
     */
    TaskExecutionLog findFirstByTaskIdOrderByExecuteTimeDesc(String taskId);

    /**
     * 统计任务执行次数
     *
     * @param taskId    任务ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 执行次数
     */
    @Query("SELECT COUNT(l) FROM TaskExecutionLog l WHERE l.taskId = :taskId AND l.executeTime BETWEEN :startTime AND :endTime")
    Long countByTaskIdAndTimeRange(@Param("taskId") String taskId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 统计任务成功次数
     *
     * @param taskId    任务ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 成功次数
     */
    @Query("SELECT COUNT(l) FROM TaskExecutionLog l WHERE l.taskId = :taskId AND l.status = 'SUCCESS' AND l.executeTime BETWEEN :startTime AND :endTime")
    Long countSuccessByTaskIdAndTimeRange(@Param("taskId") String taskId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 统计任务失败次数
     *
     * @param taskId    任务ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 失败次数
     */
    @Query("SELECT COUNT(l) FROM TaskExecutionLog l WHERE l.taskId = :taskId AND l.status = 'FAILED' AND l.executeTime BETWEEN :startTime AND :endTime")
    Long countFailedByTaskIdAndTimeRange(@Param("taskId") String taskId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查询平均执行耗时
     *
     * @param taskId 任务ID
     * @return 平均耗时（毫秒）
     */
    @Query("SELECT AVG(l.durationMs) FROM TaskExecutionLog l WHERE l.taskId = :taskId AND l.status = 'SUCCESS'")
    Double findAverageDurationByTaskId(@Param("taskId") String taskId);

    /**
     * 删除指定时间之前的日志（日志清理）
     *
     * @param beforeTime 时间界限
     * @return 删除记录数
     */
    Long deleteByExecuteTimeBefore(LocalDateTime beforeTime);

    /**
     * 根据任务ID删除日志
     *
     * @param taskId 任务ID
     * @return 删除记录数
     */
    Long deleteByTaskId(String taskId);

    /**
     * 查询正在运行的任务
     *
     * @return 运行中的任务日志列表
     */
    List<TaskExecutionLog> findByStatus(TaskExecutionLog.ExecutionStatus status);

    /**
     * 多条件分页查询日志
     *
     * @param taskId    任务ID
     * @param status    状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 分页结果
     */
    @Query("SELECT l FROM TaskExecutionLog l WHERE " +
           "(:taskId IS NULL OR l.taskId = :taskId) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:startTime IS NULL OR l.executeTime >= :startTime) AND " +
           "(:endTime IS NULL OR l.executeTime <= :endTime) " +
           "ORDER BY l.executeTime DESC")
    Page<TaskExecutionLog> searchLogs(@Param("taskId") String taskId,
                                      @Param("status") TaskExecutionLog.ExecutionStatus status,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      Pageable pageable);
}
