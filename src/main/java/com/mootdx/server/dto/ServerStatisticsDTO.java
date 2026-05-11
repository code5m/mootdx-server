package com.mootdx.server.dto;

import lombok.Data;

/**
 * 服务器统计数据传输对象
 * 功能：用于返回服务器统计信息
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ServerStatisticsDTO {

    /** 服务器总数 */
    private Long totalServers;

    /** 启用的服务器数量 */
    private Long enabledServers;

    /** 在线节点数量 */
    private Long onlineNodes;

    /** 离线节点数量 */
    private Long offlineNodes;

    /** 不稳定节点数量 */
    private Long unstableNodes;

    /** 维护中节点数量 */
    private Long maintenanceNodes;

    /** 未知状态节点数量 */
    private Long unknownNodes;

    /** 平均响应时间(毫秒) */
    private Double avgResponseTime;

    /** 平均成功率(%) */
    private Double avgSuccessRate;

    /** 默认服务器ID */
    private Long defaultServerId;

    /** 默认服务器名称 */
    private String defaultServerName;
}
