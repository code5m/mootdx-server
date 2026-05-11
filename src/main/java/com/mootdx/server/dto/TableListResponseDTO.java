package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表列表响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableListResponseDTO {

    /** 表列表 */
    private List<TableInfoDTO> tables;

    /** 总数 */
    private Integer total;

    /** 分类列表 */
    private List<TableCategoryDTO> categories;

    /** 是否有新增表 */
    private Boolean hasNewTables;

    /** 新增表数量 */
    private Integer newTableCount;
}
