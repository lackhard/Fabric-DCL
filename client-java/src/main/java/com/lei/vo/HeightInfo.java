package com.lei.vo;

import lombok.Builder;
import lombok.Data;

/**
 * @author lei
 * @since 2023/2/1
 * 区块链高度信息
 */
@Data
@Builder
public class HeightInfo {
    private Long height;
    private String currentBlockHash;
    private String previousBlockHash;
}
