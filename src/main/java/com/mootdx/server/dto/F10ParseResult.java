package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * F10数据解析结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class F10ParseResult {
    
    /**
     * 是否解析成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 股票元数据
     */
    private StockMetadata metadata;
    
    /**
     * 财务指标列表
     */
    private List<FinancialIndicator> financialIndicators;
    
    /**
     * 持股变化列表
     */
    private List<HoldingChange> holdingChanges;
    
    /**
     * EPS历史列表
     */
    private List<EpsHistory> epsHistory;
    
    /**
     * 风险提示列表
     */
    private List<RiskAlert> riskAlerts;

    /**
     * 公司基本资料
     */
    private CompanyProfile companyProfile;

    /**
     * 原始表格数量
     */
    private Integer rawTableCount;

    /**
     * 解析警告信息
     */
    private List<String> parseWarnings;
    
    /**
     * 股票元数据DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMetadata {
        private String stockCode;
        private String stockName;
        private LocalDate updateDate;
        private String dataSource;
        private String rawTitle;
        private Map<String, String> metadata;
    }
    
    /**
     * 财务指标DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialIndicator {
        private String indicatorName;
        private BigDecimal value;
        private String unit;
        private LocalDate reportDate;
        private Boolean isNegative;
        private Map<String, String> metadata;
    }
    
    /**
     * 持股变化DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingChange {
        private LocalDate reportDate;
        private Integer institutionCount;
        private BigDecimal totalHeld;
        private BigDecimal holdingRatio;
        private BigDecimal changeFromPrevious;
        private BigDecimal fundHolding;
        private Map<String, String> metadata;
    }
    
    /**
     * EPS历史DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EpsHistory {
        private String year;
        private BigDecimal annualEps;
        private BigDecimal q3Eps;
        private BigDecimal halfYearEps;
        private BigDecimal q1Eps;
        private Map<String, String> metadata;
    }
    
    /**
     * 风险提示DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAlert {
        private String riskType;
        private LocalDate alertDate;
        private String title;
        private String content;
        private Double markValue;
        private String riskLevel;
        private Map<String, String> metadata;
    }

    /**
     * 公司基本资料DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyProfile {
        private String companyName;
        private String englishName;
        private String stockName;
        private String stockCode;
        private String formerName;
        private String relatedIndex;
        private String industryCategory;
        private String securityType;
        private LocalDate listingDate;
        private LocalDate establishmentDate;
        private String registeredCapital;
        private String creditCode;
        private String legalRepresentative;
        private String president;
        private String secretary;
        private String securitiesRep;
        private String phone;
        private String fax;
        private String website;
        private String email;
        private String registeredAddress;
        private String officeAddress;
        private String accountingFirm;
        private String companyIntroduction;
        private String businessScope;
        private Map<String, String> additionalInfo;
    }
}
