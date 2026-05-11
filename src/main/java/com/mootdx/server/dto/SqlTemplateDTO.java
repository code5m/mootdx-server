package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SQL模板DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlTemplateDTO {

    /** 模板ID */
    private String id;

    /** 模板名称 */
    private String name;

    /** 分类 */
    private String category;

    /** 描述 */
    private String description;

    /** 模板内容 */
    private String template;

    /** 图标 */
    private String icon;
}
