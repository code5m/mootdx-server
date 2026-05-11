package com.mootdx.server.repository;

import com.mootdx.server.entity.TdxServerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TDX服务器配置数据访问层
 * 功能：提供服务器配置的数据库操作方法
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface TdxServerConfigRepository extends JpaRepository<TdxServerConfig, Long> {

    /**
     * 根据IP和端口查询服务器配置
     *
     * @param host 服务器IP地址
     * @param port 服务器端口
     * @return 服务器配置对象
     */
    Optional<TdxServerConfig> findByHostAndPort(String host, Integer port);

    /**
     * 查询所有启用的服务器配置
     *
     * @return 启用的服务器配置列表
     */
    List<TdxServerConfig> findByIsEnabledTrue();

    /**
     * 查询所有启用的服务器配置并按默认排序
     *
     * @return 启用的服务器配置列表，默认服务器排在前面
     */
    List<TdxServerConfig> findByIsEnabledTrueOrderByIsDefaultDescIdAsc();

    /**
     * 查询所有服务器配置并按默认排序
     *
     * @return 服务器配置列表，默认服务器排在前面
     */
    List<TdxServerConfig> findAllByOrderByIsDefaultDescIdAsc();

    /**
     * 查询默认服务器配置
     *
     * @return 默认服务器配置
     */
    Optional<TdxServerConfig> findByIsDefaultTrue();

    /**
     * 将所有服务器设为非默认
     * 使用@Modifying标记为修改操作
     */
    @Modifying
    @Query("UPDATE TdxServerConfig s SET s.isDefault = false")
    void clearAllDefault();

    /**
     * 检查指定IP和端口的服务器是否已存在
     *
     * @param host 服务器IP地址
     * @param port 服务器端口
     * @return 是否存在
     */
    boolean existsByHostAndPort(String host, Integer port);

    /**
     * 根据位置查询服务器配置
     *
     * @param location 服务器位置
     * @return 服务器配置列表
     */
    List<TdxServerConfig> findByLocation(String location);
}
