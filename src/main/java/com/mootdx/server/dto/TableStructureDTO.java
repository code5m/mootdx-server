package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表结构DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStructureDTO {

    /** 表名 */
    private String name;

    /** 表注释 */
    private String comment;

    /** 列信息 */
    private List<ColumnInfoDTO> columns;

    /** 索引信息 */
    private List<IndexInfoDTO> indexes;

    /** 主键列 */
    private List<String> primaryKeys;
}
