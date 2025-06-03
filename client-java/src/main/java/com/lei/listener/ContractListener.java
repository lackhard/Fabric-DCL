package com.lei.listener;


import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lizhi
 */
@Configuration
@Slf4j
public class ContractListener {

    @Autowired
    private Contract contract;

    @Bean
    public void addListener() {
        log.info("注册合约事件");
        contract.addContractListener(contractEvent -> {
            log.info("合约事件, 事件name:{}, 事件chaincodeId：{}， payload：{}" ,contractEvent.getName(),
                    contractEvent.getChaincodeId(), new String(contractEvent.getPayload().get()) );
        });
    }
}
