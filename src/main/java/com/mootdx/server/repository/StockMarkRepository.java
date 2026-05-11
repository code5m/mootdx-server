package com.mootdx.server.repository;

import com.mootdx.server.entity.StockMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 股票备注标记Repository接口
 * 提供备注数据的增删改查操作
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Repository
public interface StockMarkRepository extends JpaRepository<StockMark, Long> {

    /**
     * 根据股票代码查询所有标记
     *
     * @param stockCode 股票代码
     * @return 标记列表
     */
    List<StockMark> findByStockCode(String stockCode);

    /**
     * 根据标记类型查询
     *
     * @param markType 标记类型
     * @return 标记列表
     */
    List<StockMark> findByMarkType(Integer markType);

    /**
     * 根据股票代码和标记类型查询
     *
     * @param stockCode 股票代码
     * @param markType  标记类型
     * @return 标记列表
     */
    List<StockMark> findByStockCodeAndMarkType(String stockCode, Integer markType);

    /**
     * 根据股票代码、标记类型和标记时间查询
     *
     * @param stockCode 股票代码
     * @param markType  标记类型
     * @param markTime  标记时间
     * @return 标记信息
     */
    Optional<StockMark> findByStockCodeAndMarkTypeAndMarkTime(String stockCode, Integer markType, String markTime);

    /**
     * 删除指定股票的所有标记
     *
     * @param stockCode 股票代码
     */
    void deleteByStockCode(String stockCode);

    /**
     * 检查标记是否存在
     *
     * @param stockCode 股票代码
     * @param markType  标记类型
     * @param markTime  标记时间
     * @return 是否存在
     */
    boolean existsByStockCodeAndMarkTypeAndMarkTime(String stockCode, Integer markType, String markTime);

    /**
     * 统计指定股票的标记数量
     *
     * @param stockCode 股票代码
     * @return 标记数量
     */
    long countByStockCode(String stockCode);
}
