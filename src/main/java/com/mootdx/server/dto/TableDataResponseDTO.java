package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 表数据响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDataResponseDTO {

    /** 表名 */
    private String tableName;

    /** 列定义 */
    private List<Map<String, Object>> columns;

    /** 数据 */
    private List<Map<String, Object>> data;

    /** 总条数 */
    private Long total;

    /** 当前页 */
    private Integer page;

    /** 每页条数 */
    private Integer pageSize;

    /** 总页数 */
    private Integer totalPages;
}
