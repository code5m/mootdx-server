package com.mootdx.server.service;

import com.mootdx.server.config.TdxServerProperties;
import com.mootdx.server.dto.ServerConfigDTO;
import com.mootdx.server.dto.ServerStatisticsDTO;
import com.mootdx.server.dto.ServerStatusDTO;
import com.mootdx.server.entity.*;
import com.mootdx.server.repository.TdxServerConfigRepository;
import com.mootdx.server.repository.TdxServerLogRepository;
import com.mootdx.server.repository.TdxServerNodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TDX服务器管理服务类
 * 功能：管理TDX行情服务器配置、监控节点状态、自动切换服务器
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TdxServerService {

    private final TdxServerConfigRepository configRepository;
    private final TdxServerNodeRepository nodeRepository;
    private final TdxServerLogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final TdxServerProperties tdxServerProperties;

    /**
     * 应用启动时初始化默认服务器
     */
    @PostConstruct
    public void initDefaultServers() {
        log.info("[方法入口] TdxServerService.initDefaultServers - 初始化默认服务器");
        int createdCount = 0;

        // 从配置文件读取初始化服务器列表
        List<TdxServerProperties.ServerConfig> initServers = tdxServerProperties.getInitServers();
        if (initServers == null || initServers.isEmpty()) {
            log.warn("[方法内日志] 配置文件中没有配置初始化服务器列表，跳过初始化");
            return;
        }

        for (TdxServerProperties.ServerConfig serverConfig : initServers) {
            String host = serverConfig.getHost();
            int port = serverConfig.getPort();

            // 检查是否已存在
            if (!configRepository.existsByHostAndPort(host, port)) {
                TdxServerConfig server = TdxServerConfig.builder()
                        .name(serverConfig.getName())
                        .host(host)
                        .port(port)
                        .location(serverConfig.getLocation())
                        .weight(serverConfig.getWeight())
                        .serverType(ServerType.STD)
                        .isEnabled(true)
                        .isDefault(createdCount == 0) // 第一个设为默认
                        .build();

                configRepository.save(server);

                // 创建对应的节点记录
                TdxServerNode node = TdxServerNode.builder()
                        .serverId(server.getId())
                        .status(ServerStatus.UNKNOWN)
                        .successRate(0.0)
                        .failCount(0)
                        .totalRequests(0)
                        .successRequests(0)
                        .build();
                nodeRepository.save(node);

                createdCount++;
                log.info("[方法内日志] 创建服务器: {} ({}:{})", server.getName(), server.getHost(), server.getPort());
            }
        }

        log.info("[方法出口] 成功创建 {} 个默认服务器", createdCount);
    }

    /**
     * 获取所有服务器列表
     *
     * @param enabledOnly 是否只返回启用的服务器
     * @return 服务器状态DTO列表
     */
    public List<ServerStatusDTO> getServers(boolean enabledOnly) {
        log.info("[方法入口] TdxServerService.getServers - 获取服务器列表, enabledOnly={}", enabledOnly);

        List<TdxServerConfig> servers;
        if (enabledOnly) {
            servers = configRepository.findByIsEnabledTrueOrderByIsDefaultDescIdAsc();
        } else {
            servers = configRepository.findAllByOrderByIsDefaultDescIdAsc();
        }

        List<ServerStatusDTO> result = servers.stream()
                .map(this::convertToStatusDTO)
                .collect(Collectors.toList());

        log.info("[方法出口] 返回 {} 个服务器", result.size());
        return result;
    }

    /**
     * 根据ID获取服务器详情
     *
     * @param serverId 服务器ID
     * @return 服务器状态DTO
     */
    public ServerStatusDTO getServerById(Long serverId) {
        log.info("[方法入口] TdxServerService.getServerById - 获取服务器详情, serverId={}", serverId);

        TdxServerConfig server = configRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("服务器不存在: " + serverId));

        ServerStatusDTO dto = convertToStatusDTO(server);
        log.info("[方法出口] 返回服务器: {}", dto.getName());
        return dto;
    }

    /**
     * 创建服务器配置
     *
     * @param dto 服务器配置DTO
     * @return 创建后的服务器状态DTO
     */
    @Transactional
    public ServerStatusDTO createServer(ServerConfigDTO dto) {
        log.info("[方法入口] TdxServerService.createServer - 创建服务器, name={}", dto.getName());

        // 检查是否已存在
        if (configRepository.existsByHostAndPort(dto.getHost(), dto.getPort())) {
            throw new RuntimeException("服务器已存在: " + dto.getHost() + ":" + dto.getPort());
        }

        TdxServerConfig server = new TdxServerConfig();
        BeanUtils.copyProperties(dto, server);
        configRepository.save(server);

        // 创建对应的节点记录
        TdxServerNode node = TdxServerNode.builder()
                .serverId(server.getId())
                .status(ServerStatus.UNKNOWN)
                .successRate(0.0)
                .failCount(0)
                .totalRequests(0)
                .successRequests(0)
                .build();
        nodeRepository.save(node);

        // 记录日志
        addLog(server.getId(), "create", "创建服务器: " + server.getName(), null, dtoToJson(dto));

        log.info("[方法出口] 服务器创建成功: {}", server.getId());
        return convertToStatusDTO(server);
    }

    /**
     * 更新服务器配置
     *
     * @param serverId 服务器ID
     * @param dto      服务器配置DTO
     * @return 更新后的服务器状态DTO
     */
    @Transactional
    public ServerStatusDTO updateServer(Long serverId, ServerConfigDTO dto) {
        log.info("[方法入口] TdxServerService.updateServer - 更新服务器, serverId={}", serverId);

        TdxServerConfig server = configRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("服务器不存在: " + serverId));

        // 记录旧值
        ServerConfigDTO oldValue = new ServerConfigDTO();
        BeanUtils.copyProperties(server, oldValue);

        // 如果要设置为默认服务器，先清除其他服务器的默认状态
        if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(server.getIsDefault())) {
            log.info("[updateServer] 设置默认服务器，先清除其他默认状态");
            configRepository.clearAllDefault();
        }

        // 更新字段
        BeanUtils.copyProperties(dto, server);
        configRepository.save(server);

        // 记录日志
        addLog(server.getId(), "update", "更新服务器: " + server.getName(),
                dtoToJson(oldValue), dtoToJson(dto));

        log.info("[方法出口] 服务器更新成功: {}", serverId);
        return convertToStatusDTO(server);
    }

    /**
     * 删除服务器配置
     *
     * @param serverId 服务器ID
     */
    @Transactional
    public void deleteServer(Long serverId) {
        log.info("[方法入口] TdxServerService.deleteServer - 删除服务器, serverId={}", serverId);

        TdxServerConfig server = configRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("服务器不存在: " + serverId));

        // 记录日志
        addLog(server.getId(), "delete", "删除服务器: " + server.getName(),
                dtoToJson(server), null);

        // 删除关联的节点和日志
        nodeRepository.deleteByServerId(serverId);
        logRepository.deleteByServerId(serverId);

        // 删除服务器
        configRepository.delete(server);

        log.info("[方法出口] 服务器删除成功: {}", serverId);
    }

    /**
     * 设置默认服务器
     *
     * @param serverId 服务器ID
     */
    @Transactional
    public void setDefaultServer(Long serverId) {
        log.info("[方法入口] TdxServerService.setDefaultServer - 设置默认服务器, serverId={}", serverId);

        TdxServerConfig server = configRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("服务器不存在: " + serverId));

        // 先将所有服务器设为非默认
        configRepository.clearAllDefault();

        // 设置指定服务器为默认
        server.setIsDefault(true);
        configRepository.save(server);

        // 记录日志
        addLog(server.getId(), "set_default", "设置默认服务器: " + server.getName(), null, null);

        log.info("[方法出口] 默认服务器设置成功: {}", server.getName());
    }

    /**
     * 检查单个服务器状态
     *
     * @param serverId 服务器ID
     * @return 节点状态
     */
    @Transactional
    public TdxServerNode checkServerStatus(Long serverId) {
        log.info("[方法入口] TdxServerService.checkServerStatus - 检查服务器状态, serverId={}", serverId);

        TdxServerConfig server = configRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("服务器不存在: " + serverId));

        TdxServerNode node = nodeRepository.findByServerId(serverId)
                .orElseGet(() -> TdxServerNode.builder()
                        .serverId(serverId)
                        .status(ServerStatus.UNKNOWN)
                        .successRate(0.0)
                        .failCount(0)
                        .totalRequests(0)
                        .successRequests(0)
                        .build());

        // 测试连接
        long startTime = System.currentTimeMillis();
        int timeout = server.getTimeout() != null ? server.getTimeout() : 5;
        boolean isOnline = testConnection(server.getHost(), server.getPort(), timeout);
        long responseTime = System.currentTimeMillis() - startTime;

        node.setTotalRequests(node.getTotalRequests() + 1);
        node.setLastCheck(LocalDateTime.now());

        if (isOnline) {
            node.setStatus(ServerStatus.ONLINE);
            node.setResponseTime((double) responseTime);
            node.setFailCount(0);
            node.setSuccessRequests(node.getSuccessRequests() + 1);
            log.info("[方法内日志] 服务器在线: {}, 响应时间: {}ms", server.getName(), responseTime);
        } else {
            node.setStatus(ServerStatus.OFFLINE);
            node.setFailCount(node.getFailCount() + 1);
            log.warn("[方法内日志] 服务器离线: {}", server.getName());
        }

        // 计算成功率
        if (node.getTotalRequests() > 0) {
            node.setSuccessRate((double) node.getSuccessRequests() / node.getTotalRequests());
        }

        // 检查是否不稳定（连续失败3次以上）
        if (node.getFailCount() >= 3) {
            node.setStatus(ServerStatus.UNSTABLE);
            log.warn("[方法内日志] 服务器不稳定: {}, 连续失败{}次", server.getName(), node.getFailCount());
        }

        nodeRepository.save(node);

        log.info("[方法出口] 服务器状态检查完成: {}, 状态: {}", server.getName(), node.getStatus());
        return node;
    }

    /**
     * 检查所有服务器状态
     *
     * @return 服务器状态DTO列表
     */
    public List<ServerStatusDTO> checkAllServers() {
        log.info("[方法入口] TdxServerService.checkAllServers - 检查所有服务器状态");

        List<TdxServerConfig> servers = configRepository.findByIsEnabledTrue();
        List<ServerStatusDTO> results = new ArrayList<>();

        for (TdxServerConfig server : servers) {
            checkServerStatus(server.getId());
            results.add(convertToStatusDTO(server));
        }

        log.info("[方法出口] 完成 {} 个服务器的状态检查", results.size());
        return results;
    }

    /**
     * 获取最佳服务器
     *
     * @return 最佳服务器配置
     */
    public TdxServerConfig getBestServer() {
        log.info("[方法入口] TdxServerService.getBestServer - 获取最佳服务器");

        // 获取在线节点
        List<TdxServerNode> onlineNodes = nodeRepository.findByStatus(ServerStatus.ONLINE);

        // 过滤掉禁用服务器的节点
        List<Long> enabledServerIds = configRepository.findByIsEnabledTrue()
                .stream()
                .map(TdxServerConfig::getId)
                .collect(Collectors.toList());

        onlineNodes = onlineNodes.stream()
                .filter(n -> enabledServerIds.contains(n.getServerId()))
                .collect(Collectors.toList());

        if (onlineNodes.isEmpty()) {
            log.warn("[方法内日志] 没有在线的服务器");
            return null;
        }

        // 按响应时间和成功率排序
        TdxServerNode bestNode = onlineNodes.stream()
                .min(Comparator.comparing(TdxServerNode::getResponseTime,
                        Comparator.nullsLast(Double::compareTo))
                        .thenComparing(Comparator.comparing(TdxServerNode::getSuccessRate,
                                Comparator.nullsLast(Double::compareTo)).reversed()))
                .orElse(null);

        if (bestNode == null) {
            log.warn("[方法出口] 未找到最佳服务器");
            return null;
        }

        TdxServerConfig bestServer = configRepository.findById(bestNode.getServerId()).orElse(null);

        if (bestServer != null) {
            log.info("[方法出口] 最佳服务器: {}, 响应时间: {}ms",
                    bestServer.getName(), bestNode.getResponseTime());
        }

        return bestServer;
    }

    /**
     * 获取最佳可用服务器
     * 如果当前没有在线服务器，尝试检查所有服务器并返回第一个可用的
     *
     * @return 最佳可用服务器配置
     */
    public TdxServerConfig getBestAvailableServer() {
        log.info("[方法入口] TdxServerService.getBestAvailableServer - 获取最佳可用服务器");

        // 首先尝试获取已知的最佳服务器
        TdxServerConfig bestServer = getBestServer();
        if (bestServer != null) {
            log.info("[方法出口] 使用已知的最佳服务器: {}", bestServer.getName());
            return bestServer;
        }

        // 如果没有在线服务器，尝试检查所有启用的服务器
        log.info("[方法内日志] 没有在线服务器，尝试检查所有服务器");
        List<TdxServerConfig> enabledServers = configRepository.findByIsEnabledTrue();

        for (TdxServerConfig server : enabledServers) {
            try {
                TdxServerNode node = checkServerStatus(server.getId());
                if (node != null && (node.getStatus() == ServerStatus.ONLINE || node.getStatus() == ServerStatus.UNKNOWN)) {
                    log.info("[方法出口] 找到可用服务器: {}", server.getName());
                    return server;
                }
            } catch (Exception e) {
                log.warn("[方法内日志] 检查服务器失败: {}, 错误: {}", server.getName(), e.getMessage());
            }
        }

        // 如果仍然没有可用服务器，返回默认服务器（第一个启用的）
        if (!enabledServers.isEmpty()) {
            TdxServerConfig defaultServer = enabledServers.get(0);
            log.warn("[方法出口] 没有检测到可用服务器，返回默认服务器: {}", defaultServer.getName());
            return defaultServer;
        }

        log.error("[方法出口] 没有可用的服务器");
        return null;
    }

    /**
     * 获取服务器统计信息
     *
     * @return 统计数据DTO
     */
    public ServerStatisticsDTO getStatistics() {
        log.info("[方法入口] TdxServerService.getStatistics - 获取统计信息");

        ServerStatisticsDTO stats = new ServerStatisticsDTO();

        stats.setTotalServers(configRepository.count());
        stats.setEnabledServers((long) configRepository.findByIsEnabledTrue().size());
        stats.setOnlineNodes(nodeRepository.countByStatus(ServerStatus.ONLINE));
        stats.setOfflineNodes(nodeRepository.countByStatus(ServerStatus.OFFLINE));
        stats.setUnstableNodes(nodeRepository.countByStatus(ServerStatus.UNSTABLE));
        stats.setMaintenanceNodes(nodeRepository.countByStatus(ServerStatus.MAINTENANCE));
        stats.setUnknownNodes(nodeRepository.countByStatus(ServerStatus.UNKNOWN));

        Double avgResponse = nodeRepository.findAverageResponseTime();
        Double avgSuccessRate = nodeRepository.findAverageSuccessRate();

        stats.setAvgResponseTime(avgResponse != null ? Math.round(avgResponse * 100.0) / 100.0 : 0.0);
        stats.setAvgSuccessRate(avgSuccessRate != null ? Math.round(avgSuccessRate * 10000.0) / 100.0 : 0.0);

        // 获取默认服务器
        configRepository.findByIsDefaultTrue().ifPresent(defaultServer -> {
            stats.setDefaultServerId(defaultServer.getId());
            stats.setDefaultServerName(defaultServer.getName());
        });

        log.info("[方法出口] 统计信息: 总计{}, 在线{}, 离线{}",
                stats.getTotalServers(), stats.getOnlineNodes(), stats.getOfflineNodes());
        return stats;
    }

    /**
     * 获取默认服务器
     *
     * @return 默认服务器配置
     */
    public TdxServerConfig getDefaultServer() {
        return configRepository.findByIsDefaultTrue()
                .orElseGet(() -> {
                    // 如果没有默认服务器，返回第一个启用的服务器
                    List<TdxServerConfig> enabled = configRepository.findByIsEnabledTrue();
                    return enabled.isEmpty() ? null : enabled.get(0);
                });
    }

    /**
     * 测试服务器连接
     *
     * @param host    服务器IP
     * @param port    服务器端口
     * @param timeout 超时时间(秒)
     * @return 是否连接成功
     */
    private boolean testConnection(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout * 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将服务器配置转换为状态DTO
     *
     * @param server 服务器配置
     * @return 状态DTO
     */
    private ServerStatusDTO convertToStatusDTO(TdxServerConfig server) {
        ServerStatusDTO dto = new ServerStatusDTO();
        BeanUtils.copyProperties(server, dto);
        dto.setServerType(server.getServerType().getValue());

        // 获取节点状态
        nodeRepository.findByServerId(server.getId()).ifPresent(node -> {
            dto.setStatus(node.getStatus());
            dto.setResponseTime(node.getResponseTime());
            dto.setSuccessRate(node.getSuccessRate() != null ? node.getSuccessRate() * 100 : 0.0);
            dto.setLastCheck(node.getLastCheck());
            dto.setFailCount(node.getFailCount());
            dto.setTotalRequests(node.getTotalRequests());
            dto.setSuccessRequests(node.getSuccessRequests());
            dto.setUpdatedAt(node.getUpdatedAt());
        });

        return dto;
    }

    /**
     * 添加操作日志
     *
     * @param serverId 服务器ID
     * @param action   操作类型
     * @param message  日志消息
     * @param oldValue 旧值
     * @param newValue 新值
     */
    private void addLog(Long serverId, String action, String message, String oldValue, String newValue) {
        TdxServerLog logEntry = TdxServerLog.builder()
                .serverId(serverId)
                .action(action)
                .message(message)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        logRepository.save(logEntry);
    }

    /**
     * 将DTO转换为JSON字符串
     *
     * @param obj 对象
     * @return JSON字符串
     */
    private String dtoToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
