package com.lei.controller;

import com.alibaba.fastjson2.JSON;
import com.lei.enums.AttributeTypeEnum;
import com.lei.controller.request.AttributeRequest;
import com.lei.model.Attribute;
import com.lei.model.User;
import com.lei.util.JsonData;
import com.lei.util.JsonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.sdk.Peer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.*;
import java.util.concurrent.TimeoutException;


/**
 * @author lizhi
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/user")
@Api("用户相关")
public class UserController {


    @Autowired
    private Contract contract;

    @Autowired
    private Network network;

    @GetMapping("/all")
    @ApiOperation("查看所有用户身份")
    public JsonData all() throws ContractException {
        byte[] allUsers = contract.evaluateTransaction("GetAllUsers");

        return JsonData.buildSuccess(JsonUtil.bytes2Obj(allUsers, User[].class));
    }

    @GetMapping("/onlyOnePeer")
    public JsonData onlyOnePeer() throws ContractException, InterruptedException, TimeoutException {
        Collection<Peer> peers = network.getChannel().getPeers();
        Peer peer = peers.iterator().next();
        Peer peer2 = peers.iterator().next();

        contract.createTransaction("GetAllUsers").setEndorsingPeers(Arrays.asList(peer2));
        byte[] allUsers = contract.submitTransaction("GetAllUsers");

        return JsonData.buildSuccess(JsonUtil.bytes2Obj(allUsers, User[].class));
    }
    @PostMapping("/add")
    @ApiOperation("添加当前用户的身份")
    public JsonData add(@RequestParam("org")String org) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("CreateUser")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        User user = User.builder()
                .money(100)
                .org(org)
                .build();
        byte[] invokeResult = transaction.submit(JSON.toJSONString(user));
        log.info("调用结果:" + new String(invokeResult));
        String transactionId = transaction.getTransactionId();
        return JsonData.buildSuccess(transactionId);

    }
    @DeleteMapping("/del")
    @ApiOperation("删除当前用户")
    public JsonData del() throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("DeleteUser")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));

        byte[] invokeResult = transaction.submit();
        log.info("调用结果:" +  new String(invokeResult));
        String transactionId = transaction.getTransactionId();
        return JsonData.buildSuccess(transactionId);
    }
    @GetMapping("/history")
    public JsonData history() throws ContractException {
        byte[] history = contract.evaluateTransaction("GetUserHistory");

        return JsonData.buildSuccess(new String(history));
    }
    @GetMapping("/my")
    @ApiOperation("查看我的身份")
    public JsonData my() throws ContractException {
        byte[] history = contract.evaluateTransaction("GetSubmittingClientIdentity");

        return JsonData.buildSuccess(new String(history));
    }
}
