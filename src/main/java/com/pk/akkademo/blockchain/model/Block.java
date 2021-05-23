package com.pk.akkademo.blockchain.model;

import lombok.Getter;

@Getter
public class Block {
    private String previousHash;
	private Transaction transaction;
	private int nonce;
	private String hash;
	
	public Block(Transaction transaction, String previousHash) {
		this.previousHash = previousHash;
		this.transaction = transaction;
	}
	
	public void setNonce(int nonce) {
		this.nonce = nonce;
	}
	
	public void setHash(String hash) {
		this.hash = hash;
	}
}
