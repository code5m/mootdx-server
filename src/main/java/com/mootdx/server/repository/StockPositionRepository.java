package com.mootdx.server.repository;

import com.mootdx.server.entity.StockPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 股票持仓Repository
 * 功能：数据访问层，提供持仓记录CRUD操作
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Repository
public interface StockPositionRepository extends JpaRepository<StockPosition, Long> {

    /**
     * 根据平台ID查询持仓列表
     *
     * @param platformId 平台ID
     * @return 持仓列表
     */
    List<StockPosition> findByPlatformIdOrderByBuyDateDesc(Long platformId);

    /**
     * 根据平台ID和状态查询持仓列表
     *
     * @param platformId 平台ID
     * @param status     状态
     * @return 持仓列表
     */
    List<StockPosition> findByPlatformIdAndStatusOrderByBuyDateDesc(Long platformId, String status);

    /**
     * 根据状态查询持仓列表
     *
     * @param status 状态
     * @return 持仓列表
     */
    List<StockPosition> findByStatusOrderByBuyDateDesc(String status);

    /**
     * 根据股票代码查询持仓列表
     *
     * @param code 股票代码
     * @return 持仓列表
     */
    List<StockPosition> findByCodeOrderByBuyDateDesc(String code);

    /**
     * 查询持仓中的股票
     *
     * @return 持仓列表
     */
    @Query("SELECT sp FROM StockPosition sp WHERE sp.status IN ('holding', 'partial') ORDER BY sp.buyDate DESC")
    List<StockPosition> findHoldingPositions();

    /**
     * 查询指定平台的持仓中的股票
     *
     * @param platformId 平台ID
     * @return 持仓列表
     */
    @Query("SELECT sp FROM StockPosition sp WHERE sp.platformId = :platformId AND sp.status IN ('holding', 'partial') ORDER BY sp.buyDate DESC")
    List<StockPosition> findHoldingPositionsByPlatformId(@Param("platformId") Long platformId);

    /**
     * 统计指定平台的持仓数量和总成本
     *
     * @param platformId 平台ID
     * @return 统计结果 [总成本, 总市值, 总盈亏, 持仓数量]
     */
    @Query("SELECT COALESCE(SUM(sp.totalCost), 0), COALESCE(SUM(sp.marketValue), 0), COALESCE(SUM(sp.profitAmount), 0), COUNT(sp) " +
           "FROM StockPosition sp WHERE sp.platformId = :platformId AND sp.status IN ('holding', 'partial')")
    Object[] calculatePlatformStats(@Param("platformId") Long platformId);

    /**
     * 统计所有持仓的总成本和总市值
     *
     * @return 统计结果 [总成本, 总市值, 总盈亏, 持仓数量]
     */
    @Query("SELECT COALESCE(SUM(sp.totalCost), 0), COALESCE(SUM(sp.marketValue), 0), COALESCE(SUM(sp.profitAmount), 0), COUNT(sp) " +
           "FROM StockPosition sp WHERE sp.status IN ('holding', 'partial')")
    Object[] calculateTotalStats();
}
