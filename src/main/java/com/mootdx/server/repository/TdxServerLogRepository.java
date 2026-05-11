package com.mootdx.server.repository;

import com.mootdx.server.entity.TdxServerLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TDX服务器操作日志数据访问层
 * 功能：提供服务器操作日志的数据库操作方法
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface TdxServerLogRepository extends JpaRepository<TdxServerLog, Long> {

    /**
     * 根据服务器ID查询日志，按时间倒序
     *
     * @param serverId 服务器配置ID
     * @return 日志列表
     */
    List<TdxServerLog> findByServerIdOrderByCreatedAtDesc(Long serverId);

    /**
     * 根据服务器ID查询日志，支持分页
     *
     * @param serverId 服务器配置ID
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    Page<TdxServerLog> findByServerIdOrderByCreatedAtDesc(Long serverId, Pageable pageable);

    /**
     * 根据操作类型查询日志
     *
     * @param action 操作类型
     * @return 日志列表
     */
    List<TdxServerLog> findByActionOrderByCreatedAtDesc(String action);

    /**
     * 根据服务器ID和操作类型查询日志
     *
     * @param serverId 服务器配置ID
     * @param action   操作类型
     * @return 日志列表
     */
    List<TdxServerLog> findByServerIdAndActionOrderByCreatedAtDesc(Long serverId, String action);

    /**
     * 查询最近的N条日志
     *
     * @param limit 限制数量
     * @return 日志列表
     */
    @Query(value = "SELECT * FROM tdx_server_logs ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<TdxServerLog> findRecentLogs(@Param("limit") int limit);

    /**
     * 查询指定时间范围内的日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<TdxServerLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据服务器ID删除日志
     *
     * @param serverId 服务器配置ID
     */
    void deleteByServerId(Long serverId);

    /**
     * 统计指定服务器的日志数量
     *
     * @param serverId 服务器配置ID
     * @return 日志数量
     */
    long countByServerId(Long serverId);
}
