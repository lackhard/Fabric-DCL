package com.lei.biz;



import com.alibaba.fastjson2.JSON;
import com.lei.controller.request.AttributeRequest;
import com.lei.controller.request.BuyPrivateAttributeRequest;
import com.lei.controller.request.ResourceRequest;
import com.lei.enums.AttributeTypeEnum;
import com.lei.model.Attribute;

import com.lei.model.User;
import com.lei.util.JsonData;
import com.lei.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.sdk.Peer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;



import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;


/**
 * @author lei
 * @since 2023/2/7
 */

@SpringBootTest
@Slf4j
public class AttributeTest {

    @Autowired
    private Contract contract;

    @Autowired
    private Network network;


    @Test
    public void init() throws ContractException, InterruptedException, TimeoutException {
        //注册自身身份
        Transaction transaction = contract.createTransaction("CreateUser")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        User user = User.builder()
                .money(100)
                .org("org1")
                .build();
        byte[] invokeResult = transaction.submit(JSON.toJSONString(user));
        log.info("调用结果:" + new String(invokeResult));

    }
    @Test
    public void addResource() throws ContractException, InterruptedException, TimeoutException {
        //注册资源
        ResourceRequest request = new ResourceRequest();
        request.setId("resource:"+ "571971ca-932f-4e39-bc8d-475778f44401");
        request.setUrl("http://www.baidu.com");
        request.setDescription("我的资源");
        Transaction transaction2 = contract.createTransaction("CreateResource")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        byte[] invokeResult2 = transaction2.submit(JSON.toJSONString(request));
        log.info("调用结果:" + new String(invokeResult2));
    }
    //增加公有属性
    @Test
    public void AddPublicAttribute() throws ContractException, InterruptedException, TimeoutException {

        Transaction transaction = contract.createTransaction("AddAttribute")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        Attribute attribute = Attribute.builder()
                .id("attribute:" + UUID.randomUUID())
                .type(AttributeTypeEnum.PUBLIC.name())
                .ownerId("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f365130343962334a6e4d5335735a576b75626d56304c45395650555a68596e4a705979785050556835634756796247566b5a3256794c464e5550553576636e526f49454e68636d3973615735684c454d3956564d3dd41d8cd98f00b204e9800998ecf8427e")
                .key("age")
                .value("40")
                .notBefore("1669791474807")
                .notAfter("1772383443000")
                .build();

        byte[] invokeResult = transaction.submit(JsonUtil.obj2Json(attribute));
        log.info("调用结果:" +  new String(invokeResult));


    }
    //发布私有属性
    @Test
    public void publishPrivateAttribute() throws ContractException, InterruptedException, TimeoutException {
        Transaction transaction = contract.createTransaction("PublishPrivateAttribute")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));

        AttributeRequest attributeRequest = new AttributeRequest();
        attributeRequest.setType("PRIVATE");
        attributeRequest.setResourceId("resource:571971ca-932f-4e39-bc8d-475778f44401");
        attributeRequest.setOwnerId("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f365130343962334a6e4d5335735a576b75626d56304c45395650555a68596e4a705979785050556835634756796247566b5a3256794c464e5550553576636e526f49454e68636d3973615735684c454d3956564d3dd41d8cd98f00b204e9800998ecf8427e");
        attributeRequest.setKey("occupation0");
        attributeRequest.setValue("doctor");
        attributeRequest.setMoney(0);
        attributeRequest.setNotBefore("1669791474807");
        attributeRequest.setNotAfter("1772383443000");
        byte[] result = transaction.submit(JsonUtil.obj2Json(attributeRequest));
        System.out.println(new String(result));

    }
    // 增加私有属性
    @Test
    public void AddPrivateAttribute() throws ContractException {
        Transaction transaction = contract.createTransaction("BuyPrivateAttribute")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
        BuyPrivateAttributeRequest request = new BuyPrivateAttributeRequest();
        request.setAttributeId("attribute:private:resource:571971ca-932f-4e39-bc8d-475778f44401:occupation0");
        request.setBuyer("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f365130343962334a6e4d5335735a576b75626d56304c45395650555a68596e4a705979785050556835634756796247566b5a3256794c464e5550553576636e526f49454e68636d3973615735684c454d3956564d3dd41d8cd98f00b204e9800998ecf8427e");
        request.setSeller("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f365130343962334a6e4d5335735a576b75626d56304c45395650555a68596e4a705979785050556835634756796247566b5a3256794c464e5550553576636e526f49454e68636d3973615735684c454d3956564d3dd41d8cd98f00b204e9800998ecf8427e");

        byte[] invokeResult = transaction.evaluate(JsonUtil.obj2Json(request));
        System.out.println(new String(invokeResult));

    }

    // 测试增加公有属性
    @Test
    public void testAddPublicAttribute() throws ContractException {
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Transaction transaction = contract.createTransaction("AddAttribute")
                    .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
            Attribute attribute = Attribute.builder()
                    .id("attribute:" + UUID.randomUUID())
                    .type(AttributeTypeEnum.PUBLIC.name())
                    .ownerId("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f365130343962334a6e4d5335735a576b75626d56304c45395650555a68596e4a705979785050556835634756796247566b5a3256794c464e5550553576636e526f49454e68636d3973615735684c454d3956564d3dd41d8cd98f00b204e9800998ecf8427e")
                    .key("name")
                    .value("张三")
                    .notBefore("1669791474807")
                    .notAfter("1772383443000")
                    .build();

            byte[] invokeResult = transaction.evaluate(JsonUtil.obj2Json(attribute));
            //log.info("调用结果:" +  new String(invokeResult));
//            String transactionId = transaction.getTransactionId();
//            Map<String, String > res = new HashMap(2);
//            res.put("txId", transactionId);
//            res.put("data", JsonUtil.obj2Json(invokeResult));
            //log.info(JsonUtil.obj2Json(res));
        }
        long endTime = System.nanoTime();
        log.error("执行时间：{}ms",(endTime-startTime) / 1000000.0); //5614 ms
    }


    //发布私有属性 发布之前需要注册资源
    @Test
    public void testPublishPrivateAttribute() throws ContractException, InterruptedException, TimeoutException {
        long startTime  = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Transaction transaction = contract.createTransaction("PublishPrivateAttribute")
                    .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));

            AttributeRequest attributeRequest = new AttributeRequest();
            attributeRequest.setType("PRIVATE");
            attributeRequest.setResourceId("resource:571971ca-932f-4e39-bc8d-475778f44401");
            attributeRequest.setOwnerId("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f365130343962334a6e4d5335735a576b75626d56304c45395650555a68596e4a705979785050556835634756796247566b5a3256794c464e5550553576636e526f49454e68636d3973615735684c454d3956564d3dd41d8cd98f00b204e9800998ecf8427e");
            attributeRequest.setKey("occupation0");
            attributeRequest.setValue("doctor");
            attributeRequest.setMoney(0);
            attributeRequest.setNotBefore("1669791474807");
            attributeRequest.setNotAfter("1772383443000");
            byte[] result = transaction.evaluate(JsonUtil.obj2Json(attributeRequest));

