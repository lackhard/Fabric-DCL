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
public class BuyPrivateAttributeRequest {
    /**
     * 属性购买人
     */
    private String buyer;
    /**
     * 属性出售者
     */
    private String seller;
    /**
     * 属性id
     */
    private String attributeId;
}
