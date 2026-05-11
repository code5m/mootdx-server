package com.mootdx.server.repository;

import com.mootdx.server.entity.StockThemeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 股票题材详细描述数据访问层
 */
@Repository
public interface StockThemeDetailRepository extends JpaRepository<StockThemeDetail, Long> {

    /**
     * 根据股票代码和市场查询详细描述
     */
    Optional<StockThemeDetail> findByCodeAndMarket(String code, String market);

    /**
     * 检查是否存在
     */
    boolean existsByCodeAndMarket(String code, String market);

    /**
     * 删除指定股票的详细描述
     */
    void deleteByCodeAndMarket(String code, String market);
}
