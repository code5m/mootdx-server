package com.mootdx.server.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * F10公司资料数据传输对象
 * 提供结构化的公司信息数据，支持前端可视化展示
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyInfoDTO {

    // ==================== 基本信息 ====================
    /** 股票代码 */
    private String code;

    /** 股票名称 */
    private String name;

    /** 市场代码 (0=深圳, 1=上海) */
    private Integer market;

    /** 公司全称 */
    private String fullName;

    /** 英文名称 */
    private String englishName;

    /** 所属行业 */
    private String industry;

    /** 公司网址 */
    private String website;

    /** 上市日期 */
    private LocalDate listingDate;

    // ==================== 股本信息 ====================
    /** 总股本 */
    private Long totalShares;

    /** 流通股本 */
    private Long circulatingShares;

    /** 限售股本 */
    private Long restrictedShares;

    /** 总股本(格式化) */
    private String totalSharesFormatted;

    /** 流通股本(格式化) */
    private String circulatingSharesFormatted;

    // ==================== 财务指标 ====================
    /** 每股收益 */
    private BigDecimal eps;

    /** 每股净资产 */
    private BigDecimal navps;

    /** 净资产收益率 */
    private BigDecimal roe;

    /** 每股营业收入 */
    private BigDecimal revenuePerShare;

    /** 每股经营现金流 */
    private BigDecimal operatingCashFlowPerShare;

    /** 毛利率 */
    private BigDecimal grossProfitMargin;

    /** 净利率 */
    private BigDecimal netProfitMargin;

    /** 资产负债率 */
    private BigDecimal debtToAssetRatio;

    /** 市盈率 */
    private BigDecimal peRatio;

    /** 市净率 */
    private BigDecimal pbRatio;

    // ==================== 联系信息 ====================
    /** 注册地址 */
    private String registeredAddress;

    /** 办公地址 */
    private String officeAddress;

    /** 联系电话 */
    private String phone;

    /** 传真 */
    private String fax;

    /** 电子邮箱 */
    private String email;

    /** 邮政编码 */
    private String postalCode;

    // ==================== 股东信息 ====================
    /** 股东总数 */
    private Integer totalShareholders;

    /** 机构股东数 */
    private Integer institutionalShareholders;

    /** 十大流通股东 */
    private List<ShareholderDTO> top10Shareholders;

    // ==================== 分红信息 ====================
    /** 每股分红 */
    private BigDecimal dividendPerShare;

    /** 股息率 */
    private BigDecimal dividendYield;

    /** 最近分红日期 */
    private LocalDate lastDividendDate;

    // ==================== 公司概况 ====================
    /** 公司简介 */
    private String companyProfile;

    /** 经营范围 */
    private String businessScope;

    /** 主营业务 */
    private String mainBusiness;

    // ==================== 分类数据 ====================
    /** 原始分类数据映射 (分类名 -> 内容) */
    private Map<String, String> categoryData;

    /** 分类列表 */
    private List<String> categories;

    // ==================== 辅助方法 ====================

    /**
     * 获取市场名称
     */
    public String getMarketName() {
        if (market == null) return "-";
        return market == 1 ? "上海" : "深圳";
    }

    /**
     * 格式化金额 (亿/万)
     */
    public static String formatAmount(Long amount) {
        if (amount == null) return "-";
        if (amount >= 100000000) {
            return String.format("%.2f亿", amount / 100000000.0);
        } else if (amount >= 10000) {
            return String.format("%.2f万", amount / 10000.0);
        }
        return amount.toString();
    }

    /**
     * 格式化百分比
     */
    public static String formatPercent(BigDecimal value) {
        if (value == null) return "-";
        return String.format("%.2f%%", value.doubleValue());
    }

    /**
     * 格式化数值
     */
    public static String formatDecimal(BigDecimal value) {
        if (value == null) return "-";
        return String.format("%.2f", value.doubleValue());
    }
}
