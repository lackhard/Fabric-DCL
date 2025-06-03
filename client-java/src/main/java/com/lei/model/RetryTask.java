package com.lei.model;


import com.lei.config.ContractRetryConfig;
import com.lei.controller.SmallBank;
import com.lei.controller.SmallBankController;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RetryTask implements Callable<Long> {
    private String contractName;

    private String operationName;

    private String[] args;

    private Integer retryCount;

    private Long lockTime;

    private Long nextRetryTime;

    private Integer level;

    private SmallBankController smallBankController;



    @Override
    public Long call() {

        Long l = smallBankController.dclSendTransactionWithRetry(contractName, operationName,  args);
        if (l > 1) {
            // 计算下次重试时间
            RetryTask retryTask = RetryTask.builder()
                    .smallBankController(this.smallBankController)
                    .contractName(this.contractName)
                    .operationName(this.operationName)
                    .args(args)
                    .retryCount(this.retryCount + 1)
                    .lockTime(l)
                    .build();
            long delayTime = ContractRetryConfig.delayTime(this.retryCount + 1,l,this.level);
            retryTask.setNextRetryTime(System.currentTimeMillis() + delayTime);
            ContractRetryConfig.scheduledExecutorService.schedule(retryTask,delayTime, TimeUnit.MILLISECONDS);
        }
        return l;
    }
}
