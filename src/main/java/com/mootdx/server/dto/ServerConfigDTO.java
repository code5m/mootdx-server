package com.mootdx.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mootdx.server.entity.ServerType;
import lombok.Data;

import javax.validation.constraints.*;

/**
 * 服务器配置数据传输对象
 * 功能：用于创建和更新服务器配置时的数据传递
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerConfigDTO {

    /** 服务器ID，更新时必填 */
    private Long id;

    /** 服务器名称，必填，最长100字符 */
    @NotBlank(message = "服务器名称不能为空")
    @Size(max = 100, message = "服务器名称不能超过100字符")
    private String name;

    /** 服务器IP地址，必填，需符合IP格式 */
    @NotBlank(message = "服务器IP不能为空")
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
            message = "IP地址格式不正确")
    private String host;

    /** 服务器端口，必填，范围1-65535 */
    @NotNull(message = "服务器端口不能为空")
    @Min(value = 1, message = "端口最小为1")
    @Max(value = 65535, message = "端口最大为65535")
    private Integer port;

    /** 服务器类型，默认为STD */
    private ServerType serverType = ServerType.STD;

    /** 服务器位置，可选 */
    @Size(max = 50, message = "位置信息不能超过50字符")
    private String location;

    /** 权重，用于负载均衡，默认100 */
    @Min(value = 1, message = "权重最小为1")
    @Max(value = 1000, message = "权重最大为1000")
    private Integer weight = 100;

    /** 连接超时时间(秒)，默认10秒 */
    @Min(value = 1, message = "超时时间最小为1秒")
    @Max(value = 60, message = "超时时间最大为60秒")
    private Integer timeout = 10;

    /** 最大重试次数，默认3次 */
    @Min(value = 0, message = "重试次数最小为0")
    @Max(value = 10, message = "重试次数最大为10")
    private Integer maxRetries = 3;

    /** 服务器描述，可选 */
    @Size(max = 500, message = "描述不能超过500字符")
    private String description;

    /** 是否启用，默认true */
    private Boolean isEnabled = true;

    /** 是否为默认服务器，默认false */
    private Boolean isDefault = false;
}
