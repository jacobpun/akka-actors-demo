package com.pk.akkademo.blockchain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class Transaction {
    private int id;
	private long timestamp;
	private int accountNumber;
	private double amount;
}
