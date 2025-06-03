package com.lei.controller;

import com.lei.model.Record;
import com.lei.model.User;
import com.lei.request.DecideRequest;
import com.lei.util.JsonData;
import com.lei.util.JsonUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.sdk.Peer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/decide/v1")
@Api("访问控制决策相关")
@Slf4j
public class DecideController {
    @Autowired
    private Contract contract;

    @Autowired
    private Network network;

    @PostMapping("/decideNoRecord")
    public JsonData decideNoRecord(@RequestBody DecideRequest decideRequest) throws ContractException {
        Transaction transaction = contract.createTransaction("DecideNoRecord");
        DecideRequest request = DecideRequest.builder()
                .id(transaction.getTransactionId())
                .requesterId(decideRequest.getRequesterId())
                .resourceId(decideRequest.getResourceId())
                .build();

        String json = JsonUtil.obj2Json(request);
        log.info("request:{}",json);
        byte[] result = transaction.evaluate(json);

        return JsonData.buildSuccess(new String(result));
    }

    @PostMapping("/decideWithRecord")
    public JsonData decideWithRecord(@RequestBody DecideRequest decideRequest) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("DecideWithRecord")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        DecideRequest request = DecideRequest.builder()
                .id(transaction.getTransactionId())
                .requesterId(decideRequest.getRequesterId())
                .resourceId(decideRequest.getResourceId())
                .build();
        byte[] result = transaction.submit(JsonUtil.obj2Json(request));

        return JsonData.buildSuccess(new String(result));
    }

    @PostMapping
    public JsonData createRecord(@RequestBody Record record) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("CreateRecord")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));

        byte[] result = transaction.submit(JsonUtil.obj2Json(record));

        return JsonData.buildSuccess(new String(result));
    }
}
