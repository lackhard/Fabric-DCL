package com.lei.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.lei.config.GatewayConfig;
import com.lei.service.ChannelService;
import com.lei.service.impl.ChannelServiceImpl;
import com.lei.util.JsonData;
import com.lei.util.JsonUtil;
import com.lei.vo.HeightInfo;
import com.lei.vo.KVWrite;
import com.lei.vo.TransactionActionInfo;
import com.lei.vo.TransactionEnvelopeInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.HexDump;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.protos.peer.TransactionPackage;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@Slf4j
@RequestMapping("/api/v1/channel")
@Api(tags = "与通道有关的操作")
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @Autowired
    private Channel channel;


    /**
     * 根据交易id查询区块
     * @param txId
     * @return
     * @throws InvalidArgumentException
     * @throws ProposalException
     */
    @GetMapping("/queryBlockByTransactionID")
    @ApiOperation("通过交易id查询区块")
    public JsonData queryBlockByTransactionID(String txId) throws InvalidArgumentException, ProposalException {
        BlockInfo blockInfo = channel.queryBlockByTransactionID(txId);
        return JsonData.buildSuccess(JSON.toJSONString(blockInfo));
    }

    @ApiOperation("获取peer节点")
    @GetMapping("/peers")
    public JsonData getPeers() {
        Collection<Peer> peers = channel.getPeers();
        log.info("peers: {}", peers);
        String peersJson = JsonUtil.obj2Json(peers);
        return JsonData.buildSuccess(JsonUtil.json2List(peersJson, Map.class));
    }

    /**
     * 获取通道的名字
     * @return
     */
    @ApiOperation("获取通道的name")
    @GetMapping("/name")
    public JsonData getName() {
        String name = channel.getName();
        return JsonData.buildSuccess(name);
    }

    @ApiOperation("根据id查询交易")
    @GetMapping("/queryTransactionByID")
    public JsonData queryTransactionByID(String txId) throws InvalidArgumentException, ProposalException {
        TransactionInfo transactionInfo = channel.queryTransactionByID(txId);

        return JsonData.buildSuccess();
        //log.info("transactionInfo: {}", JSON.toJSONString(transactionInfo));
        ////transactionInfo.getValidationCode()
        //String jsonString = JSON.toJSONString(transactionInfo);
        //return JsonData.buildSuccess(JsonUtil.json2Obj(jsonString, Map.class));
    }

    /**
     * 获取所有链码的名字
     * @return
     */
    @ApiOperation("获取所有链码的名字")
    @GetMapping("/getChainCodeNames")
    public JsonData getChainCodeNames() {
        Collection<String> names = channel.getDiscoveredChaincodeNames();
        return JsonData.buildSuccess(names);
    }

    @GetMapping("/instantiatedChaincodes")
    @ApiOperation("查询已经实例化的链码")
    public JsonData instantiatedChaincodes() throws InvalidArgumentException, ProposalException {
        Collection<Peer> org1MSP = channel.getPeersForOrganization("Org1MSP");
        List<Query.ChaincodeInfo> chaincodeInfos = channel.queryInstantiatedChaincodes(org1MSP.iterator().next());
        log.info("已实例化的链码: {}", chaincodeInfos);
        return JsonData.buildSuccess(JsonUtil.obj2Json(chaincodeInfos));
    }

    @GetMapping("/getServiceDiscoveryProperties")
    @ApiOperation("获取服务发现的properties")
    public JsonData getServiceDiscoveryProperties() {
        Properties serviceDiscoveryProperties = channel.getServiceDiscoveryProperties();
        return JsonData.buildSuccess(serviceDiscoveryProperties);
    }


    @GetMapping("/height")
    @ApiOperation("获取区块高度")
    public JsonData getHeight() throws InvalidArgumentException, ProposalException {
        BlockchainInfo blockchainInfo = channel.queryBlockchainInfo();
        long height = blockchainInfo.getHeight();

        String currentBlockHash = Hex.encodeHexString(blockchainInfo.getCurrentBlockHash());
        String previousBlockHash = Hex.encodeHexString(blockchainInfo.getPreviousBlockHash());
        HeightInfo heightInfo = HeightInfo.builder().height(height).currentBlockHash(currentBlockHash).previousBlockHash(previousBlockHash).build();
        return JsonData.buildSuccess(heightInfo);
    }

    @GetMapping("/queryBlockByHash")
    @ApiOperation("根据hash查询区块")
    public JsonData queryBlockByHash(String hash) throws DecoderException, InvalidArgumentException, ProposalException, InvalidProtocolBufferException {
        com.lei.vo.BlockInfo blockInfo = channelService.queryBlockByHash(hash);
        return JsonData.buildSuccess(blockInfo);
    }

    @GetMapping("/getAllOrder")
    @ApiOperation("获取order节点")
    public JsonData getAllOrder() {
        Collection<Orderer> orderers = channel.getOrderers();
        return JsonData.buildSuccess(orderers);
    }

    @GetMapping("/getChannelConfig")
    @ApiOperation("获取通道配置")
    public JsonData getChannelConfig() throws InvalidArgumentException, TransactionException {
        byte[] channelConfigurationBytes = channel.getChannelConfigurationBytes();
        return JsonData.buildSuccess(new String(channelConfigurationBytes, StandardCharsets.UTF_8));
    }

    @GetMapping("/getBlockByNumber")
    @ApiOperation("根据区块编码获取区块")
    public JsonData getBlockByNumber(@RequestParam("blockNumber") Long blockNumber) throws InvalidArgumentException, ProposalException, InvalidProtocolBufferException {
        com.lei.vo.BlockInfo blockInfo = channelService.queryBlockByNumber(blockNumber);

        return JsonData.buildSuccess(blockInfo);
    }

}
