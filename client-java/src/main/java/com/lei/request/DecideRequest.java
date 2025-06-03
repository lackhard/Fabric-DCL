package com.lei.request;

import lombok.*;

/**
 * @author lizhi
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DecideRequest {
    /**
     * 请求id
     */
    private String id;

    /**
     * 请求者id
     */
    private String requesterId;

    /**
     * 请求资源的id
     */
    private String resourceId;


}
