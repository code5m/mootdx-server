package com.mootdx.server.utils;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 股票交易费用计算器
 * 功能：计算买入、卖出费用及融资利息
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Component
public class FeeCalculator {

    /**
     * 买入费用计算结果
     */
    @Data
    @Builder
    public static class BuyFeeResult {
        /**
         * 佣金
         */
        private double commission;
        /**
         * 过户费
         */
        private double transferFee;
        /**
         * 总费用
         */
        private double total;
    }

    /**
     * 卖出费用计算结果
     */
    @Data
    @Builder
    public static class SellFeeResult {
        /**
         * 佣金
         */
        private double commission;
        /**
         * 印花税
         */
        private double stampTax;
        /**
         * 过户费
         */
        private double transferFee;
        /**
         * 总费用
         */
        private double total;
    }

    /**
     * 计算买入费用
     *
     * @param amount           成交金额
     * @param quantity         成交数量
     * @param commissionRate   佣金费率
     * @param minCommission    最低佣金
     * @param transferFeeRate  过户费率
     * @return 费用计算结果
     */
    public BuyFeeResult calculateBuyFees(
            double amount,
            int quantity,
            double commissionRate,
            double minCommission,
            double transferFeeRate) {
        // 佣金 = max(成交金额 * 佣金率, 最低佣金)
        double commission = Math.max(amount * commissionRate, minCommission);

        // 过户费 = 成交金额 * 过户费率 (沪市收取，深市为0)
        double transferFee = amount * transferFeeRate;

        // 总费用
        double total = commission + transferFee;

        return BuyFeeResult.builder()
                .commission(round(commission))
                .transferFee(round(transferFee))
                .total(round(total))
                .build();
    }

    /**
     * 计算买入费用(使用默认费率)
     *
     * @param amount   成交金额
     * @param quantity 成交数量
     * @return 费用计算结果
     */
    public BuyFeeResult calculateBuyFees(double amount, int quantity) {
        return calculateBuyFees(amount, quantity, 0.00025, 5.0, 0.00002);
    }

    /**
     * 计算卖出费用
     *
     * @param amount           成交金额
     * @param quantity         成交数量
     * @param commissionRate   佣金费率
     * @param minCommission    最低佣金
     * @param stampTaxRate     印花税费率
     * @param transferFeeRate  过户费率
     * @return 费用计算结果
     */
    public SellFeeResult calculateSellFees(
            double amount,
            int quantity,
            double commissionRate,
            double minCommission,
            double stampTaxRate,
            double transferFeeRate) {
        // 佣金
        double commission = Math.max(amount * commissionRate, minCommission);

        // 印花税 = 成交金额 * 印花税率 (卖出时收取)
        double stampTax = amount * stampTaxRate;

        // 过户费
        double transferFee = amount * transferFeeRate;

        // 总费用
        double total = commission + stampTax + transferFee;

        return SellFeeResult.builder()
                .commission(round(commission))
                .stampTax(round(stampTax))
                .transferFee(round(transferFee))
                .total(round(total))
                .build();
    }

    /**
     * 计算卖出费用(使用默认费率)
     *
     * @param amount   成交金额
     * @param quantity 成交数量
     * @return 费用计算结果
     */
    public SellFeeResult calculateSellFees(double amount, int quantity) {
        return calculateSellFees(amount, quantity, 0.00025, 5.0, 0.001, 0.00002);
    }

    /**
     * 计算融资利息
     *
     * @param amount     融资金额
     * @param days       融资天数
     * @param annualRate 年利率
     * @return 利息金额
     */
    public double calculateFinancingInterest(double amount, int days, double annualRate) {
        // 利息 = 融资金额 * 年利率 * 天数 / 365
        double interest = amount * annualRate * days / 365;
        return round(interest);
    }

    /**
     * 计算盈亏金额
     *
     * @param currentPrice 当前价格
     * @param quantity     持仓数量
     * @param totalCost    总成本
     * @return 盈亏金额
     */
    public double calculateProfit(double currentPrice, int quantity, double totalCost) {
        double marketValue = currentPrice * quantity;
        return round(marketValue - totalCost);
    }

    /**
     * 计算盈亏率
     *
     * @param profit    盈亏金额
     * @param totalCost 总成本
     * @return 盈亏率(%)
     */
    public double calculateProfitRate(double profit, double totalCost) {
        if (totalCost <= 0) {
            return 0;
        }
        return round(profit / totalCost * 100);
    }

    /**
     * 四舍五入保留2位小数
     *
     * @param value 原始值
     * @return 保留2位小数的值
     */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
