package com.mootdx.server.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 服务器类型枚举
 * 功能：定义TDX服务器的类型
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ServerType {
    /** 标准行情服务器 */
    STD("std"),
    /** 扩展行情服务器 */
    EXT("ext");

    private final String value;

    ServerType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ServerType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ServerType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ServerType: " + value);
    }
}
