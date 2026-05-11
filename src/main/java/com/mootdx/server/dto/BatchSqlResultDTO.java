package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 单条批量SQL执行结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSqlResultDTO {

    /** 语句索引 */
    private Integer index;

    /** SQL语句 */
    private String sql;

    /** SQL类型 */
    private String sqlType;

    /** 是否成功 */
    private Boolean success;

    /** 执行消息 */
    private String message;

    /** 影响行数 */
    private Integer rowCount;

    /** 查询结果 */
    private List<Map<String, Object>> data;

    /** 列信息 */
    private List<Map<String, String>> columns;

    /** 执行时间(毫秒) */
    private Double executionTime;

    /** 错误信息 */
    private String error;
}
