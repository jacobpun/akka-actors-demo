package com.pk.akkademo.blockchain;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import com.pk.akkademo.blockchain.model.Block;
import com.pk.akkademo.blockchain.model.BlockChain;
import com.pk.akkademo.blockchain.model.BlockValidationException;
import com.pk.akkademo.blockchain.model.HashResult;
import com.pk.akkademo.blockchain.utils.BlocksData;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlockMiner {
    private BlockChain blocks;
    ActorSystem<ManagerBehavior.Command> actorSystem;

    public BlockMiner() {
        blocks = new BlockChain();
        actorSystem = ActorSystem.create(MiningSystemBehavior.create(), "blockchainminer");
    }

    public void mineBlock(int blockId) {
        if (blockId == 10) {
            return;
        }
        String lastHash = blocks.getLastHash();
        if (lastHash == null) {
            lastHash = "0";
        }
        Block block = BlocksData.getNextBlock(blockId, lastHash);
        CompletionStage<HashResult> result =  AskPattern.ask(
            actorSystem, 
            (ActorRef<HashResult> me) -> new ManagerBehavior.MineBlockCommand(block, 5, me), 
            Duration.ofSeconds(30), 
            actorSystem.scheduler()
        );

        result.whenComplete((hash, error) -> {
            if (hash != null) {
                block.setHash(hash.getHash());
                block.setNonce(hash.getNonce());
                try {
                    blocks.addBlock(block);
                    if (blocks.getSize() == 10) {
                        blocks.printAndValidate();
                    }
                    this.mineBlock(blockId + 1);
                } catch (BlockValidationException e) {
                    e.printStackTrace();
                    log.error("ERROR: Invalid block for index {}", blockId);
                }
            } else {
                log.warn("WARN: No mining response for block {}", blockId);
            }
        });
    }

    public void mineIndependentBlock(int blockId) {
        Block block = BlocksData.getNextBlock(blockId, "12345");
        CompletionStage<HashResult> result =  AskPattern.ask(
            actorSystem, 
            (ActorRef<HashResult> me) -> new ManagerBehavior.MineBlockCommand(block, 5, me), 
            Duration.ofSeconds(30), 
            actorSystem.scheduler()
        );

        result.whenComplete((hash, error) -> {
            if (hash != null) {
                log.info("Independent Block: {}: {}", hash.getNonce(), hash.getHash());
            } else {
                log.warn("WARN: No mining response for block {}", blockId);
            }
        });
    }
}
