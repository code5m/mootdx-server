package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量SQL执行响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSqlExecuteResponseDTO {

    /** 执行结果列表 */
    private List<BatchSqlResultDTO> results;

    /** 总语句数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 总执行时间(毫秒) */
    private Double totalExecutionTime;

    /** 执行模式 */
    private String mode;

    /** 是否有新表发现 */
    private Boolean hasNewTables;

    /** 新表列表 */
    private List<String> newTables;
}
