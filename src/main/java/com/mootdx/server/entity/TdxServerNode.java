package com.mootdx.server.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * TDX服务器节点状态实体类
 * 功能：存储TDX服务器的实时运行状态和监控数据
 * 对应数据库表：tdx_server_nodes
 * 与TdxServerConfig是一对一关系
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tdx_server_nodes")
public class TdxServerNode {

    /** 主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的服务器配置ID，唯一约束确保一对一关系 */
    @Column(name = "server_id", nullable = false, unique = true)
    private Long serverId;

    /** 节点状态：ONLINE/OFFLINE/UNSTABLE/MAINTENANCE/UNKNOWN */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ServerStatus status = ServerStatus.UNKNOWN;

    /** 响应时间(毫秒) */
    @Column(name = "response_time")
    private Double responseTime;

    /** 成功率(0-1) */
    @Column(name = "success_rate")
    private Double successRate = 0.0;

    /** 最后检查时间 */
    @Column(name = "last_check")
    private LocalDateTime lastCheck;

    /** 连续失败次数 */
    @Column(name = "fail_count")
    private Integer failCount = 0;

    /** 总请求数 */
    @Column(name = "total_requests")
    private Integer totalRequests = 0;

    /** 成功请求数 */
    @Column(name = "success_requests")
    private Integer successRequests = 0;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 实体保存前自动设置更新时间
     */
    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 实体更新前自动设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
