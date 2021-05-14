package com.pk.akkademo.bigprime;

import java.math.BigInteger;
import java.util.Random;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class WorkerBehavior extends AbstractBehavior<WorkerBehavior.Command>{

    @AllArgsConstructor
    @Getter
    public static class Command {
        private String message;
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
                .onAnyMessage(m -> {
                    if (m.getMessage().equals("start")) {
                        BigInteger nextPrime = new BigInteger(200, new Random()).nextProbablePrime();
                        m.getSender().tell(new ManagerBehavior.ResultsCommand(nextPrime));
                    }
                    return this;
                })
                .build();
    }
    
}
