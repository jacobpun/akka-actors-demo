package com.pk.akkademo.blockchain;

import com.pk.akkademo.blockchain.ManagerBehavior.HashResultCommand;
import com.pk.akkademo.blockchain.model.Block;
import com.pk.akkademo.blockchain.model.HashResult;
import com.pk.akkademo.blockchain.utils.BlockChainUtils;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import akka.actor.typed.javadsl.Behaviors;

public class WorkerBehavior extends AbstractBehavior<WorkerBehavior.Command> {
    
    @AllArgsConstructor
    @Getter
    public static class Command {
        private Block block;
        private int difficultyLevel;
        private int startingNounce;
        private ActorRef<ManagerBehavior.Command> sender;
    }

    private WorkerBehavior(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(WorkerBehavior::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                    .onMessage(Command.class, command -> {
                        String hash = new String(new char[command.difficultyLevel]).replace("\0", "X");
                        String target = new String(new char[command.difficultyLevel]).replace("\0", "0");
                                
                        int nonce = command.startingNounce;
                        while (!hash.substring(0, command.difficultyLevel).equals(target) && nonce < command.startingNounce + 1000) {
                            nonce++;
                            String dataToEncode = command.block.getPreviousHash() + Long.toString(command.block.getTransaction().getTimestamp()) + Integer.toString(nonce) + command.block.getTransaction();
                            hash = BlockChainUtils.calculateHash(dataToEncode);
                        }
                        if (hash.substring(0,command.difficultyLevel).equals(target)) {
                            HashResult hashResult = new HashResult(nonce, hash);
                            command.getSender().tell(new HashResultCommand(hashResult));
                        }
                        return Behaviors.stopped();
                    })
                    .build();
    }
}
