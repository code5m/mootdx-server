package com.mootdx.server.repository;

import com.mootdx.server.entity.TradePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 交易平台Repository
 * 功能：数据访问层，提供交易平台CRUD操作
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Repository
public interface TradePlatformRepository extends JpaRepository<TradePlatform, Long> {

    /**
     * 查询所有启用的平台，按排序顺序排列
     *
     * @return 平台列表
     */
    List<TradePlatform> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * 查询所有平台，按排序顺序排列
     *
     * @return 平台列表
     */
    List<TradePlatform> findAllByOrderBySortOrderAsc();

    /**
     * 根据ID查询启用的平台
     *
     * @param id 平台ID
     * @return 平台信息
     */
    Optional<TradePlatform> findByIdAndIsActiveTrue(Long id);

    /**
     * 查询默认平台
     *
     * @return 默认平台
     */
    Optional<TradePlatform> findByIsDefaultTrue();
}
