package com.mootdx.server.controller;

import com.mootdx.server.dto.ApiResponse;
import com.mootdx.server.dto.ServerConfigDTO;
import com.mootdx.server.dto.ServerStatisticsDTO;
import com.mootdx.server.dto.ServerStatusDTO;
import com.mootdx.server.entity.TdxServerConfig;
import com.mootdx.server.entity.TdxServerNode;
import com.mootdx.server.service.TdxServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * TDX服务器管理控制器
 * 功能：提供TDX服务器管理的REST API接口
 * 路径前缀：/api/servers
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*") // 允许跨域，前端Vue开发时使用
public class TdxServerController {

    private final TdxServerService serverService;

    /**
     * 获取服务器列表
     * GET /api/servers
     *
     * @param enabledOnly 是否只返回启用的服务器，默认false
     * @return 服务器列表
     */
    @GetMapping
    public ApiResponse<List<ServerStatusDTO>> getServers(
            @RequestParam(name = "enabled_only", required = false, defaultValue = "false") boolean enabledOnly) {
        log.info("[方法入口] TdxServerController.getServers - 获取服务器列表, enabledOnly={}", enabledOnly);
        List<ServerStatusDTO> servers = serverService.getServers(enabledOnly);
        return ApiResponse.success(servers);
    }

    /**
     * 获取单个服务器详情
     * GET /api/servers/{id}
     *
     * @param id 服务器ID
     * @return 服务器详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ServerStatusDTO> getServerById(@PathVariable Long id) {
        log.info("[方法入口] TdxServerController.getServerById - 获取服务器详情, id={}", id);
        ServerStatusDTO server = serverService.getServerById(id);
        return ApiResponse.success(server);
    }

    /**
     * 创建服务器配置
     * POST /api/servers
     *
     * @param dto 服务器配置DTO
     * @return 创建后的服务器信息
     */
    @PostMapping
    public ApiResponse<ServerStatusDTO> createServer(@Valid @RequestBody ServerConfigDTO dto) {
        log.info("[方法入口] TdxServerController.createServer - 创建服务器, name={}", dto.getName());
        ServerStatusDTO server = serverService.createServer(dto);
        return ApiResponse.success("服务器创建成功", server);
    }

    /**
     * 更新服务器配置
     * PUT /api/servers/{id}
     *
     * @param id  服务器ID
     * @param dto 服务器配置DTO
     * @return 更新后的服务器信息
     */
    @PutMapping("/{id}")
    public ApiResponse<ServerStatusDTO> updateServer(
            @PathVariable Long id,
            @Valid @RequestBody ServerConfigDTO dto) {
        log.info("[方法入口] TdxServerController.updateServer - 更新服务器, id={}", id);
        ServerStatusDTO server = serverService.updateServer(id, dto);
        return ApiResponse.success("服务器更新成功", server);
    }

    /**
     * 删除服务器配置
     * DELETE /api/servers/{id}
     *
     * @param id 服务器ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteServer(@PathVariable Long id) {
        log.info("[方法入口] TdxServerController.deleteServer - 删除服务器, id={}", id);
        serverService.deleteServer(id);
        return ApiResponse.success("服务器删除成功", null);
    }

    /**
     * 设置默认服务器
     * POST /api/servers/{id}/default
     *
     * @param id 服务器ID
     * @return 操作结果
     */
    @PostMapping("/{id}/default")
    public ApiResponse<Void> setDefaultServer(@PathVariable Long id) {
        log.info("[方法入口] TdxServerController.setDefaultServer - 设置默认服务器, id={}", id);
        serverService.setDefaultServer(id);
        return ApiResponse.success("默认服务器设置成功", null);
    }

    /**
     * 检查单个服务器状态
     * POST /api/servers/{id}/check
     *
     * @param id 服务器ID
     * @return 节点状态
     */
    @PostMapping("/{id}/check")
    public ApiResponse<TdxServerNode> checkServerStatus(@PathVariable Long id) {
        log.info("[方法入口] TdxServerController.checkServerStatus - 检查服务器状态, id={}", id);
        TdxServerNode node = serverService.checkServerStatus(id);
        return ApiResponse.success(node);
    }

    /**
     * 检查所有服务器状态
     * POST /api/servers/check-all
     *
     * @return 所有服务器状态列表
     */
    @PostMapping("/check-all")
    public ApiResponse<List<ServerStatusDTO>> checkAllServers() {
        log.info("[方法入口] TdxServerController.checkAllServers - 检查所有服务器状态");
        List<ServerStatusDTO> servers = serverService.checkAllServers();
        return ApiResponse.success(servers);
    }

    /**
     * 获取最佳服务器
     * GET /api/servers/best
     *
     * @return 最佳服务器信息
     */
    @GetMapping("/best")
    public ApiResponse<TdxServerConfig> getBestServer() {
        log.info("[方法入口] TdxServerController.getBestServer - 获取最佳服务器");
        TdxServerConfig server = serverService.getBestServer();
        if (server == null) {
            return ApiResponse.error("没有可用的服务器");
        }
        return ApiResponse.success(server);
    }

    /**
     * 获取默认服务器
     * GET /api/servers/default
     *
     * @return 默认服务器信息
     */
    @GetMapping("/default")
    public ApiResponse<TdxServerConfig> getDefaultServer() {
        log.info("[方法入口] TdxServerController.getDefaultServer - 获取默认服务器");
        TdxServerConfig server = serverService.getDefaultServer();
        if (server == null) {
            return ApiResponse.error("没有设置默认服务器");
        }
        return ApiResponse.success(server);
    }

    /**
     * 获取服务器统计信息
     * GET /api/servers/statistics
     *
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public ApiResponse<ServerStatisticsDTO> getStatistics() {
        log.info("[方法入口] TdxServerController.getStatistics - 获取统计信息");
        ServerStatisticsDTO stats = serverService.getStatistics();
        return ApiResponse.success(stats);
    }
}
