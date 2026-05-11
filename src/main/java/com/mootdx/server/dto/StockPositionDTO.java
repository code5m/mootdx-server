package com.mootdx.server.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票持仓DTO
 * 功能：持仓记录数据传输对象
 *
 * @author MooTDX
 * @since 1.0.0
 */
public class StockPositionDTO {

    /**
     * 创建持仓请求
     */
    @Data
    public static class CreateRequest {
        @NotNull(message = "平台ID不能为空")
        private Long platformId;

        @NotBlank(message = "股票代码不能为空")
        private String code;

        @NotBlank(message = "市场不能为空")
        private String market;

        private String name;

        @NotNull(message = "买入价格不能为空")
        @Positive(message = "买入价格必须大于0")
        private Double buyPrice;

        @NotNull(message = "买入数量不能为空")
        @Positive(message = "买入数量必须大于0")
        private Integer buyQuantity;

        @NotNull(message = "买入日期不能为空")
        private LocalDate buyDate;

        private LocalDateTime buyTime;

        // 融资相关
        private Boolean isFinancing = false;
        private Double financingAmount = 0.0;
        private Integer financingDays = 0;

        // 交易策略
        private String strategy;
        private Double stopLossPrice;
        private Double targetPrice;

        // 笔记标签
        private String notes;
        private String tags;
    }

    /**
     * 更新持仓请求
     */
    @Data
    public static class UpdateRequest {
        private String name;
        private String strategy;
        private Double stopLossPrice;
        private Double targetPrice;
        private String notes;
        private String tags;
    }

    /**
     * 卖出请求
     */
    @Data
    public static class SellRequest {
        @NotNull(message = "卖出价格不能为空")
        @Positive(message = "卖出价格必须大于0")
        private Double sellPrice;

        private Integer sellQuantity;

        private LocalDate sellDate;
    }

    /**
     * 更新价格请求
     */
    @Data
    public static class UpdatePriceRequest {
        @NotNull(message = "当前价格不能为空")
        @Positive(message = "当前价格必须大于0")
        private Double currentPrice;
    }

    /**
     * 持仓响应
     */
    @Data
    public static class Response {
        private Long id;
        private Long platformId;
        private String platformName;

        // 股票信息
        private String code;
        private String market;
        private String name;

        // 买入信息
        private Double buyPrice;
        private Integer buyQuantity;
        private Double buyAmount;
        private LocalDate buyDate;
        private LocalDateTime buyTime;

        // 当前持仓
        private Integer currentQuantity;
        private Integer soldQuantity;
        private Double avgBuyPrice;

        // 买入费用明细
        private Double buyCommission;
        private Double buyTransferFee;
        private Double financingInterest;
        private Double totalFee;
        private Double totalCost;

        // 卖出信息
        private Double sellPrice;
        private Integer sellQuantity;
        private Double sellAmount;
        private LocalDate sellDate;
        private Double sellCommission;
        private Double sellStampTax;
        private Double sellTransferFee;

        // 盈亏计算
        private Double currentPrice;
        private Double marketValue;
        private Double profitAmount;
        private Double profitRate;

        // 状态
        private String status;

        // 交易策略
        private String strategy;
        private Double stopLossPrice;
        private Double targetPrice;

        // 笔记标签
        private String notes;
        private String tags;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 持仓汇总统计
     */
    @Data
    public static class Summary {
        private Double totalCost;
        private Double totalMarketValue;
        private Double totalProfit;
        private Double totalProfitRate;
        private Long positionCount;
    }
}
