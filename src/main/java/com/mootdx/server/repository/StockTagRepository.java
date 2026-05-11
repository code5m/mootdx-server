package com.mootdx.server.repository;

import com.mootdx.server.entity.StockTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTagRepository extends JpaRepository<StockTag, Long> {

    Optional<StockTag> findByTagId(String tagId);

    List<StockTag> findByTagType(String tagType);

    List<StockTag> findByTagTypeAndParentTagId(String tagType, String parentTagId);

    @Query("SELECT t FROM StockTag t WHERE t.tagType = :tagType ORDER BY t.stockCount DESC")
    List<StockTag> findByTagTypeOrderByStockCountDesc(@Param("tagType") String tagType);

    @Query("SELECT t FROM StockTag t WHERE " +
           "t.tagName LIKE %:keyword% OR " +
           "t.keywords LIKE %:keyword% OR " +
           "t.description LIKE %:keyword%")
    List<StockTag> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT t FROM StockTag t WHERE t.tagType = :tagType AND (" +
           "t.tagName LIKE %:keyword% OR " +
           "t.keywords LIKE %:keyword% OR " +
           "t.description LIKE %:keyword%)")
    List<StockTag> searchByKeywordAndType(@Param("keyword") String keyword, @Param("tagType") String tagType);

    boolean existsByTagId(String tagId);
}
