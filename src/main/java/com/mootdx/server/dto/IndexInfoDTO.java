package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 索引信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexInfoDTO {

    /** 索引名 */
    private String name;

    /** 索引列 */
    private List<String> columns;

    /** 是否唯一索引 */
    private Boolean unique;
}