//            Map<String, Object> map = new HashMap<>(2);
//            map.put("txId", transaction.getTransactionId());
//            // 里面应该是 属性id
//            map.put("data", new String(result));
            //log.info(JsonUtil.obj2Json(map));
        }
        long endTime = System.nanoTime();
        log.error("执行时间：{}ms",(endTime-startTime) / 1000000.0); // 5753
    }
    //根据资源检索私有属性
    @Test
    public void testGetAttributeByResourceId() throws ContractException {
        long startTime  = System.nanoTime();
        String resourceId = "resource:571971ca-932f-4e39-bc8d-475778f44401";
        for (int i = 0; i < 1000; i++) {
            byte[] attributes = contract.evaluateTransaction("FindAttributeByResourceId", resourceId);

            //log.info(new String(invokeResult));
        }
        long endTime = System.nanoTime();
        log.error("执行时间：{}ms",(endTime-startTime) / 1000000.0); // 5559 ms
    }

    /**
     * 测试增加私有属性
     * @throws ContractException
     */
    @Test
    public void testAddPrivateAttribute() throws ContractException {
        long startTime  = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Transaction transaction = contract.createTransaction("BuyPrivateAttribute")
                    .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
            BuyPrivateAttributeRequest request = new BuyPrivateAttributeRequest();
            request.setAttributeId("attribute:private:resource:571971ca-932f-4e39-bc8d-475778f44401:occupation0");
            request.setBuyer("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f365130343962334a6e4d5335735a576b75626d56304c45395650555a68596e4a705979785050556835634756796247566b5a3256794c464e5550553576636e526f49454e68636d3973615735684c454d3956564d3dd41d8cd98f00b204e9800998ecf8427e");
            request.setSeller("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f365130343962334a6e4d5335735a576b75626d56304c45395650555a68596e4a705979785050556835634756796247566b5a3256794c464e5550553576636e526f49454e68636d3973615735684c454d3956564d3dd41d8cd98f00b204e9800998ecf8427e");

            byte[] invokeResult = transaction.evaluate(JsonUtil.obj2Json(request));

            //log.info(JsonUtil.obj2Json(map));
        }
        long endTime = System.nanoTime();
        log.error("执行时间：{}ms",(endTime-startTime) / 1000000.0);// 6628ms
    }

    /**
     * 测试 根据属性id查询属性
     * @throws ContractException
     */
    @Test
    public void testGetAttributeByAttributeID() throws ContractException {
        long startTime  = System.nanoTime();
        String attributeId = "attribute:private:resource:571971ca-932f-4e39-bc8d-475778f44401:occupation0";
        for (int i = 0; i < 1000; i++) {
            Transaction transaction = contract.createTransaction("FindAttributeById")
                    .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)));
            byte[] invokeResult = transaction.evaluate(attributeId);

            //log.info(new String(invokeResult));
        }
        long endTime = System.nanoTime();
        log.error("执行时间：{}ms",(endTime-startTime) / 1000000.0); // 5301ms
    }



}
