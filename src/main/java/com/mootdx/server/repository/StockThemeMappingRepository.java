package com.mootdx.server.repository;

import com.mootdx.server.entity.StockThemeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 股票题材关联数据访问层
 */
@Repository
public interface StockThemeMappingRepository extends JpaRepository<StockThemeMapping, Long> {

    /**
     * 根据题材ID查询关联的股票
     */
    List<StockThemeMapping> findByThemeId(String themeId);

    /**
     * 根据股票代码和市场查询关联的题材
     */
    List<StockThemeMapping> findByCodeAndMarket(String code, String market);

    /**
     * 根据题材ID统计股票数量
     */
    long countByThemeId(String themeId);

    /**
     * 删除指定题材的所有关联
     */
    void deleteByThemeId(String themeId);

    /**
     * 删除指定股票的题材关联
     */
    void deleteByCodeAndMarket(String code, String market);

    /**
     * 检查关联是否存在
     */
    boolean existsByCodeAndMarketAndThemeId(String code, String market, String themeId);

    /**
     * 获取有题材关联的所有股票代码
     */
    @Query("SELECT DISTINCT CONCAT(m.market, ':', m.code) FROM StockThemeMapping m")
    List<String> findAllStockCodesWithTheme();

    /**
     * 根据题材ID列表查询关联
     */
    List<StockThemeMapping> findByThemeIdIn(List<String> themeIds);
}
