package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * K线数据响应DTO
 * 用于返回K线数据给前端
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlineResponseDTO {

    /** 股票代码 */
    private String code;

    /** 市场代码 */
    private String market;

    /** 频率 */
    private String freq;

    /** K线数据列表 */
    private List<KlineDataDTO> data;
}
