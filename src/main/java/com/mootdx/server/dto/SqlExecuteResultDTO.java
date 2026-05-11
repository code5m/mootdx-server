package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SQL执行结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlExecuteResultDTO {

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

    /** SQL类型 */
    private String sqlType;
}
