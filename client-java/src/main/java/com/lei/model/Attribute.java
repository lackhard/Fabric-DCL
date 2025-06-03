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
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel("属性实体类")
public class Attribute {
    /**
     * 私有属性id格式
     */
    @ApiModelProperty("属性id")
    private String id;

    @ApiModelProperty("属性类型")
    private String type;

    /**
     * 针对那一个资源
     */
    @ApiModelProperty("资源id")
    private String resourceId;

    /**
     * 属性发布者
     */
    private String ownerId;

    private double money;

    private String key;
    private String value;
    /**
     * 生效时间
     */
    @ApiModelProperty("生效时间")
    private String notBefore;
    /**
     * 失效时间
     */
    @ApiModelProperty("失效时间")
    private String notAfter;


}
