package com.mootdx.server.repository;

import com.mootdx.server.entity.StockTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 股票题材数据访问层
 */
@Repository
public interface StockThemeRepository extends JpaRepository<StockTheme, Long> {

    /**
     * 根据题材ID查询
     */
    Optional<StockTheme> findByThemeId(String themeId);

    /**
     * 根据题材名称查询
     */
    Optional<StockTheme> findByThemeName(String themeName);

    /**
     * 根据题材名称模糊搜索
     */
    List<StockTheme> findByThemeNameContainingOrderByStockCountDesc(String keyword);

    /**
     * 获取所有题材，按股票数量降序排列
     */
    List<StockTheme> findAllByOrderByStockCountDesc();

    /**
     * 获取热门题材（股票数量最多的前N个）
     */
    List<StockTheme> findTop20ByOrderByStockCountDesc();

    /**
     * 根据题材ID列表查询
     */
    List<StockTheme> findByThemeIdIn(List<String> themeIds);

    /**
     * 搜索题材（名称或关键词匹配）
     */
    @Query("SELECT t FROM StockTheme t WHERE t.themeName LIKE %:keyword% OR t.themeKeywords LIKE %:keyword% ORDER BY t.stockCount DESC")
    List<StockTheme> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 检查题材是否存在
     */
    boolean existsByThemeId(String themeId);
}
