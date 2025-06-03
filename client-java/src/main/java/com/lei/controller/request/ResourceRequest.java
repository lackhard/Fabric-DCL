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
public class ResourceRequest {
    /**
     * 资源id
     */
    private String id;
    /**
     * 资源拥有者
     */
    private String owner;
    /**
     * 资源路径
     */
    private String url;
    /**
     * 资源描述
     */
    private String description;
}
