package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表分类DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableCategoryDTO {

    /** 分类名称 */
    private String name;

    /** 分类描述 */
    private String description;
}
