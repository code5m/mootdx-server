package com.mootdx.server.entity;

/**
 * 服务器状态枚举
 * 功能：定义TDX服务器的运行状态
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ServerStatus {
    /** 在线状态 */
    ONLINE("online"),
    /** 离线状态 */
    OFFLINE("offline"),
    /** 不稳定状态 */
    UNSTABLE("unstable"),
    /** 维护中状态 */
    MAINTENANCE("maintenance"),
    /** 未知状态 */
    UNKNOWN("unknown");

    private final String value;

    ServerStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
