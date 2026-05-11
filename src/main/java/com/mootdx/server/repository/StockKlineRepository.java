package com.mootdx.server.repository;

import com.mootdx.server.entity.StockKline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * K线数据Repository接口
 * 提供K线数据的增删改查操作
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Repository
public interface StockKlineRepository extends JpaRepository<StockKline, Long> {

    /**
     * 查询指定股票、频率的K线数据（按时间倒序）
     *
     * @param code   股票代码
     * @param market 市场代码
     * @param freq   频率
     * @return K线数据列表
     */
    List<StockKline> findTop500ByCodeAndMarketAndFreqOrderByTradeDateDescTradeTimeDesc(
            String code, String market, String freq);

    /**
     * 查询指定股票、频率、日期范围的K线数据（按时间倒序）
     *
     * @param code      股票代码
     * @param market    市场代码
     * @param freq      频率
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return K线数据列表
     */
    @Query("SELECT sk FROM StockKline sk WHERE sk.code = :code AND sk.market = :market AND sk.freq = :freq AND sk.tradeDate BETWEEN :startDate AND :endDate ORDER BY sk.tradeDate DESC, sk.tradeTime DESC")
    List<StockKline> findByCodeAndMarketAndFreqAndTradeDateBetweenOrderByTradeDateDesc(
            @Param("code") String code,
            @Param("market") String market,
            @Param("freq") String freq,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询指定股票、频率的最新一条K线数据
     *
     * @param code   股票代码
     * @param market 市场代码
     * @param freq   频率
     * @return 最新的K线数据
     */
    Optional<StockKline> findTopByCodeAndMarketAndFreqOrderByTradeDateDescTradeTimeDesc(
            String code, String market, String freq);

    /**
     * 删除指定股票、频率的所有K线数据
     *
     * @param code   股票代码
     * @param market 市场代码
     * @param freq   频率
     */
    void deleteByCodeAndMarketAndFreq(String code, String market, String freq);

    /**
     * 检查指定股票、频率、日期的K线数据是否存在（不区分大小写）
     *
     * @param code      股票代码
     * @param market    市场代码
     * @param freq      频率
     * @param tradeDate 交易日期
     * @return 是否存在
     */
    @Query("SELECT COUNT(sk) > 0 FROM StockKline sk WHERE sk.code = :code AND UPPER(sk.market) = UPPER(:market) AND sk.freq = :freq AND sk.tradeDate = :tradeDate")
    boolean existsByCodeAndMarketAndFreqAndTradeDate(@Param("code") String code, @Param("market") String market, @Param("freq") String freq, @Param("tradeDate") LocalDate tradeDate);

    /**
     * 查询指定股票、频率的所有K线数据（不区分大小写）
     *
     * @param code   股票代码
     * @param market 市场代码
     * @param freq   频率
     * @return K线数据列表
     */
    @Query("SELECT sk FROM StockKline sk WHERE sk.code = :code AND UPPER(sk.market) = UPPER(:market) AND sk.freq = :freq")
    List<StockKline> findByCodeAndMarketAndFreq(@Param("code") String code, @Param("market") String market, @Param("freq") String freq);

    /**
     * 统计指定市场、频率的K线记录数（不区分大小写）
     *
     * @param market 市场代码
     * @param freq   频率
     * @return 记录数
     */
    @Query("SELECT COUNT(sk) FROM StockKline sk WHERE UPPER(sk.market) = UPPER(:market) AND sk.freq = :freq")
    long countByMarketAndFreq(@Param("market") String market, @Param("freq") String freq);

    /**
     * 统计指定股票、市场、频率的K线记录数（不区分大小写）
     *
     * @param code   股票代码
     * @param market 市场代码
     * @param freq   频率
     * @return 记录数
     */
    @Query("SELECT COUNT(sk) FROM StockKline sk WHERE sk.code = :code AND UPPER(sk.market) = UPPER(:market) AND sk.freq = :freq")
    long countByCodeAndMarketAndFreq(@Param("code") String code, @Param("market") String market, @Param("freq") String freq);

    /**
     * 批量统计指定股票列表、市场、频率的K线记录数
     *
     * @param codes  股票代码列表
     * @param market 市场代码
     * @param freq   频率
     * @return 每个股票的记录数 [code, count]
     */
    @Query("SELECT sk.code, COUNT(sk) FROM StockKline sk WHERE sk.code IN :codes AND UPPER(sk.market) = UPPER(:market) AND sk.freq = :freq GROUP BY sk.code")
    List<Object[]> countByCodeListAndMarketAndFreq(@Param("codes") List<String> codes, @Param("market") String market, @Param("freq") String freq);

    /**
     * 批量查询指定股票列表的最新日期
     *
     * @param codes  股票代码列表
     * @param market 市场代码
     * @param freq   频率
     * @return 每个股票的最新日期 [code, max(tradeDate)]
     */
    @Query("SELECT sk.code, MAX(sk.tradeDate) FROM StockKline sk WHERE sk.code IN :codes AND UPPER(sk.market) = UPPER(:market) AND sk.freq = :freq GROUP BY sk.code")
    List<Object[]> findLatestDateByCodeList(@Param("codes") List<String> codes, @Param("market") String market, @Param("freq") String freq);
}
