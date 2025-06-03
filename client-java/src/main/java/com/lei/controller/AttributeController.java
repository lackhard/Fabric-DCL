package com.lei.controller;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.lei.controller.request.AttributeRequest;
import com.lei.controller.request.BuyPrivateAttributeRequest;
import com.lei.enums.AttributeTypeEnum;
import com.lei.model.Attribute;
import com.lei.util.JsonData;
import com.lei.util.JsonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.peer.ProposalPackage;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric.sdk.transaction.ProposalBuilder;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author lizhi
 */
@RestController
@Slf4j
@Api(tags = "属性操作相关接口")
@RequestMapping("/api/attribute/v1")
public class AttributeController {

    @Autowired
    private Contract contract;

    @Autowired
    private Network network;

    @Autowired
    private Channel channel;

    @Autowired
    private Gateway gateway;

    @Autowired
    private HFClient hfClient;

    // 经过客户端验证的 peer 的背书响应
    Collection<ProposalResponse> validProposalResponses2;
    public static Method sendProposalAsyncMethod = null;

    public static Method sendTransactionMethod = null;
    // 反射获取方法
   static   {
       //发送给peer的方法
       Class<Peer> peerClass = Peer.class;
       try {
           sendProposalAsyncMethod = peerClass.getDeclaredMethod("sendProposalAsync", ProposalPackage.SignedProposal.class);
       } catch (NoSuchMethodException e) {
           throw new RuntimeException(e);
       }
       sendProposalAsyncMethod.setAccessible(true);

        // 发送给orderer
        Class<Orderer> ordererClass = Orderer.class;
        try {
            sendTransactionMethod = ordererClass.getDeclaredMethod("sendTransaction", Common.Envelope.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        sendTransactionMethod.setAccessible(true);
    }
    @GetMapping("/attributes")
    @ApiOperation("根据用户id查找该用户的所有属性")
    public JsonData attributes(@RequestParam("user_id") String userId) throws ContractException {
        byte[] attributes = contract.evaluateTransaction("FindAttributeByUserId", userId);
        if (attributes == null || attributes.length == 0) {
            return JsonData.buildError("属性为null 或者长度为0");
        }

        return JsonData.buildSuccess(JsonUtil.bytes2Obj(attributes, Attribute[].class));
    }

    @PostMapping("/addAttribute")
    @ApiOperation("增加公有属性")
    public JsonData  addAttribute(@RequestBody AttributeRequest request) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("AddAttribute")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));

        Attribute attribute = Attribute.builder()
                .id("attribute:" + UUID.randomUUID())
                .type(AttributeTypeEnum.PUBLIC.name())
                .ownerId(request.getOwnerId())
                .key(request.getKey())
                .value(request.getValue())
                .notBefore(request.getNotBefore())
                .notAfter(request.getNotAfter())
                .build();

