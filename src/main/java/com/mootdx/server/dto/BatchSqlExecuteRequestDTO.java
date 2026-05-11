package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量SQL执行请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSqlExecuteRequestDTO {

    /** SQL语句列表 */
    private List<String> statements;

    /** 执行模式: single(逐条)|batch(批量)|transaction(事务) */
    private String mode;
}
