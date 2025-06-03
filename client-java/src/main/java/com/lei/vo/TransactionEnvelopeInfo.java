package com.lei.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author lei
 * @since 2023/2/1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEnvelopeInfo {
    /**
     * 通道的名字
     */
    private String channelId;

    /**
     * 创建者的证书
     */
    private String creatorId;

    /**
     * 创建者的msp id
     */
    private String creatorMspid;

    private String nonce;

    /**
     * 时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date timestamp;


    /**
     * 交易id
     */
    private String transactionID;

    /**
     * 交易状态
     */
    private String validation;

    /**
     * 交易动作信息
     */
    private List<TransactionActionInfo> transactionActionInfos;
}
