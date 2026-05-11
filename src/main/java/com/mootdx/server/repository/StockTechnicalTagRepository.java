package com.mootdx.server.repository;

import com.mootdx.server.entity.StockTechnicalTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockTechnicalTagRepository extends JpaRepository<StockTechnicalTag, Long> {

    Optional<StockTechnicalTag> findByCodeAndMarketAndIndicatorNameAndTradeDateAndSignalType(
            String code, String market, String indicatorName, LocalDate tradeDate, String signalType);

    List<StockTechnicalTag> findByCodeAndMarket(String code, String market);

    List<StockTechnicalTag> findByCodeAndMarketOrderByTradeDateDesc(String code, String market);

    @Query("SELECT t FROM StockTechnicalTag t WHERE t.code = :code AND t.market = :market " +
           "AND t.tradeDate BETWEEN :startDate AND :endDate ORDER BY t.tradeDate DESC")
    List<StockTechnicalTag> findByCodeAndMarketAndTradeDateBetween(
            @Param("code") String code, @Param("market") String market,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM StockTechnicalTag t WHERE " +
           "(:code IS NULL OR t.code = :code) AND " +
           "(:market IS NULL OR t.market = :market) AND " +
           "(:indicatorName IS NULL OR t.indicatorName = :indicatorName) AND " +
           "(:signalType IS NULL OR t.signalType = :signalType) AND " +
           "(:tradeDate IS NULL OR t.tradeDate = :tradeDate) AND " +
           "(:startDate IS NULL OR t.tradeDate >= :startDate) AND " +
           "(:endDate IS NULL OR t.tradeDate <= :endDate) AND " +
           "(:isActive IS NULL OR t.isActive = :isActive) " +
           "ORDER BY t.tradeDate DESC")
    List<StockTechnicalTag> searchTechnicalTags(
            @Param("code") String code, @Param("market") String market,
            @Param("indicatorName") String indicatorName, @Param("signalType") String signalType,
            @Param("tradeDate") LocalDate tradeDate, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, @Param("isActive") Integer isActive);

    @Query("SELECT t FROM StockTechnicalTag t WHERE t.indicatorName = :indicatorName " +
           "AND t.signalType = :signalType AND t.tradeDate = :tradeDate " +
           "AND t.signalStrength >= :minStrength AND t.isActive = 1 " +
           "ORDER BY t.signalStrength DESC")
    List<StockTechnicalTag> findBySignal(
            @Param("indicatorName") String indicatorName,
            @Param("signalType") String signalType,
            @Param("tradeDate") LocalDate tradeDate,
            @Param("minStrength") Integer minStrength);

    List<StockTechnicalTag> findByTradeDateAndIsActiveOrderBySignalStrengthDesc(LocalDate tradeDate, Integer isActive);
}
