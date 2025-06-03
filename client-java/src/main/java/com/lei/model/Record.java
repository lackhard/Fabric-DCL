package com.lei.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 访问记录实体类
 * @author lei
 * @since 2023-05-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {
    /**
     * 访问记录id
     */
    private String Id;

    /**
     * 访问 请求者id
     */
    private String requesterId;

    /**
     * 访问资源的id
     */
    private String resourceId;

    /**
     * 合约决策的响应
     */
    private String response;
}