        byte[] invokeResult = transaction.submit(JsonUtil.obj2Json(attribute));
        log.info("调用结果:" +  new String(invokeResult));
        String transactionId = transaction.getTransactionId();
        Map<String, String > res = new HashMap(2);
        res.put("txId", transactionId);
        res.put("data", JsonUtil.obj2Json(invokeResult));
        return JsonData.buildSuccess(res);
    }


    // 尝试只进行背书，不提交到orderer节点
    @PostMapping("/addAttributeOnlyPeer")
    @ApiOperation("增加属性(只进行背书)")
    public JsonData  addAttributeOnlyPeer(@RequestBody AttributeRequest request) throws ContractException, InterruptedException, TimeoutException, InvalidArgumentException, ProposalException, IOException {

        TransactionProposalRequest transactionProposalRequest = network.getGateway().getClient().newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeName("abac");
        transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
        transactionProposalRequest.setFcn("AddAttribute");
        Attribute attribute = Attribute.builder()
                .id("attribute:" + UUID.randomUUID())
                .type(AttributeTypeEnum.PUBLIC.name())
                .ownerId(request.getOwnerId())
                .key(request.getKey())
                .value(request.getValue())
                .notBefore(request.getNotBefore())
                .notAfter(request.getNotAfter())
                .build();

        transactionProposalRequest.setArgs(JsonUtil.obj2Json(attribute));
        Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(transactionProposalRequest,
                network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));

        // 自己验证一下
        validProposalResponses2 = validatePeerResponses(proposalResponses);

        Map<String, String > res = new HashMap(2);
        res.put("txId", proposalResponses.toString());
        return JsonData.buildSuccess(res);
    }




    // 只提交到orderer节点
    @PostMapping("/addAttributeOnlyOrder")
    @ApiOperation("增加属性(只发送到orderer节点)")
    public JsonData  addAttributeOnlyOrder() throws ContractException, InterruptedException, TimeoutException, InvalidArgumentException, ProposalException, IOException, ClassNotFoundException, NoSuchMethodException {

        ProposalResponse proposalResponse = validProposalResponses2.iterator().next();
        for (int i = 0; i < 10000; i++) {
            Channel.TransactionOptions transactionOptions = Channel.TransactionOptions.createTransactionOptions()
                    .nOfEvents(Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour
            channel.sendTransaction(validProposalResponses2, transactionOptions);
        }

//        Channel.TransactionOptions transactionOptions = Channel.TransactionOptions.createTransactionOptions()
//                .nOfEvents(Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour
//        channel.sendTransaction(validProposalResponses, transactionOptions);

//        GatewayImpl gatewayImpl = (GatewayImpl)gateway;
//        CommitHandlerFactory commitHandlerFactory = gatewayImpl.getCommitHandlerFactory();
//        CommitHandler commitHandler = commitHandlerFactory.create(proposalResponse.getTransactionID(), network);
//        commitHandler.startListening();
//
//        try {
//            Channel.TransactionOptions transactionOptions = Channel.TransactionOptions.createTransactionOptions()
//                    .nOfEvents(Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour
//            channel.sendTransaction(validProposalResponses, transactionOptions)
//                    .get(5, TimeUnit.MINUTES);
//        } catch (TimeoutException e) {
//            commitHandler.cancelListening();
//            throw e;
//        } catch (Exception e) {
//            commitHandler.cancelListening();
//            throw new ContractException("Failed to send transaction to the orderer", e);
//        }
//
//        commitHandler.waitForEvents(5, TimeUnit.MINUTES);

        Map<String, String > res = new HashMap(2);
        //res.put("txId", proposalResponse.getTransactionID());
        return JsonData.buildSuccess(res);
    }

    @DeleteMapping("/clear")
    @ApiOperation("清空该用户公有属性")
    public JsonData clearPublicAttribute() {

        return null;
    }
    /**
     * 发布私有属性
     * @param request
     * @return
     * @throws ContractException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @PostMapping("/publish")
    @ApiOperation("发布私有属性")
    public JsonData publish(@RequestBody AttributeRequest request) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("PublishPrivateAttribute")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));

        request.setId("attribute:" + request.getResourceId() + ":" + UUID.randomUUID());
        byte[] result = transaction.submit(JsonUtil.obj2Json(request));

        Map<String, Object> map = new HashMap<>(2);
        map.put("txId", transaction.getTransactionId());
        // 里面应该是 属性id
        map.put("data", new String(result));
        return JsonData.buildSuccess(map);
    }

    /**
     * 根据资源id查找属性
     * @param resourceId
     * @return
     * @throws ContractException
     */
    @GetMapping("/findByResourceId")
    @ApiOperation("根据资源id查找属性")
    public JsonData find(@RequestParam("resourceId")String resourceId) throws ContractException {
        byte[] attributes = contract.evaluateTransaction("FindAttributeByResourceId", resourceId);

        return JsonData.buildSuccess(JsonUtil.bytes2Obj(attributes, Attribute[].class));
    }

    /**
     * 购买私有属性
     * @param request
     * @return
     * @throws ContractException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @PostMapping("/buy")
    @ApiOperation("购买私有属性")
    public JsonData buy(@RequestBody BuyPrivateAttributeRequest request) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("BuyPrivateAttribute")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));

        byte[] result = transaction.submit(JsonUtil.obj2Json(request));

        Map<String, Object> map = new HashMap<>(2);
        map.put("txId", transaction.getTransactionId());
        map.put("data", new String(result));
        return JsonData.buildSuccess(map);
    }


    @GetMapping("/FindAttributeById")
    @ApiOperation("根据属性id查询属性")
    public JsonData findAttributeById(@RequestParam("attributeId") String attributeId) throws ContractException {
        Transaction transaction = contract.createTransaction("FindAttributeById")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        byte[] attribute = transaction.evaluate(attributeId);
        Map<String, Object> map = new HashMap<>(1);
        if (attribute.length != 0) {
            map.put("data", JsonUtil.bytes2Obj(attribute, Attribute.class));
        }else {
            map.put("data", 0);
        }
        return JsonData.buildSuccess(map);
    }
    @PostMapping("/PutAttribute")
    @ApiOperation("更改属性")
    public JsonData putAttribute(@RequestBody AttributeRequest request) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("PutAttribute")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        String json = JsonUtil.obj2Json(request);
        byte[] res = transaction.submit(json);
        return JsonData.buildSuccess(new String(res));
    }
    @GetMapping("/AddAttributeValue")
    @ApiOperation("属性加值-官方使用")
    public JsonData AddAttributeValue(@RequestParam("attributeId") String attributeId, @RequestParam("value") String value) throws ContractException, InterruptedException, TimeoutException {
        try {
            Transaction transaction = contract.createTransaction("AddAttributeValue")
                    .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
            byte[] res = transaction.submit(attributeId,value);
        }catch (Exception e) {
            return JsonData.buildCodeAndMsg(501,"冲突");
        }
        return JsonData.buildSuccess();
    }

    @GetMapping("/AddAttributeValue2")
    @ApiOperation("属性加值2-废弃不要使用")
    public JsonData AddAttributeValue2(@RequestParam("attributeId") String attributeId, @RequestParam("value") String value) throws ContractException, InterruptedException, TimeoutException, InvalidArgumentException, ProposalException, ServiceDiscoveryException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ExecutionException, CryptoException, ClassNotFoundException, InstantiationException, InvalidProtocolBufferException {

        User userContext = hfClient.getUserContext();
        TransactionProposalRequest transactionProposalRequest = TransactionProposalRequest.newInstance(userContext);
        transactionProposalRequest.setChaincodeName("abac");
        transactionProposalRequest.setFcn("AddAttributeValue");
        transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
        transactionProposalRequest.setArgs(attributeId,value);

        Optional<TransactionContext> transactionContextOption = transactionProposalRequest.getTransactionContext();
        TransactionContext transactionContext;
        if (!transactionContextOption.isPresent()) {
            //System.out.println("fuck");
            transactionContext = new TransactionContext(channel,userContext, CryptoSuiteFactory.getDefault().getCryptoSuite());
        }else {
            transactionContext = transactionContextOption.get();
        }

        ProposalBuilder proposalBuilder = ProposalBuilder.newBuilder();

        proposalBuilder.context(transactionContext);
        proposalBuilder.request(transactionProposalRequest);
        ProposalPackage.Proposal proposal = proposalBuilder.build();

        // 签名 之后的proposal
        ProposalPackage.SignedProposal sp;
        sp = ProposalPackage.SignedProposal.newBuilder()
                .setProposalBytes(proposal.toByteString())
                .setSignature(transactionContext.signByteString(proposal.toByteArray()))
                .build();
        Collection<Peer> peers = channel.getPeers();
        List<CompletableFuture<ProposalResponsePackage.ProposalResponse>> futureList = new ArrayList<>(peers.size());
        for (Peer peer : peers) {
            Object responseObject = AttributeController.sendProposalAsyncMethod.invoke(peer, sp);
            CompletableFuture<ProposalResponsePackage.ProposalResponse> future = (CompletableFuture<ProposalResponsePackage.ProposalResponse>) responseObject;
            futureList.add(future);
        }
        List<ProposalResponse> peerResponses = new ArrayList<>(futureList.size());
        for (CompletableFuture<ProposalResponsePackage.ProposalResponse> future : futureList) {
            ProposalResponsePackage.ProposalResponse response = future.get();
            int status = response.getResponse().getStatus();
//            if (status == 501 || status == 502) {
//                // 缓存冲突
//                return JsonData.buildCodeAndMsg(status,response.getResponse().getMessage());
//            }
            System.out.println(response.getPayload());
            //peerResponses.add();
        }

//        //验证 response
//        Collection<ProposalResponse> proposalResponses = validatePeerResponses(peerResponses);
//        //发送给orderer
//
//
//        Channel.TransactionOptions transactionOptions = Channel.TransactionOptions.createTransactionOptions()
//                .nOfEvents(Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour
//        channel.sendTransaction(validProposalResponses2, transactionOptions);
        return JsonData.buildSuccess();
    }

    /**
     * 验证peer节点返回的背书提案是否有效
     * @param proposalResponses
     * @return
     * @throws ContractException
     */
    private Collection<ProposalResponse> validatePeerResponses(Collection<ProposalResponse> proposalResponses) throws ContractException {
        final Collection<ProposalResponse> validResponses = new ArrayList<>();
        final Collection<String> invalidResponseMsgs = new ArrayList<>();
        proposalResponses.forEach(response -> {
            String peerUrl = response.getPeer() != null ? response.getPeer().getUrl() : "<unknown>";
            if (response.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
                log.debug(String.format("validatePeerResponses: valid response from peer %s", peerUrl));
                validResponses.add(response);
            } else {
                log.warn(String.format("validatePeerResponses: invalid response from peer %s, message %s", peerUrl, response.getMessage()));
                invalidResponseMsgs.add(response.getMessage());
            }
        });

        if (validResponses.isEmpty()) {
            String msg = String.format("No valid proposal responses received. %d peer error responses: %s",
                    invalidResponseMsgs.size(), String.join("; ", invalidResponseMsgs));
            log.error(msg);
            throw new ContractException(msg, proposalResponses);
        }
        return validResponses;
    }


    @GetMapping("/AddAttributeValue3")
    @ApiOperation("属性加值3-DCL使用")
    public JsonData AddAttributeValue3(@RequestParam("attributeId") String attributeId, @RequestParam("value") String value) throws ContractException, InterruptedException, TimeoutException, InvalidArgumentException, ProposalException, ServiceDiscoveryException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ExecutionException, CryptoException, ClassNotFoundException, InstantiationException, InvalidProtocolBufferException {

        String transactionId = null;
        TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeName("abac");
        transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
        transactionProposalRequest.setFcn("AddAttributeValue");
        transactionProposalRequest.setArgs(attributeId,value);
        Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(transactionProposalRequest,
                network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        for (ProposalResponse response : proposalResponses) {
            ProposalResponsePackage.ProposalResponse proposalResponse = response.getProposalResponse();
            int code = proposalResponse.getResponse().getStatus();
            if (proposalResponse.getResponse().getStatus() == 501 || proposalResponse.getResponse().getStatus() == 502) {
                // 冲突
                //取冲突时间 System.out.println("payload" + response.getProposalResponse().getPayload().toStringUtf8());
                return JsonData.buildCodeAndMsg(code,proposalResponse.getResponse().getMessage());
            }
            transactionId = response.getTransactionID();
        }
        Collection<ProposalResponse> validPeerResponse = null;
        try {
            validPeerResponse  = validatePeerResponses(proposalResponses);
        }catch (Exception e) {
            return JsonData.buildCodeAndMsg(503,e.getMessage());
        }

        //发送给orderer
        GatewayImpl gatewayImpl = (GatewayImpl)gateway;
        CommitHandlerFactory commitHandlerFactory = gatewayImpl.getCommitHandlerFactory();
        CommitHandler commitHandler = commitHandlerFactory.create(transactionId, network);
        commitHandler.startListening();
        try {
            Channel.TransactionOptions transactionOptions = Channel.TransactionOptions.createTransactionOptions()
                    .nOfEvents(Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour
            channel.sendTransaction(validPeerResponse, transactionOptions)
                    .get(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            commitHandler.cancelListening();
            throw e;
        } catch (Exception e) {
            commitHandler.cancelListening();
            throw new ContractException("Failed to send transaction to the orderer", e);
        }

        commitHandler.waitForEvents(5, TimeUnit.MINUTES);
        return JsonData.buildSuccess();
    }
}
