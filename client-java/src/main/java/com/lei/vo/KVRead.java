package com.lei.vo;

import lombok.Data;

/**
 * @author lei
 * @since 2023/2/1
 * 读集
 */
@Data
public class KVRead {
    private String key;
    private String version;
}
