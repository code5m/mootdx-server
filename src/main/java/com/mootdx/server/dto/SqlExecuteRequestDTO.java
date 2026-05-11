package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * SQL执行请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlExecuteRequestDTO {

    /** SQL语句 */
    private String sql;

    /** SQL参数 */
    private Map<String, Object> params;
}
