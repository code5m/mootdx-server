package com.mootdx.server.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * TDX服务器配置实体类
 * 功能：存储TDX行情服务器的基本配置信息
 * 对应数据库表：tdx_server_configs
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
@Table(name = "tdx_server_configs")
public class TdxServerConfig {

    /** 主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 服务器名称，如"电信主站1" */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 服务器IP地址 */
    @Column(name = "host", nullable = false, length = 50)
    private String host;

    /** 服务器端口，默认7709 */
    @Column(name = "port", nullable = false)
    private Integer port = 7709;

    /** 服务器类型：STD标准行情/EXT扩展行情 */
    @Enumerated(EnumType.STRING)
    @Column(name = "server_type", length = 10)
    private ServerType serverType = ServerType.STD;

    /** 服务器位置，如"电信"、"联通"、"北京电信" */
    @Column(name = "location", length = 50)
    private String location;

    /** 权重，用于负载均衡，默认100 */
    @Column(name = "weight")
    private Integer weight = 100;

    /** 连接超时时间(秒)，默认10秒 */
    @Column(name = "timeout")
    private Integer timeout = 10;

    /** 最大重试次数，默认3次 */
    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    /** 服务器描述信息 */
    @Column(name = "description", length = 500)
    private String description;

    /** 是否启用，默认true */
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    /** 是否为默认服务器，默认false */
    @Column(name = "is_default")
    private Boolean isDefault = false;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 实体保存前自动设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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
