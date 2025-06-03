package com.lei.controller.request;

import lombok.*;

/**
 * @author lei
 * @since 2023-03-03
 */

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddResourceControllerRequest {
    private String resourceId;
    private String controllerId;
}
