package com.mootdx.server.repository;

import com.mootdx.server.entity.ServerStatus;
import com.mootdx.server.entity.TdxServerNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TDX服务器节点状态数据访问层
 * 功能：提供服务器节点状态的数据库操作方法
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface TdxServerNodeRepository extends JpaRepository<TdxServerNode, Long> {

    /**
     * 根据服务器ID查询节点状态
     *
     * @param serverId 服务器配置ID
     * @return 节点状态对象
     */
    Optional<TdxServerNode> findByServerId(Long serverId);

    /**
     * 查询所有在线的节点
     *
     * @return 在线节点列表
     */
    List<TdxServerNode> findByStatus(ServerStatus status);

    /**
     * 查询指定状态的节点列表
     *
     * @param status 服务器状态
     * @return 节点列表
     */
    List<TdxServerNode> findAllByStatus(ServerStatus status);

    /**
     * 根据服务器ID删除节点
     *
     * @param serverId 服务器配置ID
     */
    void deleteByServerId(Long serverId);

    /**
     * 查询所有在线节点并按响应时间排序
     *
     * @return 在线节点列表
     */
    @Query("SELECT n FROM TdxServerNode n WHERE n.status = 'ONLINE' ORDER BY n.responseTime ASC")
    List<TdxServerNode> findOnlineNodesOrderByResponseTime();

    /**
     * 查询平均响应时间
     *
     * @return 平均响应时间
     */
    @Query("SELECT AVG(n.responseTime) FROM TdxServerNode n WHERE n.responseTime IS NOT NULL")
    Double findAverageResponseTime();

    /**
     * 查询平均成功率
     *
     * @return 平均成功率
     */
    @Query("SELECT AVG(n.successRate) FROM TdxServerNode n WHERE n.successRate IS NOT NULL")
    Double findAverageSuccessRate();

    /**
     * 统计指定状态的节点数量
     *
     * @param status 服务器状态
     * @return 节点数量
     */
    long countByStatus(ServerStatus status);

    /**
     * 查询指定服务器ID列表中的节点
     *
     * @param serverIds 服务器ID列表
     * @return 节点列表
     */
    @Query("SELECT n FROM TdxServerNode n WHERE n.serverId IN :serverIds")
    List<TdxServerNode> findByServerIdIn(@Param("serverIds") List<Long> serverIds);
}
