package com.mootdx.server.service;

import com.mootdx.server.dto.StockPositionDTO;
import com.mootdx.server.dto.TradePlatformDTO;
import com.mootdx.server.entity.StockPosition;
import com.mootdx.server.entity.TradePlatform;
import com.mootdx.server.repository.StockPositionRepository;
import com.mootdx.server.repository.TradePlatformRepository;
import com.mootdx.server.utils.FeeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 持仓笔记服务
 * 功能：提供持仓笔记业务逻辑处理
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockNoteService {

    private final TradePlatformRepository platformRepository;
    private final StockPositionRepository positionRepository;
    private final FeeCalculator feeCalculator;

    // ==================== 平台管理 ====================

    /**
     * 创建交易平台
     *
     * @param request 创建请求
     * @return 平台响应
     */
    @Transactional
    public TradePlatformDTO.Response createPlatform(TradePlatformDTO.CreateRequest request) {
        log.info("[createPlatform] 创建交易平台: name={}", request.getName());

        TradePlatform platform = TradePlatform.builder()
                .name(request.getName())
                .accountNo(request.getAccountNo())
                .accountType(request.getAccountType())
                .buyCommissionRate(request.getBuyCommissionRate())
                .buyMinCommission(request.getBuyMinCommission())
                .sellCommissionRate(request.getSellCommissionRate())
                .sellMinCommission(request.getSellMinCommission())
                .stampTaxRate(request.getStampTaxRate())
                .transferFeeRate(request.getTransferFeeRate())
                .financingRate(request.getFinancingRate())
                .shortSellingRate(request.getShortSellingRate())
                .isActive(request.getIsActive())
                .isDefault(request.getIsDefault())
                .sortOrder(request.getSortOrder())
                .remark(request.getRemark())
                .build();

        platform = platformRepository.save(platform);
        return convertToPlatformResponse(platform);
    }

    /**
     * 获取所有平台列表
     *
     * @param activeOnly 是否只返回启用的平台
     * @return 平台列表
     */
    public List<TradePlatformDTO.Response> getPlatforms(boolean activeOnly) {
        log.info("[getPlatforms] 获取平台列表: activeOnly={}", activeOnly);

        List<TradePlatform> platforms;
        if (activeOnly) {
            platforms = platformRepository.findByIsActiveTrueOrderBySortOrderAsc();
        } else {
            platforms = platformRepository.findAllByOrderBySortOrderAsc();
        }

        return platforms.stream()
                .map(this::convertToPlatformResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取平台
     *
     * @param id 平台ID
     * @return 平台信息
     */
    public Optional<TradePlatformDTO.Response> getPlatformById(Long id) {
        log.info("[getPlatformById] 获取平台: id={}", id);
        return platformRepository.findById(id)
                .map(this::convertToPlatformResponse);
    }

    /**
     * 更新平台信息
     *
     * @param id      平台ID
     * @param request 更新请求
     * @return 更新后的平台信息
     */
    @Transactional
    public Optional<TradePlatformDTO.Response> updatePlatform(Long id, TradePlatformDTO.UpdateRequest request) {
        log.info("[updatePlatform] 更新平台: id={}", id);

        return platformRepository.findById(id)
                .map(platform -> {
                    if (request.getName() != null) platform.setName(request.getName());
                    if (request.getAccountNo() != null) platform.setAccountNo(request.getAccountNo());
                    if (request.getAccountType() != null) platform.setAccountType(request.getAccountType());
                    if (request.getBuyCommissionRate() != null)
                        platform.setBuyCommissionRate(request.getBuyCommissionRate());
                    if (request.getBuyMinCommission() != null)
                        platform.setBuyMinCommission(request.getBuyMinCommission());
                    if (request.getSellCommissionRate() != null)
                        platform.setSellCommissionRate(request.getSellCommissionRate());
                    if (request.getSellMinCommission() != null)
                        platform.setSellMinCommission(request.getSellMinCommission());
                    if (request.getStampTaxRate() != null) platform.setStampTaxRate(request.getStampTaxRate());
                    if (request.getTransferFeeRate() != null)
                        platform.setTransferFeeRate(request.getTransferFeeRate());
                    if (request.getFinancingRate() != null) platform.setFinancingRate(request.getFinancingRate());
                    if (request.getShortSellingRate() != null)
                        platform.setShortSellingRate(request.getShortSellingRate());
                    if (request.getIsActive() != null) platform.setIsActive(request.getIsActive());
                    if (request.getIsDefault() != null) platform.setIsDefault(request.getIsDefault());
                    if (request.getSortOrder() != null) platform.setSortOrder(request.getSortOrder());
                    if (request.getRemark() != null) platform.setRemark(request.getRemark());

                    platform = platformRepository.save(platform);
                    return convertToPlatformResponse(platform);
                });
    }

    /**
     * 删除平台
     *
     * @param id 平台ID
     */
    @Transactional
    public void deletePlatform(Long id) {
        log.info("[deletePlatform] 删除平台: id={}", id);
        platformRepository.deleteById(id);
    }

    // ==================== 持仓管理 ====================

    /**
     * 创建持仓记录
     *
     * @param request 创建请求
     * @return 持仓响应
     */
    @Transactional
    public StockPositionDTO.Response createPosition(StockPositionDTO.CreateRequest request) {
        log.info("[createPosition] 创建持仓: code={}, quantity={}", request.getCode(), request.getBuyQuantity());

        // 获取平台信息用于计算费用
        TradePlatform platform = platformRepository.findById(request.getPlatformId())
                .orElseThrow(() -> new IllegalArgumentException("平台不存在"));

        // 计算买入金额
        double buyPrice = request.getBuyPrice();
        int quantity = request.getBuyQuantity();
        double buyAmount = buyPrice * quantity;

        // 计算买入费用
        FeeCalculator.BuyFeeResult buyFees = feeCalculator.calculateBuyFees(
                buyAmount, quantity,
                platform.getBuyCommissionRate(),
                platform.getBuyMinCommission(),
                platform.getTransferFeeRate()
        );

        // 计算融资利息
        double financingInterest = 0.0;
        if (request.getIsFinancing() != null && request.getIsFinancing()
                && request.getFinancingAmount() != null && request.getFinancingAmount() > 0) {
            financingInterest = feeCalculator.calculateFinancingInterest(
                    request.getFinancingAmount(),
                    request.getFinancingDays() != null ? request.getFinancingDays() : 0,
                    platform.getFinancingRate()
            );
        }

        // 计算总费用和总成本
        double totalFee = buyFees.getTotal() + financingInterest;
        double totalCost = buyAmount + totalFee;

        // 构建持仓实体
        StockPosition position = StockPosition.builder()
                .platformId(request.getPlatformId())
                .code(request.getCode())
                .market(request.getMarket())
                .name(request.getName())
                .buyPrice(buyPrice)
                .buyQuantity(quantity)
                .buyAmount(buyAmount)
                .buyDate(request.getBuyDate())
                .buyTime(request.getBuyTime())
                .currentQuantity(quantity)
                .soldQuantity(0)
                .avgBuyPrice(buyPrice)
                .buyCommission(buyFees.getCommission())
                .buyTransferFee(buyFees.getTransferFee())
                .financingInterest(financingInterest)
                .totalFee(totalFee)
                .totalCost(totalCost)
                .status("holding")
                .strategy(request.getStrategy())
                .stopLossPrice(request.getStopLossPrice())
                .targetPrice(request.getTargetPrice())
                .notes(request.getNotes())
                .tags(request.getTags())
                .build();

        position = positionRepository.save(position);
        return convertToPositionResponse(position);
    }

    /**
     * 获取持仓列表
     *
     * @param platformId 平台ID(可选)
     * @param status     状态(可选)
     * @param code       股票代码(可选)
     * @return 持仓列表
     */
    public List<StockPositionDTO.Response> getPositions(Long platformId, String status, String code) {
        log.info("[getPositions] 获取持仓列表: platformId={}, status={}, code={}", platformId, status, code);

        List<StockPosition> positions;

        if (code != null && !code.isEmpty()) {
            positions = positionRepository.findByCodeOrderByBuyDateDesc(code);
        } else if (platformId != null && status != null && !status.isEmpty()) {
            positions = positionRepository.findByPlatformIdAndStatusOrderByBuyDateDesc(platformId, status);
        } else if (platformId != null) {
            positions = positionRepository.findByPlatformIdOrderByBuyDateDesc(platformId);
        } else if (status != null && !status.isEmpty()) {
            positions = positionRepository.findByStatusOrderByBuyDateDesc(status);
        } else {
            positions = positionRepository.findAll();
        }

        return positions.stream()
                .map(this::convertToPositionResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取持仓中的股票列表
     *
     * @param platformId 平台ID(可选)
     * @return 持仓列表
     */
    public List<StockPositionDTO.Response> getHoldingPositions(Long platformId) {
        log.info("[getHoldingPositions] 获取持仓中的股票: platformId={}", platformId);

        List<StockPosition> positions;
        if (platformId != null) {
            positions = positionRepository.findHoldingPositionsByPlatformId(platformId);
        } else {
            positions = positionRepository.findHoldingPositions();
        }

        return positions.stream()
                .map(this::convertToPositionResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取持仓
     *
     * @param id 持仓ID
     * @return 持仓信息
     */
    public Optional<StockPositionDTO.Response> getPositionById(Long id) {
        log.info("[getPositionById] 获取持仓: id={}", id);
        return positionRepository.findById(id)
                .map(this::convertToPositionResponse);
    }

    /**
     * 更新持仓信息
     *
     * @param id      持仓ID
     * @param request 更新请求
     * @return 更新后的持仓信息
     */
    @Transactional
    public Optional<StockPositionDTO.Response> updatePosition(Long id, StockPositionDTO.UpdateRequest request) {
        log.info("[updatePosition] 更新持仓: id={}", id);

        return positionRepository.findById(id)
                .map(position -> {
                    if (request.getName() != null) position.setName(request.getName());
                    if (request.getStrategy() != null) position.setStrategy(request.getStrategy());
                    if (request.getStopLossPrice() != null) position.setStopLossPrice(request.getStopLossPrice());
                    if (request.getTargetPrice() != null) position.setTargetPrice(request.getTargetPrice());
                    if (request.getNotes() != null) position.setNotes(request.getNotes());
                    if (request.getTags() != null) position.setTags(request.getTags());

                    position = positionRepository.save(position);
                    return convertToPositionResponse(position);
                });
    }

    /**
     * 更新持仓当前价格和盈亏
     *
     * @param id      持仓ID
     * @param request 更新价格请求
     * @return 更新后的持仓信息
     */
    @Transactional
    public Optional<StockPositionDTO.Response> updatePositionPrice(Long id, StockPositionDTO.UpdatePriceRequest request) {
        log.info("[updatePositionPrice] 更新持仓价格: id={}, currentPrice={}", id, request.getCurrentPrice());

        return positionRepository.findById(id)
                .map(position -> {
                    if (!"holding".equals(position.getStatus()) && !"partial".equals(position.getStatus())) {
                        return null;
                    }

                    double currentPrice = request.getCurrentPrice();
                    int currentQuantity = position.getCurrentQuantity();
                    double totalCost = position.getTotalCost();

                    // 计算市值和盈亏
                    double marketValue = currentPrice * currentQuantity;
                    double profitAmount = feeCalculator.calculateProfit(currentPrice, currentQuantity, totalCost);
                    double profitRate = feeCalculator.calculateProfitRate(profitAmount, totalCost);

                    position.setCurrentPrice(currentPrice);
                    position.setMarketValue(marketValue);
                    position.setProfitAmount(profitAmount);
                    position.setProfitRate(profitRate);

                    position = positionRepository.save(position);
                    return convertToPositionResponse(position);
                });
    }

    /**
     * 卖出持仓
     *
     * @param id      持仓ID
     * @param request 卖出请求
     * @return 更新后的持仓信息
     */
    @Transactional
    public Optional<StockPositionDTO.Response> sellPosition(Long id, StockPositionDTO.SellRequest request) {
        log.info("[sellPosition] 卖出持仓: id={}, sellPrice={}", id, request.getSellPrice());

        StockPosition position = positionRepository.findById(id).orElse(null);
        if (position == null) {
            return Optional.empty();
        }

        TradePlatform platform = platformRepository.findById(position.getPlatformId()).orElse(null);
        if (platform == null) {
            return Optional.empty();
        }

        // 确定卖出数量
        int sellQuantity = request.getSellQuantity() != null ? request.getSellQuantity() : position.getCurrentQuantity();
        if (sellQuantity > position.getCurrentQuantity()) {
            throw new IllegalArgumentException("卖出数量不能大于持仓数量");
        }

        // 计算卖出金额和费用
        double sellPrice = request.getSellPrice();
        double sellAmount = sellPrice * sellQuantity;
        FeeCalculator.SellFeeResult sellFees = feeCalculator.calculateSellFees(
                sellAmount, sellQuantity,
                platform.getSellCommissionRate(),
                platform.getSellMinCommission(),
                platform.getStampTaxRate(),
                platform.getTransferFeeRate()
        );

        // 更新持仓信息
        position.setSellPrice(sellPrice);
        position.setSellAmount(sellAmount);
        position.setSellDate(request.getSellDate() != null ? request.getSellDate() : LocalDate.now());
        position.setSellCommission(sellFees.getCommission());
        position.setSellStampTax(sellFees.getStampTax());
        position.setSellTransferFee(sellFees.getTransferFee());

        // 更新持仓数量
        position.setSoldQuantity((position.getSoldQuantity() != null ? position.getSoldQuantity() : 0) + sellQuantity);
        position.setCurrentQuantity(position.getBuyQuantity() - position.getSoldQuantity());

        // 更新状态
        if (position.getCurrentQuantity() <= 0) {
            position.setStatus("sold");
        } else {
            position.setStatus("partial");
        }

        position = positionRepository.save(position);
        return Optional.of(convertToPositionResponse(position));
    }

    /**
     * 删除持仓
     *
     * @param id 持仓ID
     */
    @Transactional
    public void deletePosition(Long id) {
        log.info("[deletePosition] 删除持仓: id={}", id);
        positionRepository.deleteById(id);
    }

    /**
     * 获取持仓汇总统计
     *
     * @param platformId 平台ID(可选)
     * @return 汇总统计
     */
    public StockPositionDTO.Summary getSummary(Long platformId) {
        log.info("[getSummary] 获取持仓汇总: platformId={}", platformId);

        Object[] stats;
        if (platformId != null) {
            stats = positionRepository.calculatePlatformStats(platformId);
        } else {
            stats = positionRepository.calculateTotalStats();
        }

        StockPositionDTO.Summary summary = new StockPositionDTO.Summary();
        if (stats != null && stats.length == 4) {
            summary.setTotalCost(((Number) stats[0]).doubleValue());
            summary.setTotalMarketValue(((Number) stats[1]).doubleValue());
            summary.setTotalProfit(((Number) stats[2]).doubleValue());
            summary.setPositionCount(((Number) stats[3]).longValue());

            double totalCost = summary.getTotalCost();
            if (totalCost > 0) {
                summary.setTotalProfitRate(summary.getTotalProfit() / totalCost * 100);
            } else {
                summary.setTotalProfitRate(0.0);
            }
        }

        return summary;
    }

    // ==================== 转换方法 ====================

    private TradePlatformDTO.Response convertToPlatformResponse(TradePlatform platform) {
        TradePlatformDTO.Response response = new TradePlatformDTO.Response();
        BeanUtils.copyProperties(platform, response);
        return response;
    }

    private StockPositionDTO.Response convertToPositionResponse(StockPosition position) {
        StockPositionDTO.Response response = new StockPositionDTO.Response();
        BeanUtils.copyProperties(position, response);

        // 设置平台名称
        if (position.getPlatform() != null) {
            response.setPlatformName(position.getPlatform().getName());
        } else {
            platformRepository.findById(position.getPlatformId())
                    .ifPresent(p -> response.setPlatformName(p.getName()));
        }

        return response;
    }
}
