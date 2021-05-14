package com.pk.akkademo.bigprime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.IntStream;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import akka.actor.typed.javadsl.Behaviors;

public class ManagerBehavior extends AbstractBehavior<ManagerBehavior.Command> {

    public static interface Command extends Serializable {}

    @AllArgsConstructor
    @Getter
    public static class InstructionCommand implements Command {
        private String message;
    }

    @AllArgsConstructor
    @Getter
    public static class ResultsCommand implements Command {
        private BigInteger result;
    }

    private ManagerBehavior(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(ManagerBehavior::new);
    }


    private SortedSet<BigInteger> primes = new TreeSet<>();
    private static final int PRIMES_COUNT = 20;
    public static final String MANAGER_START = "start";

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ResultsCommand.class, command -> {
                    primes.add(command.getResult());
                    System.out.println("Received " + primes.size() + " result(s) back");
                    if (primes.size() == PRIMES_COUNT) {
                        primes.forEach(System.out::println);
                    }
                    return this;
                })
                .onMessage(InstructionCommand.class, command -> {
                    if (command.getMessage().equals("start")) {
                        IntStream.rangeClosed(1, PRIMES_COUNT).forEach(i -> {
                            var worker = this.getContext().spawn(WorkerBehavior.create(), "worker-" + i);
                            worker.tell(new WorkerBehavior.Command("start", getContext().getSelf()));
                        });
                    }
                    return this;
                })
                .build();
    }    
}
