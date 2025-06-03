package com.lei.config;

import org.springframework.beans.factory.annotation.Configurable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 合约重试配置
 */
@Configurable
public class ContractRetryConfig {

    public static long epson = 2;
    public static long delta = 1000;
    public static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(8);

    public void submit() {

    }

    public static Long getDelta() {
        return (long) (Math.random()* delta);
    }

    public static Long delayTime(int N, long tl, long level) {
        long tc = System.currentTimeMillis();
        return ((int) Math.pow(2, N) - 1) * epson * (tc - tl) / level + getDelta();
    }
}
