package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfoDTO {

    /** 字段名 */
    private String name;

    /** 数据类型 */
    private String type;

    /** 是否可为空 */
    private Boolean nullable;

    /** 默认值 */
    private String defaultValue;

    /** 字段注释 */
    private String comment;

    /** 是否主键 */
    private Boolean isPrimaryKey;
}
