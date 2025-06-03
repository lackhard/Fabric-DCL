package com.lei.model.smallbank;

import lombok.Data;

/**
 * @author lei
 * @since 2024-01-26
 */
@Data
public class Account {
    private Integer customer_id;
    private String customer_name;
    private Integer initial_checking_balance;
    private Integer initial_savings_balance;
}
