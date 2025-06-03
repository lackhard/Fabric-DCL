package com.lei.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.lei.util.JsonData;
import com.lei.vo.BlockInfo;
import org.apache.commons.codec.DecoderException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

/**
 * @author lizhi
 */
public interface ChannelService {
    BlockInfo queryBlockByHash(String hash) throws DecoderException, InvalidArgumentException, ProposalException, InvalidProtocolBufferException;
    BlockInfo queryBlockByNumber(Long blkNumber) throws  InvalidArgumentException, ProposalException, InvalidProtocolBufferException;
}
