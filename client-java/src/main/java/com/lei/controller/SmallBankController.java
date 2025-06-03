package com.lei.controller;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.lei.config.ContractRetryConfig;
import com.lei.config.GatewayConfig;
import com.lei.model.RetryTask;
import com.lei.model.smallbank.Account;
import com.lei.model.smallbank.QueryArgument;
import com.lei.util.JsonData;
import io.swagger.annotations.Api;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;

import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.gateway.impl.GatewayImpl;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;

import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.protos.peer.ProposalPackage;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author lei
 * @since 2024-01-26
 */
@RestController
@Slf4j
@Api(tags = "small bank 操作相关接口")
@RequestMapping("/api/smallbank/v1")
public class SmallBankController {


    @Autowired
    private Contract contract;

    @Autowired
    private Network network;

    @Autowired
    private Channel channel;
    @Autowired
    private HFClient hfClient;

    @Autowired
    private Gateway gateway;

    @Autowired
    private GatewayConfig gatewayConfig;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static ConcurrentHashMap<String,Long> concurrentHashMap = new ConcurrentHashMap<>();

    private SmallBank smallBank = new SmallBank(0);
    {
        // 设置当前work的索引
        smallBank.setAccountSuffix(0,1);

    }
    //返回当前创建的账户数量
    @GetMapping("/getAccountNumber")
    public JsonData getAccountNumber() {

        return JsonData.buildSuccess(smallBank.getAccountsGenerated());
    }
    // 创建一批账户
    @GetMapping("/create")
    public JsonData create() throws ContractException, InterruptedException, TimeoutException {

        Account createAccountArguments = smallBank.getCreateAccountArguments();
        Integer customerId = createAccountArguments.getCustomer_id();
        String customerName = createAccountArguments.getCustomer_name();
        Integer initialCheckingBalance = createAccountArguments.getInitial_checking_balance();
        Integer initialSavingsBalance = createAccountArguments.getInitial_savings_balance();

        Transaction transaction = contract.createTransaction("create_account");
        //transaction.setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        byte[] res = transaction.submit(customerId.toString(),customerName,initialCheckingBalance.toString(),initialSavingsBalance.toString());
        return JsonData.buildSuccess(new String(res));
    }


