package com.lei.controller;

import com.lei.controller.request.AttributeRequest;
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
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author lei
 * @since 2024-02-26
 */
@RestController
@Slf4j
@Api(tags = "idea2相关接口")
@RequestMapping("/api/attribute/v2")
public class IDEA2Controller {


    @Autowired
    private Channel channel;
    @Autowired
    private Network network;

    @Autowired
    private Contract contract;

    @Autowired
    private HFClient hfClient;

    @Autowired
    private Gateway gateway;

    @Autowired
            private RedissonClient redissonClient;
    Map<String,Attribute> map = new HashMap<>(100);
    List<String> keys = new ArrayList<>(100);


    @GetMapping("/noConfilctG")
    @ApiOperation("不冲突情况下官方接口")
    public JsonData noConfilctG() {
        try {
            Attribute attributeRequest = new Attribute();
            attributeRequest.setKey("money");
            String attributeID = "attribute:" + UUID.randomUUID().toString();
            attributeRequest.setId(attributeID);
            attributeRequest.setValue("10");
            Transaction transaction = contract.createTransaction("AddAttributeIDEA2")
                    .setEndorsingPeers(channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
            byte[] res = transaction.submit(JsonUtil.obj2Json(attributeRequest));
        }catch (Exception e) {
            return JsonData.buildError(e.getMessage());
        }
        return JsonData.buildSuccess();
    }
    @GetMapping("/noConfilctDCL")
    @ApiOperation("不冲突情况下-DCL")
    public JsonData noConfilctDCL() {
        try {
            Attribute attributeRequest = new Attribute();
            attributeRequest.setKey("money");
            String attributeID = "attribute:" + UUID.randomUUID().toString();
            attributeRequest.setId(attributeID);
            attributeRequest.setValue("10");
            boolean res = dclSubmit("AddAttributeIDEA2", JsonUtil.obj2Json(attributeRequest));
            if (res) {
                return JsonData.buildSuccess();
            }else {
                return JsonData.buildError("error");
            }
        }catch (Exception e) {
            return JsonData.buildError(e.getMessage());
        }
    }
    @GetMapping("/noConfilctLMQF")
    @ApiOperation("不冲突情况下-LMQF")
    public JsonData noConfilctRedis() {
        try {

            Attribute attributeRequest = new Attribute();
            attributeRequest.setKey("money");
            String attributeID = "attribute:" + UUID.randomUUID().toString();
            attributeRequest.setId(attributeID);
            attributeRequest.setValue("10");
            RLock lock = redissonClient.getLock(attributeID);
            boolean b = lock.tryLock();
            if (b) {
                Transaction transaction = contract.createTransaction("AddAttributeIDEA2")
                        .setEndorsingPeers(channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
                byte[] res = transaction.submit(JsonUtil.obj2Json(attributeRequest));
                lock.unlock();
            }else {
                lock.unlock();
                return JsonData.buildError("冲突");
            }
        }catch (Exception e) {
            return JsonData.buildError(e.getMessage());
        }

        return JsonData.buildSuccess();
    }



    @GetMapping("/attributeInit")
    @ApiOperation("属性初始化")
    public JsonData attributeInit() throws ContractException, InterruptedException, TimeoutException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) {
            executorService.submit(()->{
                Attribute attributeRequest = new Attribute();
                attributeRequest.setKey("money");
                String attributeID = "attribute:" + UUID.randomUUID().toString();
                attributeRequest.setId(attributeID);
                attributeRequest.setValue("10");
                Transaction transaction = contract.createTransaction("AddAttributeIDEA2")
                        .setEndorsingPeers(channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
                String json = JsonUtil.obj2Json(attributeRequest);
                try {
                    transaction.submit(json);
                } catch (ContractException e) {
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                map.put(attributeID,attributeRequest);
                keys.add(attributeID);
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(2,TimeUnit.MINUTES);
        return JsonData.buildSuccess();
    }

    @GetMapping("/getAllAttributeMap")
    @ApiOperation("/获取所有属性")
    public JsonData getAllAttribute()  {
        return JsonData.buildSuccess(map);
    }

    @GetMapping("/test")
    @ApiOperation("临时测试")
    public JsonData test(@RequestParam("attributeId") String attributeId ) {
        Attribute attribute = map.get(attributeId);
        // put的那个key
        Random random = new Random();
        int num = random.nextInt(100);
        try {
            if (num < 50) {
                putAttributeValueG(attribute.getId(), attribute.getKey(),String.valueOf(num));
            }else {
                AddAttributeValueByAnotherAttribute(keys.get(num),attributeId);
            }
        }catch (Exception e) {
            return JsonData.buildError(e.getMessage());
        }
        return JsonData.buildSuccess();
    }

    // 官方使用
    private byte[] putAttributeValueG(String id, String key, String value) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("PutAttributeValue")
                .setEndorsingPeers(channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        return transaction.submit(id,key,value);
    }

    private byte[] AddAttributeValueByAnotherAttribute(String id, String anotherId) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("AddAttributeValueByAnotherAttribute")
                .setEndorsingPeers(channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        return transaction.submit(id,anotherId);
    }

    // DCL使用
    private byte[] putAttributeValueDCL(String id, String key, String value) throws ContractException, InterruptedException, TimeoutException, InvalidArgumentException, ProposalException {
        String transactionId = null;
        TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeName("abac");
        transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
        transactionProposalRequest.setFcn("PutAttributeValue");
        transactionProposalRequest.setArgs(id,key,value);
        Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(transactionProposalRequest,
                network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        for (ProposalResponse response : proposalResponses) {
            ProposalResponsePackage.ProposalResponse proposalResponse = response.getProposalResponse();
            int code = proposalResponse.getResponse().getStatus();
            if (code == 501 || code == 502) {
                // 冲突
                //取冲突时间 System.out.println("payload" + response.getProposalResponse().getPayload().toStringUtf8());
                return null;
            }
            transactionId = response.getTransactionID();
        }
        Collection<ProposalResponse> validPeerResponse = null;

        validPeerResponse  = validatePeerResponses(proposalResponses);

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
        return null;
    }

    // 成功返回true 否则返回false
    private boolean dclSubmit(String contractName,String... args) throws ContractException, InterruptedException, TimeoutException, InvalidArgumentException, ProposalException {
        String transactionId = null;
        TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeName("abac");
        transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
        transactionProposalRequest.setFcn(contractName);
        transactionProposalRequest.setArgs(args);
        Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(transactionProposalRequest,
                network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        for (ProposalResponse response : proposalResponses) {
            ProposalResponsePackage.ProposalResponse proposalResponse = response.getProposalResponse();
            int code = proposalResponse.getResponse().getStatus();
            if (code == 501 || code == 502) {
                // 冲突
                //取冲突时间 System.out.println("payload" + response.getProposalResponse().getPayload().toStringUtf8());
                return false;
            }
            transactionId = response.getTransactionID();
        }
        Collection<ProposalResponse> validPeerResponse = null;

        validPeerResponse  = validatePeerResponses(proposalResponses);

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
        return true;
    }

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

}
