package com.lei.biz;

import com.lei.controller.AttributeController;
import com.lei.controller.DecideController;
import com.lei.controller.ResourceController;
import com.lei.controller.UserController;
import com.lei.controller.request.AttributeRequest;
import com.lei.controller.request.BuyPrivateAttributeRequest;
import com.lei.controller.request.ResourceRequest;
import com.lei.model.Record;
import com.lei.request.DecideRequest;
import com.lei.util.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.ContractException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * @author lei
 * @since 2023-05-13
 */

@SpringBootTest
@Slf4j
public class AllTest {
    @Autowired
    UserController userController;

    @Autowired
    ResourceController resourceController;

    @Autowired
    AttributeController attributeController;

    @Autowired
    DecideController decideController;

    @Test
    public void registerUser() throws ContractException, InterruptedException, TimeoutException {
        long startTime = System.currentTimeMillis();
        JsonData res = userController.add("org1");
        long endTime = System.currentTimeMillis();
        log.info("res={}", res);
        log.info("执行时间={}ms",endTime-startTime);
    }
    @Test
    public void publishResource() throws ContractException, InterruptedException, TimeoutException {
        long startTime = System.currentTimeMillis();
        JsonData jsonData = null;

        ResourceRequest resourceRequest = ResourceRequest.builder().id(UUID.randomUUID().toString())
                .description("this is a resource")
                .url("http://www.baidu.com")
                .owner("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f36513034396332396d644335705a6d46756447467a655335755a58517354315539526d4669636d6c6a4c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a315655773d3dd41d8cd98f00b204e9800998ecf8427e")
                .build();
         jsonData = resourceController.createResource(resourceRequest);

        long endTime = System.currentTimeMillis();
        log.info("res={}", jsonData);
        log.info("执行时间={}ms",(endTime-startTime));
    }

    @Test
    public void publishPrivateAttribute() throws ContractException, InterruptedException, TimeoutException {
        long startTime = System.currentTimeMillis();
        AttributeRequest attributeRequest = AttributeRequest.builder()
                .type("PRIVATE")
                .resourceId("resource:86f9bfb4-4621-4514-b837-8e0fcb3837fa")
                .ownerId("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f36513034396332396d644335705a6d46756447467a655335755a58517354315539526d4669636d6c6a4c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a315655773d3dd41d8cd98f00b204e9800998ecf8427e")
                .key("occupation")
                .value("doctor")
                .money(50)
                .notBefore("1669791474807")
                .notAfter("1672383443000").build();
        JsonData jsonData = attributeController.publish(attributeRequest);
        long endTime = System.currentTimeMillis();
        log.info("res={}", jsonData);
        log.info("执行时间={}ms",(endTime-startTime));
    }

    @Test
    public void addPrivateAttribute() throws ContractException, InterruptedException, TimeoutException {
        long startTime = System.currentTimeMillis();
        BuyPrivateAttributeRequest attributeRequest = BuyPrivateAttributeRequest.builder()
                .attributeId("")
                .buyer("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f36513034396332396d644335705a6d46756447467a655335755a58517354315539526d4669636d6c6a4c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a315655773d3dd41d8cd98f00b204e9800998ecf8427e")
                .seller("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f36513034396332396d644335705a6d46756447467a655335755a58517354315539526d4669636d6c6a4c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a315655773d3dd41d8cd98f00b204e9800998ecf8427e")
                .attributeId("attribute:private:resource:86f9bfb4-4621-4514-b837-8e0fcb3837fa:occupation")
                .build();
        JsonData jsonData = attributeController.buy(attributeRequest);
        long endTime = System.currentTimeMillis();
        System.out.println("增加私有属性成功");
        log.info("res={}", jsonData);
        log.info("执行时间={}ms",(endTime-startTime));
    }

    @Test
    public void addPublicAttribute() throws ContractException, InterruptedException, TimeoutException {
        long startTime = System.currentTimeMillis();
        AttributeRequest attributeRequest = AttributeRequest.builder()
                .type("PUBLIC")
                .key("age")
                .value("40")
                .money(50)
                .notBefore("1669791474807")
                .notAfter("1672383443000").build();
        JsonData jsonData = attributeController.addAttribute(attributeRequest);
        long endTime = System.currentTimeMillis();
        log.info("res={}", jsonData);
        log.info("执行时间={}ms",(endTime-startTime));
    }

    @Test
    public void accessResource() throws ContractException {
        long startTime = System.currentTimeMillis();
        DecideRequest decideRequest = DecideRequest.builder()
                .resourceId("resource:86f9bfb4-4621-4514-b837-8e0fcb3837fa")
                .requesterId("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f36513034396332396d644335705a6d46756447467a655335755a58517354315539526d4669636d6c6a4c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a315655773d3dd41d8cd98f00b204e9800998ecf8427e")
                .build();
        JsonData jsonData = decideController.decideNoRecord(decideRequest);
        long endTime = System.currentTimeMillis();
        log.info("res={}", jsonData);
        log.info("执行时间={}ms",(endTime-startTime));
    }

    @Test
    public void addRecord() throws ContractException, InterruptedException, TimeoutException {
        long startTime = System.currentTimeMillis();
        Record record = Record.builder()
                .Id(UUID.randomUUID().toString())
                .requesterId("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f36513034396332396d644335705a6d46756447467a655335755a58517354315539526d4669636d6c6a4c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a315655773d3dd41d8cd98f00b204e9800998ecf8427e")
                .response("xxxxx")
                .resourceId("resource:86f9bfb4-4621-4514-b837-8e0fcb3837fa").build();
        JsonData jsonData = decideController.createRecord(record);
        long endTime = System.currentTimeMillis();
        log.info("res={}", jsonData);
        log.info("执行时间={}ms",(endTime-startTime));
    }
}
