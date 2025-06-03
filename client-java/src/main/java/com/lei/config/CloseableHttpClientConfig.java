package com.lei.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author lei
 * @since 2023-03-25
 */
@Configuration
public class CloseableHttpClientConfig {
    @Bean
    public CloseableHttpClient closeableHttpClient() {
        CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
        return closeableHttpClient;
    }
}
