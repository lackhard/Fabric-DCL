package com.lei.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lei
 * @since 2023/2/1
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockInfo {
    /**
     * 通道的名字
     */
    private String channelId;
    /**
     * 区块编号
     */
    private long blockNumber;

    /**
     * 区块中数据的hash
     */
    private String dataHash;

    /**
     * 前一个区块的hash
     */
    private String previousHash;

    /**
     * 区块类型
     */
    private String type;

    /**
     * 区块中包含交易的数量
     */
    private int transactionCount;

    /**
     * 这个东西 好像是 = transactionCount
     */
    private int envelopeCount;

    /**
     * 交易信息
     */
    private List<TransactionEnvelopeInfo> transactions;
}
