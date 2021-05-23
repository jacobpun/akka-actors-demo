package com.pk.akkademo.blockchain;

public class Main {
    public static void main(String[] args) {
        BlockMiner miner = new BlockMiner();
        miner.mineBlock(0);
        miner.mineIndependentBlock(9);
    }
}