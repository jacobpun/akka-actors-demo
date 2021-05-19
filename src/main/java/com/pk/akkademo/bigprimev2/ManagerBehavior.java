package com.pk.akkademo.bigprimev2;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.Duration;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.IntStream;

import akka.actor.typed.ActorRef;
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
        private ActorRef<SortedSet<BigInteger>> sender;
    }

    @AllArgsConstructor
    @Getter
    public static class ResultsCommand implements Command {
        private BigInteger result;
        private ActorRef<WorkerBehavior.Command> worker;
    }

    @Getter
    @AllArgsConstructor
    private static class NoResponseReceivedCommand implements Command {
        private ActorRef<WorkerBehavior.Command> worker;
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
    private ActorRef<SortedSet<BigInteger>> sender;

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ResultsCommand.class, command -> {
                    primes.add(command.getResult());
                    getContext().getLog().info("Received results # {} from {}.", primes.size(), command.getWorker().path());
                    getContext().stop(command.getWorker());
                    if (primes.size() == PRIMES_COUNT) {
                        this.sender.tell(primes);
                        return Behaviors.ignore();
                    }
                    return Behaviors.same();
                })
                .onMessage(InstructionCommand.class, command -> {
                    this.sender = command.getSender();
                    IntStream.rangeClosed(1, PRIMES_COUNT).forEach(i -> {
                        var worker = this.getContext().spawn(WorkerBehavior.create(), "worker-" + i);
                        this.askWorkerForPrime(worker);
                    });
                    return Behaviors.same();
                })
                .onMessage(NoResponseReceivedCommand.class, command -> {
                    getContext().getLog().warn("Retrying worker: {}", command.getWorker().path());
                    this.askWorkerForPrime(command.getWorker());
                    return Behaviors.same();
                })
                .build();
    }    

    private void askWorkerForPrime(ActorRef<WorkerBehavior.Command> worker) {
        getContext().ask(
            Command.class, 
            worker, 
            Duration.ofSeconds(7), 
            me -> new WorkerBehavior.Command("start", me),
            (resp, error) -> {
                if (resp != null) {
                    return resp;
                } else {
                    getContext().getLog().info("Did not receive response from {}", worker.path());
                    return new NoResponseReceivedCommand(worker);
                }
            }
        );
    }
}