    @GetMapping("/query")
    public JsonData query() throws ContractException, InterruptedException, TimeoutException {

        QueryArgument queryArguments = smallBank.getQueryArguments();

        Iterator<Peer> iterator = network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)).iterator();
        Peer peer = null;
        while (iterator.hasNext()) {
            Peer current = iterator.next();
            peer = current;
        }
        Transaction transaction = contract.createTransaction("query");
        transaction.setEndorsingPeers(Collections.singleton(peer));


        byte[] res = transaction.evaluate(String.valueOf(queryArguments.getCustomer_id()));
        //byte[] res = transaction.evaluate("1");
        return JsonData.buildSuccess(new String(res));
    }

    @GetMapping("/queryAccount")
    public JsonData queryAccount(@RequestParam("id")String id) throws ContractException  {

        Iterator<Peer> iterator = network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)).iterator();
        Peer peer = null;
        while (iterator.hasNext()) {
            Peer current = iterator.next();
//            if (current.getName().contains("org1")) {
//                peer = current;
//            }
            peer = current;
        }
        Transaction transaction = contract.createTransaction("query");
        transaction.setEndorsingPeers(Collections.singleton(peer));


        byte[] res = transaction.evaluate(String.valueOf(id));
        //byte[] res = transaction.evaluate("1");
        return JsonData.buildSuccess(new String(res));
    }

    @GetMapping("/modify")
    public JsonData modify()  {

        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);

        Transaction transaction = contract.createTransaction(operation);
        transaction.setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        try {
            byte[] res = transaction.submit(args);
        }catch (Exception e) {

        }
        return JsonData.buildSuccess();
    }
    @GetMapping("/modifyDCL")
    public JsonData modifyDCL() {
        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        try {
            byte[] bytes = dclSendTransaction(gatewayConfig.getContractName(), operation, channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)), args);
            //return JsonData.buildSuccess();
        } catch (Exception e) {
            //log.info(e.getMessage());
            //return JsonData.buildError(e.getMessage());
        }
        return JsonData.buildSuccess();
    }

    @GetMapping("/modifyEndorseOr")
    public JsonData modifyEndorseOr()  {
        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        // 随机选择peer
        Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
        List<Peer> EndorsePeer = new ArrayList<>(1);
        List<Peer> peerList = new ArrayList<>(peers);
        if (peerList.size() >= 2) {
            EndorsePeer.add(peerList.get((int)(Math.random()*peers.size())));
        }
        Transaction transaction = contract.createTransaction(operation);
        transaction.setEndorsingPeers(EndorsePeer);
        try {
            byte[] res = transaction.submit(args);
        }catch (Exception e) {
            log.info(e.getMessage());
        }
        return JsonData.buildSuccess();
    }
    @GetMapping("/modifyDCLEndorseOr")
    public JsonData modifyDCLEndorseOr() {
        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        try {
            Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
            List<Peer> EndorsePeer = new ArrayList<>(1);
            List<Peer> peerList = new ArrayList<>(peers);
            if (peerList.size() >= 2) {
                EndorsePeer.add(peerList.get((int)(Math.random()*peers.size())));
            }
            byte[] bytes = dclSendTransaction(gatewayConfig.getContractName(), operation, EndorsePeer, args);
            //return JsonData.buildSuccess();
        } catch (Exception e) {
            log.info(e.getMessage());
            //return JsonData.buildError(e.getMessage());
        }
        return JsonData.buildSuccess();
    }

    @GetMapping("/write_check")
    public JsonData write_check() throws ContractException, InterruptedException, TimeoutException {

        Transaction transaction = contract.createTransaction("write_check");
        transaction.setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));

        byte[] res = transaction.submit("103","990");
        return JsonData.buildSuccess(new String(res));
    }
    private Collection<ProposalResponse> validatePeerResponses(Collection<ProposalResponse> proposalResponses) throws ContractException {
        Collection<ProposalResponse> validResponses = new ArrayList<>(proposalResponses.size());
        Collection<String> invalidResponseMsgs = new ArrayList<>(proposalResponses.size());
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

    @GetMapping("/modify3orgs")
    public JsonData modify3orgs()  {

        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);

        Transaction transaction = contract.createTransaction(operation);
        try {
            byte[] res = transaction.submit(args);
        }catch (Exception e) {

        }

        return JsonData.buildSuccess();
    }
    @GetMapping("/modifyDCL3org")
    public JsonData modifyDCL3org() {
        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        try {
            Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
            List<Peer> EndorsePeer = new ArrayList<>(peers);
            // 随机移除1个 剩余两个背书节点
            EndorsePeer.remove((int)(Math.random()*peers.size()));
            byte[] bytes = dclSendTransaction(gatewayConfig.getContractName(), operation, EndorsePeer, args);
            //return JsonData.buildSuccess();
        } catch (Exception e) {
            log.error(e.getMessage());
            //return JsonData.buildError(e.getMessage());
        }
        return JsonData.buildSuccess();
    }
    @GetMapping("/modifyLMQF3org")
    public JsonData modifyRedis3org() {
        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        try {
            Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
            List<Peer> EndorsePeer = new ArrayList<>(peers);
            // 随机移除1个 剩余两个背书节点
            EndorsePeer.remove((int)(Math.random()*peers.size()));
            byte[] bytes = RedisSendTransaction(gatewayConfig.getContractName(), operation, EndorsePeer, args);

            //return JsonData.buildSuccess();
        } catch (Exception e) {
            log.info(e.getMessage());
            //return JsonData.buildError(e.getMessage());
        }
        return JsonData.buildSuccess();
    }

    @GetMapping("/modifyLMQF5org")
    public JsonData modifyRedis5org() {
        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
        List<Peer> EndorsePeer = new ArrayList<>(peers); // 选择三个，随机剔除2个
        EndorsePeer.remove((int)(Math.random()*EndorsePeer.size()));
        EndorsePeer.remove((int)(Math.random()*EndorsePeer.size()));
        try {

            byte[] bytes = RedisSendTransaction(gatewayConfig.getContractName(), operation, EndorsePeer, args);
            //return JsonData.buildSuccess();
        } catch (Exception e) {
            log.info(e.getMessage());
            //return JsonData.buildError(e.getMessage());
        }
        return JsonData.buildSuccess();
    }

    @GetMapping("/modify5orgs")
    public JsonData modify5orgs() throws ContractException, InterruptedException, TimeoutException {

        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);

        Transaction transaction = contract.createTransaction(operation);

        byte[] res = transaction.submit(args);
        return JsonData.buildSuccess();
    }
    @GetMapping("/modifyDCL5org")
    public JsonData modifyDCL5org() {
        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
        List<Peer> EndorsePeer = new ArrayList<>(peers); // 选择三个，随机剔除2个
        EndorsePeer.remove((int)(Math.random()*EndorsePeer.size()));
        EndorsePeer.remove((int)(Math.random()*EndorsePeer.size()));
        try {
            byte[] res = dclSendTransaction(gatewayConfig.getContractName(), operation, EndorsePeer, args);
            //return JsonData.buildSuccess();
        }catch (Exception e) {
            log.info(e.getMessage());
            //return JsonData.buildError("冲突返回");
        }
        return JsonData.buildSuccess();
    }


    private byte[] dclSendTransaction( String contractName, String actionName,Collection<Peer> endorsePeers,String... args) throws InvalidArgumentException, ContractException, ProposalException, TimeoutException {
        Long lockTime = concurrentHashMap.getOrDefault(args[0], 0L);
        Long currentTime = System.currentTimeMillis();
        if (currentTime - lockTime < 10000) { // 10s
            throw new RuntimeException("本地缓存冲突");
        }
        concurrentHashMap.put(args[0],currentTime);
        byte[] res = null;
        try {
            String transactionId = null;
            TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeName(contractName);
            transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            transactionProposalRequest.setFcn(actionName);
            transactionProposalRequest.setArgs(args);

            Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(transactionProposalRequest,
                    endorsePeers);

            for (ProposalResponse response : proposalResponses) {
                ProposalResponsePackage.ProposalResponse proposalResponse = response.getProposalResponse();
                int code = proposalResponse.getResponse().getStatus();
                if (code == 501 || code == 502) {
                    // 冲突
                    //取冲突时间
                    //System.out.println("payload" + response.getProposalResponse().getPayload().toStringUtf8());
                    log.error("发生冲突，{}",response.getMessage());
                    throw new RuntimeException("发生冲突");
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
                commitHandler.waitForEvents(5, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                commitHandler.cancelListening();
                throw e;
            } catch (Exception e) {
                commitHandler.cancelListening();
                throw new ContractException("Failed to send transaction to the orderer", e);
            }
            res = validPeerResponse.iterator().next().getChaincodeActionResponsePayload();
        }catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
        concurrentHashMap.remove(args[0]);
        return res;
    }

    // 新版，还没测试
    private byte[] dclSendTransactionV2(String chaincodeId, String contractName,List<Peer> endorsePeers,String... args) throws InvalidArgumentException, ContractException, ProposalException, TimeoutException {
        Long lockTime = concurrentHashMap.getOrDefault(args[0], 0L);
        Long currentTime = System.currentTimeMillis();
        if (currentTime - lockTime < 10000) { // 10s
            throw new RuntimeException("本地缓存冲突");
        }

        byte[] res = null;
        try {
            concurrentHashMap.put(args[0],currentTime);
            String transactionId = null;
            TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeName(chaincodeId);
            transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            transactionProposalRequest.setFcn(contractName);
            transactionProposalRequest.setArgs(args);

            Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(transactionProposalRequest,
                    endorsePeers);

            for (ProposalResponse response : proposalResponses) {
                ProposalResponsePackage.ProposalResponse proposalResponse = response.getProposalResponse();
                int code = proposalResponse.getResponse().getStatus();
                if (code == 501 || code == 502) {
                    // 冲突
                    //取冲突时间
                    //System.out.println("payload" + response.getProposalResponse().getPayload().toStringUtf8());
                    log.error("发生冲突，{}",response.getMessage());
                    throw new RuntimeException("发生冲突");
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
                commitHandler.waitForEvents(5, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                commitHandler.cancelListening();
                throw e;
            } catch (Exception e) {
                commitHandler.cancelListening();
                throw new ContractException("Failed to send transaction to the orderer", e);
            }
            res = validPeerResponse.iterator().next().getChaincodeActionResponsePayload();
        }catch (Exception e) {
            log.info(e.getMessage());
            throw e;
        }finally {
            concurrentHashMap.remove(args[0]);
        }
        return res;
    }

    @GetMapping("/modifyLMQF")
    public JsonData modifyLMQF()  {

        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        try {
            byte[] bytes = RedisSendTransaction(gatewayConfig.getContractName(), operation, network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)), args);
        }catch (Exception e) {
            //return JsonData.buildError(e.getMessage());
        }
        return JsonData.buildSuccess();
    }
    @GetMapping("/modifyLMQFEndorseOr")
    public JsonData modifyLMQFEndorseOr()  {

        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        try {
            Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
            List<Peer> EndorsePeer = new ArrayList<>(1);
            List<Peer> peerList = new ArrayList<>(peers);
            if (peerList.size() >= 2) {
                EndorsePeer.add(peerList.get((int)(Math.random()*peers.size())));
            }
            byte[] bytes = RedisSendTransaction(gatewayConfig.getContractName(), operation, EndorsePeer, args);
        }catch (Throwable e) {
            //return JsonData.buildError(e.getMessage());
        }
        return JsonData.buildSuccess();
    }
    private byte[] RedisSendTransaction( String contractName, String actionName,Collection<Peer> endorsePeers,String... args) throws InvalidArgumentException, ContractException, ProposalException, TimeoutException, InvalidProtocolBufferException, InterruptedException {
        String key = args[0];
        RLock lock = redissonClient.getLock(key);
        //boolean b = lock.tryLock(100, TimeUnit.MILLISECONDS);
        boolean b = lock.tryLock();
        if (!b) {
            return null;
        }
        try {
            String transactionId = null;
            TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeName(contractName);
            transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            transactionProposalRequest.setFcn(actionName);
            transactionProposalRequest.setArgs(args);
            Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(transactionProposalRequest,
                    endorsePeers);
            for (ProposalResponse response : proposalResponses) {
                transactionId = response.getTransactionID();
//                TxReadWriteSetInfo readWriteSetInfo = response.getChaincodeActionResponseReadWriteSetInfo();
//                Iterable<TxReadWriteSetInfo.NsRwsetInfo> nsRwsetInfos = readWriteSetInfo.getNsRwsetInfos();
//                for (TxReadWriteSetInfo.NsRwsetInfo info : nsRwsetInfos) {
//                    if (info.getNamespace().equalsIgnoreCase("_lifecycle")) {
//                        continue;
//                    }
//                    KvRwset.KVRWSet rwset = info.getRwset();
//                    List<KvRwset.KVWrite> writesList = rwset.getWritesList();
//                    for (KvRwset.KVWrite write : writesList) {
//                        key = write.getKey();
//                        redisTemplate.opsForValue().setIfAbsent(key, ":1111", 10, TimeUnit.SECONDS);
//                        break;
//                    }
//                }
//                break;
            }

            Collection<ProposalResponse> validPeerResponse = validatePeerResponses(proposalResponses);

            //发送给orderer
            GatewayImpl gatewayImpl = (GatewayImpl) gateway;
            CommitHandlerFactory commitHandlerFactory = gatewayImpl.getCommitHandlerFactory();
            CommitHandler commitHandler = commitHandlerFactory.create(transactionId, network);
            commitHandler.startListening();
            try {
                Channel.TransactionOptions transactionOptions = Channel.TransactionOptions.createTransactionOptions()
                        .nOfEvents(Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour
                channel.sendTransaction(validPeerResponse, transactionOptions)
                        .get(5, TimeUnit.MINUTES);
                commitHandler.waitForEvents(5, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                commitHandler.cancelListening();
                throw e;
            } catch (Exception e) {
                commitHandler.cancelListening();
                throw new ContractException("Failed to send transaction to the orderer", e);
            }

            return null;
        }catch (Exception e) {
            throw e;
        }finally {
            lock.unlock();
        }
//        } finally {
//            if (!key.equalsIgnoreCase("")) {
//                redisTemplate.delete(key);
//            }
//        }

    }


    private byte[] RedisSendTransactionV2( String contractName, String actionName,Collection<Peer> endorsePeers,String... args) throws InvalidArgumentException, ContractException, ProposalException, TimeoutException {
        String exist = redisTemplate.opsForValue().get(args[0]);
        if ("xxxxx".equalsIgnoreCase(exist)) {
            throw new RuntimeException("本地缓存冲突");
        }
        redisTemplate.opsForValue().set(args[0],"xxxxx");
        byte[] res = null;
        try {
            String transactionId = null;
            TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeName(contractName);
            transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            transactionProposalRequest.setFcn(actionName);
            transactionProposalRequest.setArgs(args);

            Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(transactionProposalRequest,
                    endorsePeers);

            for (ProposalResponse response : proposalResponses) {
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
                commitHandler.waitForEvents(5, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                commitHandler.cancelListening();
                throw e;
            } catch (Exception e) {
                commitHandler.cancelListening();
                throw new ContractException("Failed to send transaction to the orderer", e);
            }
            res = validPeerResponse.iterator().next().getChaincodeActionResponsePayload();
        }catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }finally {
            redisTemplate.delete(args[0]);
        }
        return res;
    }


    // 重试机制
    // 返回1 表示提交成功
    // > 1 表示锁定的时间戳
    //
    public Long dclSendTransactionWithRetry( String contractName, String actionName, String... args) {
        Long lockTime = concurrentHashMap.getOrDefault(args[0], 0L);
        Long currentTime = System.currentTimeMillis();
        if (currentTime - lockTime < 10000) { // 10s
            return lockTime;
        }
        concurrentHashMap.put(args[0],currentTime);

        String transactionId = null;
        TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeName(contractName);
        transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
        transactionProposalRequest.setFcn(actionName);
        transactionProposalRequest.setArgs(args);

        //先取所有的Peer
        Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
        Collection<ProposalResponse> proposalResponses = null;
        try {
            proposalResponses = channel.sendTransactionProposal(transactionProposalRequest,
                    peers);
        } catch (ProposalException | InvalidArgumentException e) {
            log.error("背书错误，err={}",e.getMessage());
            return -1L;
        }
        for (ProposalResponse response : proposalResponses) {
            ProposalResponsePackage.ProposalResponse proposalResponse = response.getProposalResponse();
            int code = proposalResponse.getResponse().getStatus();
            if (code == 501 || code == 502) {
                // 冲突
                //取冲突时间
                //System.out.println("payload" + response.getProposalResponse().getPayload().toStringUtf8());
                log.error("发生冲突，{}",response.getMessage());
                String lockTimeStr = response.getProposalResponse().getPayload().toStringUtf8();
                return Long.valueOf(lockTimeStr);
            }
            transactionId = response.getTransactionID();
        }

        Collection<ProposalResponse> validPeerResponse = null;
        try {
            validPeerResponse  = validatePeerResponses(proposalResponses);
        } catch (ContractException e) {
            log.error("验证多个背书响应错误,err={}",e.getMessage());
            return -1L;
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
            commitHandler.waitForEvents(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            commitHandler.cancelListening();
            log.error("提交给orderer超时,exception={}",e);
            return -1L;
        } catch (Exception e) {
            commitHandler.cancelListening();
            log.error("Failed to send transaction to the orderer", e);
            return -1L;
        }
        //validPeerResponse.iterator().next().getChaincodeActionResponsePayload();
        concurrentHashMap.remove(args[0]);
        return 1L;
    }


    @GetMapping("/modifyDCLWithRetry")
    public JsonData modifyDCLWithRetry() {
        String operation = SmallBank.getRandomOperationName();
        String[] args = smallBank.getRandomOperationArguments(operation);
        Long l = dclSendTransactionWithRetry(gatewayConfig.getContractName(), operation, args);
        if (l > 1) {
            RetryTask retryTask = RetryTask.builder().level(1).retryCount(1).contractName(gatewayConfig.getContractName())
                    .operationName(operation).args(args).lockTime(l).smallBankController(this).build();
            ContractRetryConfig.scheduledExecutorService.schedule(retryTask,ContractRetryConfig.delayTime(1,l,1L),TimeUnit.MILLISECONDS);
        }
        return JsonData.buildSuccess();
    }
}
