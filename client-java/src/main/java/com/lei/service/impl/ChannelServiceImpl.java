package com.lei.service.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.lei.config.GatewayConfig;
import com.lei.service.ChannelService;
import com.lei.vo.KVRead;
import com.lei.vo.KVWrite;
import com.lei.vo.TransactionActionInfo;
import com.lei.vo.TransactionEnvelopeInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.protos.peer.TransactionPackage;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lei
 * @since 2023/2/1
 */
@Slf4j
@Service
public class ChannelServiceImpl implements ChannelService {
    @Autowired
    private Channel channel;

    @Autowired
    private GatewayConfig gatewayConfig;

    @Override
    public com.lei.vo.BlockInfo queryBlockByHash(String hash) throws DecoderException, InvalidArgumentException, ProposalException, InvalidProtocolBufferException {
        BlockInfo blockInfo = channel.queryBlockByHash(Hex.decodeHex(hash));

        return convert(blockInfo);
    }

    @Override
    public com.lei.vo.BlockInfo queryBlockByNumber(Long blkNumber) throws InvalidArgumentException, ProposalException, InvalidProtocolBufferException {
        BlockInfo blockInfo = channel.queryBlockByNumber(blkNumber);
        return convert(blockInfo);
    }

    private com.lei.vo.BlockInfo convert(BlockInfo blockInfo) throws InvalidProtocolBufferException {
        if (blockInfo == null) {
            return null;
        }

        com.lei.vo.BlockInfo info = new com.lei.vo.BlockInfo();
        String channelId = info.getChannelId();
        info.setChannelId(blockInfo.getChannelId());
        info.setBlockNumber(blockInfo.getBlockNumber());
        info.setDataHash(Hex.encodeHexString(blockInfo.getDataHash()));
        info.setPreviousHash(Hex.encodeHexString(blockInfo.getPreviousHash()));
        info.setType(blockInfo.getType().name());

        info.setTransactionCount(blockInfo.getTransactionCount());
        info.setEnvelopeCount(blockInfo.getEnvelopeCount());



        Iterable<BlockInfo.EnvelopeInfo> envelopeInfoIterable = blockInfo.getEnvelopeInfos();
        List<TransactionEnvelopeInfo> transactions = new ArrayList<>();
        for (BlockInfo.EnvelopeInfo envelopeInfo : envelopeInfoIterable) {
            String envelopeInfoChannelId = envelopeInfo.getChannelId();
            String type = envelopeInfo.getType().name();
            BlockInfo.EnvelopeInfo.IdentitiesInfo creator = envelopeInfo.getCreator();
            String creatorId = creator.getId();
            String creatorMspid = creator.getMspid();
            String nonce = Hex.encodeHexString(envelopeInfo.getNonce());
            Date timestamp = envelopeInfo.getTimestamp();
            String transactionID = envelopeInfo.getTransactionID();
            byte validationCode = envelopeInfo.getValidationCode();
            String validation = TransactionPackage.TxValidationCode.forNumber(validationCode).name();
            //log.info("envelopeInfoChannelId={},type={},creatorId={},creatorMspid={},nonce={},timestamp={},transactionID={},validation={}",
            //        envelopeInfoChannelId, type,creatorId,creatorMspid, nonce, timestamp, transactionID, validation);


            // 强转类型
            BlockInfo.TransactionEnvelopeInfo transactionEnvelopeInfo = (BlockInfo.TransactionEnvelopeInfo) envelopeInfo;
            List<TransactionActionInfo> transactionActionInfos = new ArrayList<>();
            // 获取操作事务的信息
            for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo : transactionEnvelopeInfo.getTransactionActionInfos()) {

                // 链码名称
                String chaincodeIDName = transactionActionInfo.getChaincodeIDName();
                // 链码版本
                String chaincodeIDVersion = transactionActionInfo.getChaincodeIDVersion();
                //log.info("proposal chaincodeIDName:{}, chaincodeIDVersion: {}", chaincodeIDName, chaincodeIDVersion);

                //背书者数量
                int endorsementsCount = transactionActionInfo.getEndorsementsCount();
                for (int i = 0; i < endorsementsCount; i++) {
                    BlockInfo.EndorserInfo endorsementInfo = transactionActionInfo.getEndorsementInfo(i);
                    String id = endorsementInfo.getId();
                    String mspid = endorsementInfo.getMspid();
                    String signature = Hex.encodeHexString(endorsementInfo.getSignature());
                    log.debug("id:{}, mspid:{}, signature:{}",id,mspid, signature);
                }
                int responseStatus = transactionActionInfo.getResponseStatus();
                String responseMessage = transactionActionInfo.getResponseMessage();
                log.debug("responseStatus={},responseMessage={}",responseStatus, responseMessage);

                int proposalResponseStatus = transactionActionInfo.getProposalResponseStatus();
                String proposalResponsePayload = new String(transactionActionInfo.getProposalResponsePayload(), StandardCharsets.UTF_8);
                log.debug("proposalResponseStatus={},proposalResponsePayload={}",proposalResponseStatus, proposalResponsePayload);
                //链码输入参数
                int chaincodeInputArgsCount = transactionActionInfo.getChaincodeInputArgsCount();
                for (int i = 0; i < chaincodeInputArgsCount; i++) {
                    byte[] chaincodeInputArgs = transactionActionInfo.getChaincodeInputArgs(i);
                    String inputArg = new String(chaincodeInputArgs, StandardCharsets.UTF_8);
                    log.debug("inputArg:{}",inputArg);
                }

                // 操作事务的读写集
                TxReadWriteSetInfo rwsetInfo = transactionActionInfo.getTxReadWriteSet();
                List<KVWrite> kvWriteList = new ArrayList<>();
                List<KVRead> KVReadList = new ArrayList<>();
                if (null != rwsetInfo) {
                    for (TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo : rwsetInfo.getNsRwsetInfos()) {
                        // 含有默认链码 _lifecycle  和 自定义链码
                        String namespace = nsRwsetInfo.getNamespace();
                        // 只要符合要求的链码的
                        if (!namespace.equals(gatewayConfig.getContractName())) {
                            //log.error("链码名称不正确 跳过 ,namespace ={}", namespace);
                            continue;
                        }
                        KvRwset.KVRWSet rws = nsRwsetInfo.getRwset();
                        //写集合的数据
                        for (KvRwset.KVWrite write : rws.getWritesList()) {
                            //String valAsString = printableString(new String(writeList.getValue().toByteArray(), Charset.forName("UTF-8")));
                            String key = write.getKey();
                            //log.info("Namespace {}  key {} has value '{}' ", namespace, writeList.getKey(), valAsString);
                            String value = new String(write.getValue().toByteArray(), StandardCharsets.UTF_8);
                            //if (StringUtils.isNotBlank(valAsString)) {
                            //   value = JSON.parseObject(valAsString, Map.class);
                            //}
                            KVWrite kvWrite = new KVWrite();
                            kvWrite.setKey(key);
                            kvWrite.setValue(value);
                            kvWriteList.add(kvWrite);
                        }
                        //读集合
                        for (KvRwset.KVRead kvRead : rws.getReadsList()) {
                            String key = kvRead.getKey();
                            //String version = new String(kvRead.getVersion().toByteArray(), StandardCharsets.UTF_8);
                            String version = kvRead.getVersion().getBlockNum() + ":" + kvRead.getVersion().getTxNum();
                            KVRead kvRead1 = new KVRead();
                            kvRead1.setKey(key);
                            kvRead1.setVersion(version);
                            KVReadList.add(kvRead1);
                        }
                    }
                }
                TransactionActionInfo actionInfo = TransactionActionInfo.builder().
                        chaincodeIDName(chaincodeIDName)
                        .chaincodeIDVersion(chaincodeIDVersion)
                        .writeList(kvWriteList)
                        .readList(KVReadList)
                        .build();
                transactionActionInfos.add(actionInfo);
            }
            TransactionEnvelopeInfo envelopeInfo1 = TransactionEnvelopeInfo.builder().channelId(channelId).creatorId(creatorId).creatorMspid(creatorMspid)
                    .nonce(nonce).timestamp(timestamp).transactionID(transactionID).validation(validation)
                    .transactionActionInfos(transactionActionInfos).build();
            transactions.add(envelopeInfo1);

        }

        info.setTransactions(transactions);
        return info;
    }


}
