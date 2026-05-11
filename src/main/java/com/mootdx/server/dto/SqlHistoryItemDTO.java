package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SQL历史记录项DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlHistoryItemDTO {

    /** 记录ID */
    private String id;

    /** SQL语句 */
    private String sql;

    /** 参数 */
    private Map<String, Object> params;

    /** SQL类型 */
    private String sqlType;

    /** 执行时间 */
    private LocalDateTime executedAt;

    /** 是否成功 */
    private Boolean success;

    /** 影响行数 */
    private Integer rowCount;

    /** 执行时间(毫秒) */
    private Double executionTime;

    /** 执行消息 */
    private String message;
}
