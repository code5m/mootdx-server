package com.mootdx.server.repository;

import com.mootdx.server.entity.StockTagHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockTagHistoryRepository extends JpaRepository<StockTagHistory, Long> {

    List<StockTagHistory> findByCodeAndMarketOrderByCreatedAtDesc(String code, String market);

    List<StockTagHistory> findByTagIdOrderByCreatedAtDesc(String tagId);

    @Query("SELECT h FROM StockTagHistory h WHERE h.code = :code AND h.market = :market AND h.tagId = :tagId ORDER BY h.createdAt DESC")
    List<StockTagHistory> findByCodeAndMarketAndTagId(
            @Param("code") String code, @Param("market") String market, @Param("tagId") String tagId);

    @Query("SELECT h FROM StockTagHistory h WHERE h.createdAt >= :startTime ORDER BY h.createdAt DESC")
    List<StockTagHistory> findRecentHistory(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT h FROM StockTagHistory h WHERE h.operator = :operator ORDER BY h.createdAt DESC")
    List<StockTagHistory> findByOperator(@Param("operator") String operator);
}
