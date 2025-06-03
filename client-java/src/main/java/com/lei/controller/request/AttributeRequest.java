package com.lei.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lizhi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeRequest {

    /**
     * 属性id
     */
    private String id;

    /**
     * 属性的类型， PUBLIC、PRIVATE
     */
    private String type;
    /**
     * 属性所属的资源id
     */
    private String resourceId;
    /**
     * 属性拥有者
     */
    private String ownerId;
    /**
     * 属性价格
     */
    private double money;
    /**
     * 属性的key
     */
    private String key;
    private String value;
    /**
     * 生效时间
     */
    private String notBefore;
    /**
     * 失效时间
     */
    private String notAfter;

}
