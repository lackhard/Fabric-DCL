package com.lei.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;

import java.util.List;

/**
 * @author lei
 * @since 2023/2/1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionActionInfo {
    /**
     * 链码名称
     */
    private String chaincodeIDName;

    /**
     * 链码版本
     */
    private String chaincodeIDVersion;

    /**
     * 写集合
     */
    private List<KVWrite> writeList;

    /**
     * 读集合
     */
    private List<KVRead> readList;
}
