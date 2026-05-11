package com.mootdx.server.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * 股东信息数据传输对象
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareholderDTO {

    /** 排名 */
    private Integer rank;

    /** 股东名称 */
    private String name;

    /** 股东类型 (个人/机构/基金等) */
    private String type;

    /** 持股数量 */
    private Long shares;

    /** 持股比例 */
    private BigDecimal ratio;

    /** 持股变动 */
    private String change;

    /** 变动数量 */
    private Long changeAmount;

    /**
     * 获取格式化后的持股数量
     */
    public String getSharesFormatted() {
        if (shares == null) return "-";
        if (shares >= 100000000) {
            return String.format("%.2f亿股", shares / 100000000.0);
        } else if (shares >= 10000) {
            return String.format("%.2f万股", shares / 10000.0);
        }
        return shares.toString();
    }

    /**
     * 获取格式化后的持股比例
     */
    public String getRatioFormatted() {
        if (ratio == null) return "-";
        return String.format("%.2f%%", ratio.doubleValue());
    }
}
