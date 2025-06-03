package com.lei.controller.request;


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
public class PrivateAttributeRequest {
    private String sellerId;

    private String resourceId;

    private String money;

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

    /**
     * 签名
     */
    private String sig;
}
