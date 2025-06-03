package com.lei.model;

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
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ApiModel("用户实体类")
public class User {
    @ApiModelProperty("用户id")
    private String id;
    @ApiModelProperty("用户所具有的钱")
    private double money;
    @ApiModelProperty("用户的组织")
    private String  org;
}
