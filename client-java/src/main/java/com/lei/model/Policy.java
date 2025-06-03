package com.lei.model;

import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.annotation.JSONType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lizhi
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel("策略实体类")
public class Policy {
    @ApiModelProperty("策略id")
    private String id;

    @ApiModelProperty("资源id")
    @JsonProperty("resource_id")
    @JSONField(name = "resource_id")
    private String resourceId;

    @ApiModelProperty("请求者需要具备的属性key")
    @JsonProperty("requesterAttributeKeys")
    @JSONField(name = "requesterAttributeKeys")
    private List<String> requesterAttributeKeys;

    @ApiModelProperty("请求者需要具备的属性value")
    @JsonProperty("requester_attribute_values")
    @JSONField(name = "requester_attribute_values")
    private List<String> requesterAttributeValues;

    @ApiModelProperty("资源需要具备的属性key")
    @JsonProperty("resourceAttributeKeys")
    @JSONField(name = "resourceAttributeKeys")
    private List<String> resourceAttributeKeys;

    @ApiModelProperty("资源需要具备的属性value")
    @JsonProperty("resource_attribute_values")
    @JSONField(name = "resource_attribute_values")
    private List<String> resourceAttributeValues;

    @ApiModelProperty("访问者需要具备的私有属性key")
    @JsonProperty("private_keys")
    @JSONField(name = "private_keys")
    private List<String> privateKeys;

    @ApiModelProperty("访问者需要具备的私有属性value")
    @JsonProperty("private_values")
    @JSONField(name = "private_values")
    private List<String> privateValues;
}
