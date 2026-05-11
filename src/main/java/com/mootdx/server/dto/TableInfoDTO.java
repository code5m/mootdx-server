package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfoDTO {

    /** 表名 */
    private String name;

    /** 表注释 */
    private String comment;

    /** 行数 */
    private Long rowCount;

    /** 列数 */
    private Integer columnsCount;

    /** 业务分类 */
    private TableCategoryDTO category;

    /** 是否为新表（本次刷新新增的） */
    private Boolean isNew;
}
