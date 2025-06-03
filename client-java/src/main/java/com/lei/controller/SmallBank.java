package com.lei.controller;


import com.lei.model.smallbank.Account;
import com.lei.model.smallbank.QueryArgument;
import io.swagger.models.auth.In;

import java.util.ArrayList;
import java.util.List;

/**
 * 仅用于模拟 fabric中small bank 负载测试
 * @author lei
 * @since 2024-01-29
 */

public class SmallBank {
    private static List<String> OperationTypes = new ArrayList<>();

    static  {
        OperationTypes.add("transact_savings");
        OperationTypes.add("deposit_checking");
        OperationTypes.add("send_payment");
        OperationTypes.add("write_check");
        OperationTypes.add("amalgamate");
    }

    private static String Dictionary = "ABCDEFGHIJKL MNOPQRSTUVWXYZ abcdefghij klmnopqrstuvwxyz";
    private static Integer InitialBalance = 1000000;
    private static Integer CustomerNameLength  = 12;

    // 完整的账号id ： accountsGenerated + accountSuffix
    // accountSuffix 是workIndex
    // 已经生成的账号id
    private  Integer accountsGenerated  = 0;

    //账户后缀
    private  String accountSuffix = "";

    public SmallBank() {
        this.accountsGenerated = 0;
        this.accountSuffix = "";
    }

    public SmallBank(Integer accountsGenerated) {
        this.accountsGenerated = accountsGenerated;
        this.accountSuffix = "";
    }

    public Integer getAccountsGenerated() {
        return this.accountsGenerated;
    }

    //获取随机操作
    public static String getRandomOperationName() {
        int index = (int) (Math.random() * OperationTypes.size());
        return OperationTypes.get(index);
    }

   public String[] getRandomOperationArguments(String operation) {
        List<String> res = new ArrayList<>();
       switch (operation) {
           case "transact_savings":
           case "deposit_checking":
           case "write_check": {
               // amount
               res.add(getRandomInteger(200).toString());
               // customer_id
               res.add(this.getRandomAccountKey().toString());
               break;
           }
           case "send_payment": {
               Integer[] accountKeys = this.getTwoDifferentRandomAccountKeys();
               // amount
               res.add(getRandomInteger(200).toString());
               //'dest_customer_id'
               res.add(accountKeys[0].toString());
               //'source_customer_id'
               res.add(accountKeys[1].toString());
                break;
           }
           case "amalgamate": {
               Integer[] accountKeys = this.getTwoDifferentRandomAccountKeys();
               //'dest_customer_id'
               res.add(accountKeys[0].toString());
               //'source_customer_id'
               res.add(accountKeys[1].toString());

               break;
           }
           default: {
               // this shouldn't happen

           }
       }
       return res.toArray(new String[0]);
   }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(Dictionary.charAt((int)(Math.random()* ( Dictionary.length()))));
        }
        return sb.toString();
    }

    // 递增的方式 产生一个账号
    public synchronized Account getCreateAccountArguments() {
        Account account = new Account();
        account.setCustomer_id(this.generateAccountNumber());
        account.setCustomer_name(generateRandomString(CustomerNameLength));
        account.setInitial_checking_balance(InitialBalance);
        account.setInitial_savings_balance(InitialBalance);
        return account;
    }

    private Integer getRandomInteger(int max) {
        return (int) Math.ceil(Math.random()*max);
    }

    // 设置账号后缀
    public void setAccountSuffix(int workerIndex, int totalWorks) {
        int lengthForLastWorker  = String.valueOf(totalWorks - 1).length();
        this.accountSuffix = String.format("%0" + lengthForLastWorker + "d", workerIndex);
    }

    private synchronized int generateAccountNumber() {
        int num = getAccountNumberFromIndex(this.accountsGenerated);
        this.accountsGenerated++;
        return num;
    }

    private int getAccountNumberFromIndex(Integer index) {
        String accountKeyString = index.toString() + this.accountSuffix;
        return  Integer.parseInt(accountKeyString);
    }

    public QueryArgument getQueryArguments() {
        QueryArgument queryArgument = new QueryArgument();
        queryArgument.setCustomer_id(getRandomAccountKey());

        return queryArgument;
    }

    //  获取真正的账户key
    private   Integer  getRandomAccountKey () {
        return this.getAccountNumberFromIndex(this.getRandomAccountIndex());
    }

    private Integer  getRandomAccountIndex() {
        return (int) Math.floor(Math.random() * this.accountsGenerated);
    }

    private Integer[] getTwoDifferentRandomAccountKeys() {
        Integer idx1 = this.getRandomAccountIndex();
        Integer idx2 = this.getRandomAccountIndex();
        if(idx2.equals(idx1)) {
            // Cheap resolution for selecting the same index (compared to redrawing again, and again).
            // Suppose we remove the first index after selection.
            // If we draw the same index again, that actually refers to the next index in the original array.
            idx2 = (idx2 + 1) % this.accountsGenerated;
        }
        Integer[] res = new Integer[2];
        res[0] = this.getAccountNumberFromIndex(idx1);
        res[1] = this.getAccountNumberFromIndex(idx2);


        return res;
    }
}
