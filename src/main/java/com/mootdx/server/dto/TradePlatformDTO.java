package com.mootdx.server.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 交易平台DTO
 * 功能：交易平台数据传输对象
 *
 * @author MooTDX
 * @since 1.0.0
 */
public class TradePlatformDTO {

    /**
     * 创建平台请求
     */
    @Data
    public static class CreateRequest {
        @NotBlank(message = "平台名称不能为空")
        private String name;

        private String accountNo;
        private String accountType = "stock";
        private Double buyCommissionRate = 0.00025;
        private Double buyMinCommission = 5.0;
        private Double sellCommissionRate = 0.00025;
        private Double sellMinCommission = 5.0;
        private Double stampTaxRate = 0.001;
        private Double transferFeeRate = 0.00002;
        private Double financingRate = 0.08;
        private Double shortSellingRate = 0.10;
        private Boolean isActive = true;
        private Boolean isDefault = false;
        private Integer sortOrder = 0;
        private String remark;
    }

    /**
     * 更新平台请求
     */
    @Data
    public static class UpdateRequest {
        private String name;
        private String accountNo;
        private String accountType;
        private Double buyCommissionRate;
        private Double buyMinCommission;
        private Double sellCommissionRate;
        private Double sellMinCommission;
        private Double stampTaxRate;
        private Double transferFeeRate;
        private Double financingRate;
        private Double shortSellingRate;
        private Boolean isActive;
        private Boolean isDefault;
        private Integer sortOrder;
        private String remark;
    }

    /**
     * 平台响应
     */
    @Data
    public static class Response {
        private Long id;
        private String name;
        private String accountNo;
        private String accountType;
        private Double buyCommissionRate;
        private Double buyMinCommission;
        private Double sellCommissionRate;
        private Double sellMinCommission;
        private Double stampTaxRate;
        private Double transferFeeRate;
        private Double financingRate;
        private Double shortSellingRate;
        private Boolean isActive;
        private Boolean isDefault;
        private Integer sortOrder;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
