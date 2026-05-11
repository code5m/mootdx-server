package com.mootdx.server.dto;

import com.mootdx.server.entity.ServerStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务器状态数据传输对象
 * 功能：用于返回服务器及其节点状态信息
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ServerStatusDTO {

    /** 服务器配置ID */
    private Long id;

    /** 服务器名称 */
    private String name;

    /** 服务器IP地址 */
    private String host;

    /** 服务器端口 */
    private Integer port;

    /** 服务器类型 */
    private String serverType;

    /** 服务器位置 */
    private String location;

    /** 是否启用 */
    private Boolean isEnabled;

    /** 是否为默认服务器 */
    private Boolean isDefault;

    /** 节点状态 */
    private ServerStatus status;

    /** 响应时间(毫秒) */
    private Double responseTime;

    /** 成功率(0-100) */
    private Double successRate;

    /** 最后检查时间 */
    private LocalDateTime lastCheck;

    /** 连续失败次数 */
    private Integer failCount;

    /** 总请求数 */
    private Integer totalRequests;

    /** 成功请求数 */
    private Integer successRequests;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
