package com.mootdx.server.repository;

import com.mootdx.server.entity.StockTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTagMappingRepository extends JpaRepository<StockTagMapping, Long> {

    Optional<StockTagMapping> findByCodeAndMarketAndTagId(String code, String market, String tagId);

    List<StockTagMapping> findByCodeAndMarket(String code, String market);

    List<StockTagMapping> findByCodeAndMarketAndTagType(String code, String market, String tagType);

    List<StockTagMapping> findByTagId(String tagId);

    @Query("SELECT m FROM StockTagMapping m WHERE m.tagId = :tagId ORDER BY m.weight DESC")
    List<StockTagMapping> findByTagIdOrderByWeightDesc(@Param("tagId") String tagId);

    long countByTagId(String tagId);

    void deleteByCodeAndMarketAndTagId(String code, String market, String tagId);

    @Query("SELECT DISTINCT m.code, m.market FROM StockTagMapping m WHERE m.tagId = :tagId")
    List<Object[]> findStockCodesByTagId(@Param("tagId") String tagId);
}
