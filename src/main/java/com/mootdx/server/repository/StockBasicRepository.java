package com.mootdx.server.repository;

import com.mootdx.server.entity.StockBasic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 股票基础信息数据访问层
 */
@Repository
public interface StockBasicRepository extends JpaRepository<StockBasic, Long> {

    /**
     * 根据代码和市场查询
     */
    Optional<StockBasic> findByCodeAndMarket(String code, String market);

    /**
     * 根据代码查询（跨市场）
     */
    List<StockBasic> findByCode(String code);

    /**
     * 检查股票是否存在
     */
    boolean existsByCodeAndMarket(String code, String market);

    /**
     * 根据市场查询
     */
    List<StockBasic> findByMarket(String market);

    /**
     * 查询没有题材关联的股票
     */
    @Query(value = "SELECT sb FROM StockBasic sb WHERE CONCAT(sb.market, ':', sb.code) NOT IN " +
                   "(SELECT CONCAT(m.market, ':', m.code) FROM StockThemeMapping m)")
    List<StockBasic> findStocksWithoutTheme();

    /**
     * 统计没有题材关联的股票数量
     */
    @Query(value = "SELECT COUNT(sb) FROM StockBasic sb WHERE CONCAT(sb.market, ':', sb.code) NOT IN " +
                   "(SELECT CONCAT(m.market, ':', m.code) FROM StockThemeMapping m)")
    long countStocksWithoutTheme();

    /**
     * 根据代码列表查询
     */
    @Query("SELECT sb FROM StockBasic sb WHERE sb.code IN :codes AND sb.market IN :markets")
    List<StockBasic> findByCodeAndMarketIn(@Param("codes") List<String> codes, @Param("markets") List<String> markets);
}
