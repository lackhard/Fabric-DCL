package com.lei.biz;

import com.alibaba.fastjson2.TypeReference;
import com.lei.controller.AttributeController;
import com.lei.controller.ResourceController;
import com.lei.controller.UserController;
import com.lei.controller.request.*;
import com.lei.model.Attribute;
import com.lei.model.Resource;
import com.lei.util.JsonData;
import com.lei.util.JsonUtil;
import net.minidev.json.JSONUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hyperledger.fabric.gateway.ContractException;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RestController;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author lei
 * @since 2023-03-25
 */

@SpringBootTest
public class InitTest {
    @Autowired
    private CloseableHttpClient closeableHttpClient;

    @Autowired
    private ResourceController resourceController;

    @Autowired
    private AttributeController attributeController;

    @Autowired
    private UserController userController;



    public static String goServerRootUrl = "http://127.0.0.1:7788";


    @Test
    public void  registerCurrentUser() throws ContractException, InterruptedException, TimeoutException {
        JsonData jsonData = userController.add("org1");
        System.out.println(jsonData);
        System.out.println("registerCurrentUser 成功");
    }

    @Test
    public void allInit() throws Exception {
        registerAllUser();
        addPublicAttribute();
        addResource();
        //查询资源
        JsonData allResource = resourceController.findAllResource();
        List<Resource> data = allResource.getData(new TypeReference<List<Resource>>() {
        });
        String resourceId = data.get(0).getId();
        publishPrivateAttribute(resourceId);

        JsonData jsonData = attributeController.find(resourceId);
        List<Attribute> dataData = jsonData.getData(new TypeReference<List<Attribute>>() {
        });
        Attribute attribute = dataData.get(0);
        String attributeId = attribute.getId();
        addPrivateAttribute(attributeId);

        addResourceControllers(resourceId);
        System.out.println("============================over=====================");

    }

    /**
     * 注册所有用户
     */
    @Test
    public void registerAllUser() throws IOException {
        HttpPost httpPost = new HttpPost(goServerRootUrl + "/api/user/addAllUser");
        CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpPost);
        System.out.println(httpResponse.getEntity().getContent().toString());
        httpResponse.close();
        System.out.println("注册所有用户成功");
    }

    @Test
    public void addPublicAttribute() throws UnsupportedEncodingException, ContractException, InterruptedException, TimeoutException {

        AttributeRequest attributeRequest = AttributeRequest.builder().type("PUBLIC")
                .key("age")
                .value("40")
                .money(50)
                .notBefore("1669791474807")
                .notAfter("1772383443000").build();
        JsonData jsonData = attributeController.addAttribute(attributeRequest);
        System.out.println(jsonData);
    }

    @Test
    public void addResource() throws  ContractException, InterruptedException, TimeoutException {
        ResourceRequest resourceRequest = ResourceRequest.builder().url("http://www.baidu.com")
                .description("访问百度")
                .build();
        JsonData jsonData = resourceController.createResource(resourceRequest);
        System.out.println(jsonData);
        System.out.println("增加资源成功");

    }

    @Test
    public void publishPrivateAttribute(@Value("") String resourceId) throws  ContractException, InterruptedException, TimeoutException {
        if (StringUtils.isBlank(resourceId)) {
            resourceId = "resource:c5f68ecc-3cb9-4382-bb6e-f6633f872dbf";
        }
        //需要更改resourceId 和 ownerId
        AttributeRequest attributeRequest = AttributeRequest.builder()
                .type("PRIVATE")
                .resourceId(resourceId)
                .ownerId("user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f36513034396332396d644335705a6d46756447467a655335755a58517354315539526d4669636d6c6a4c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a315655773d3dd41d8cd98f00b204e9800998ecf8427e")
                .key("occupation")
                .value("doctor")
                .money(50)
                .notBefore("1669791474807")
                .notAfter("1672383443000").build();
        JsonData jsonData = attributeController.publish(attributeRequest);
        System.out.println(jsonData);
        System.out.println("发布私有属性成功");

    }
    @Test
    public void addPrivateAttribute(@Value("") String attributeId) throws ContractException, InterruptedException, TimeoutException {
        //需要更改resourceId 和 ownerId
        if (StringUtils.isBlank(attributeId)) {
            attributeId = "attribute:private:resource:02bd38ba-cc37-4c02-b26d-d8b3fb902fac:occupation";
        }
        String userId = "user:654455774f546f365130343964584e6c636a457354315539593278705a5735304c45383953486c775a584a735a57526e5a58497355315139546d3979644767675132467962327870626d4573517a3156557a6f365130343962334a6e4d5335735a576b75626d56304c45395650555a68596e4a705979785050556835634756796247566b5a3256794c464e5550553576636e526f49454e68636d3973615735684c454d3956564d3dd41d8cd98f00b204e9800998ecf8427e";
        BuyPrivateAttributeRequest attributeRequest = BuyPrivateAttributeRequest.builder().attributeId("")
                .buyer(userId)
                .seller(userId)
                .attributeId(attributeId)
                .build();
        JsonData jsonData = attributeController.buy(attributeRequest);
        System.out.println(jsonData);
        System.out.println("增加私有属性成功");

    }

    @Test
    public void addResourceControllers(@Autowired(required = false) String resourceId) throws ContractException, InterruptedException, TimeoutException {

        if (StringUtils.isBlank(resourceId)) {
            resourceId = "resource:e4c5c522-924b-458b-b417-951ef1ff0ddf";
        }
        //查询所有用户身份
        JsonData all = userController.all();
        List<Map> data = all.getData(new TypeReference<List<Map>>() {
        });
        for (int i = 0; i < data.size(); i++) {
            String userid = (String) data.get(i).get("id");
            //需要更改resourceId 和 ownerId
            AddResourceControllerRequest request = AddResourceControllerRequest.builder()
                    .resourceId(resourceId)
                    .controllerId(userid)
                    .build();
            JsonData jsonData = resourceController.addController(request);
            System.out.println(jsonData);
            System.out.println("增加资源控制器成功");
        }


    }
}
