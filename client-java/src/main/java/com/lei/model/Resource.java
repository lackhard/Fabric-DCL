package com.lei.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lizhi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel("资源实体类")
public class Resource {
    @ApiModelProperty("资源的id")
    private String id;

    @ApiModelProperty("资源的拥有者")
    private String owner;

    @ApiModelProperty("资源的路径")
    private String url;

    @ApiModelProperty("资源描述信息")
    private String description;

    @ApiModelProperty("资源控制器")
    private String[] controllers;
}
