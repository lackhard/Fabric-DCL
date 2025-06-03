package com.lei.controller;

import com.alibaba.fastjson2.JSON;
import com.lei.model.Policy;
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

import javax.json.Json;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @author lei
 * @since 2024-02-01
 */

@RestController
@RequestMapping("/api/policy/v1")
@Api("策略相关")
@Slf4j
public class PolicyController {
    @Autowired
    private Contract contract;

    @Autowired
    private Network network;
    @PostMapping("/create")
    @ApiOperation("创建策略")
    public JsonData createPolicy(@RequestBody Policy policy) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("CreatePolicy")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        List<String> empty = new ArrayList<>();
        if (policy.getPrivateKeys() == null) {
            policy.setPrivateKeys(empty);
        }
        if (policy.getPrivateValues() == null) {
            policy.setPrivateValues(empty);
        }
        if (policy.getResourceAttributeKeys() == null) {
            policy.setRequesterAttributeKeys(empty);
        }
        if (policy.getResourceAttributeValues() == null) {
            policy.setResourceAttributeValues(empty);
        }
        System.out.println(JSON.toJSONString(policy));
        byte[] invokeResult = transaction.submit(JSON.toJSONString(policy));
        log.info("调用结果:" + new String(invokeResult));
        String transactionId = transaction.getTransactionId();
        return JsonData.buildSuccess(transactionId);
    }

    @GetMapping("/get")
    @ApiOperation("根据策略id取回策略")
    public JsonData getPolicy(@RequestParam("policyId") String policyId) throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("FindPolicyById");
        byte[] polcyBytes = transaction.evaluate(policyId);
        Policy policy = JsonUtil.json2Obj(new String(polcyBytes), Policy.class);
        return JsonData.buildSuccess(policy);
    }
}
