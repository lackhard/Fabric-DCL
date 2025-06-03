package com.lei.listener;

import lombok.extern.slf4j.Slf4j;

import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

@Configuration
@Slf4j
public class ChannelListener {
    @Autowired
    private Channel channel;
    @Bean
    public void registerChaincodeEventListener() throws InvalidArgumentException {
        log.info("注册通道链码事件监听器");
        Pattern  all = compile(".*");
        channel.registerChaincodeEventListener(all, all, (handle, blockEvent, chaincodeEvent) -> {
            log.info("handle:{}, blockEvent: {} chainCodeEvent:{}", handle,blockEvent.toString(), chaincodeEvent.toString() );
        });
    }
}
