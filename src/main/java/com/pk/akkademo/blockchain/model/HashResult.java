package com.pk.akkademo.blockchain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class HashResult {
    private int nonce;
	private String hash;
}
