package com.mootdx.server.repository;

import com.mootdx.server.entity.StockBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 股票板块Repository接口
 * 提供板块数据的增删改查操作
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Repository
public interface StockBlockRepository extends JpaRepository<StockBlock, Long> {

    /**
     * 根据板块代码查询所有股票
     *
     * @param blockCode 板块代码
     * @return 板块股票列表
     */
    List<StockBlock> findByBlockCode(String blockCode);

    /**
     * 根据板块类型查询
     *
     * @param blockType 板块类型(system/custom)
     * @return 板块列表
     */
    List<StockBlock> findByBlockType(String blockType);

    /**
     * 根据股票代码查询所属板块
     *
     * @param stockCode 股票代码
     * @return 板块列表
     */
    List<StockBlock> findByStockCode(String stockCode);

    /**
     * 删除指定板块的所有股票
     *
     * @param blockCode 板块代码
     */
    void deleteByBlockCode(String blockCode);

    /**
     * 检查板块股票是否存在
     *
     * @param blockCode 板块代码
     * @param stockCode 股票代码
     * @return 是否存在
     */
    boolean existsByBlockCodeAndStockCode(String blockCode, String stockCode);

    /**
     * 统计板块中的股票数量
     *
     * @param blockCode 板块代码
     * @return 股票数量
     */
    long countByBlockCode(String blockCode);
}
