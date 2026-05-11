package com.mootdx.server.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * TDX服务器操作日志实体类
 * 功能：记录服务器配置的变更历史
 * 对应数据库表：tdx_server_logs
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
@Table(name = "tdx_server_logs")
public class TdxServerLog {

    /** 主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的服务器配置ID */
    @Column(name = "server_id", nullable = false)
    private Long serverId;

    /** 操作类型：create/update/delete/set_default/check_status等 */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /** 日志消息 */
    @Column(name = "message", length = 1000)
    private String message;

    /** 旧值，JSON格式存储 */
    @Column(name = "old_value", length = 2000)
    private String oldValue;

    /** 新值，JSON格式存储 */
    @Column(name = "new_value", length = 2000)
    private String newValue;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 实体保存前自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
