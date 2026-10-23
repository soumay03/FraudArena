package com.Terminator.FraudArena.dto;

import java.math.BigDecimal;

public class AccountResponse {

    private Long id;
    private String name;
    private BigDecimal balance;

    public AccountResponse() {
    }

    public AccountResponse(Long id, String name, BigDecimal balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
