package com.mootdx.server.repository;

import com.mootdx.server.entity.StockIndustry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockIndustryRepository extends JpaRepository<StockIndustry, Long> {

    Optional<StockIndustry> findByCodeAndMarketAndStandard(String code, String market, String standard);

    List<StockIndustry> findByCodeAndMarket(String code, String market);

    List<StockIndustry> findByStandard(String standard);

    List<StockIndustry> findByIndustryLevel1Code(String level1Code);

    List<StockIndustry> findByIndustryLevel2Code(String level2Code);

    @Query("SELECT DISTINCT i.industryLevel1Code, i.industryLevel1Name FROM StockIndustry i WHERE i.standard = :standard")
    List<Object[]> findDistinctLevel1ByStandard(@Param("standard") String standard);

    @Query("SELECT DISTINCT i.industryLevel2Code, i.industryLevel2Name FROM StockIndustry i " +
           "WHERE i.standard = :standard AND i.industryLevel1Code = :level1Code")
    List<Object[]> findDistinctLevel2ByStandardAndLevel1(
            @Param("standard") String standard, @Param("level1Code") String level1Code);

    @Query("SELECT DISTINCT i.industryLevel3Code, i.industryLevel3Name FROM StockIndustry i " +
           "WHERE i.standard = :standard AND i.industryLevel2Code = :level2Code")
    List<Object[]> findDistinctLevel3ByStandardAndLevel2(
            @Param("standard") String standard, @Param("level2Code") String level2Code);
}
