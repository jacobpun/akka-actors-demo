package com.pk.akkademo.blockchain;

import java.util.stream.IntStream;

import com.pk.akkademo.blockchain.model.Block;
import com.pk.akkademo.blockchain.model.HashResult;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.StashBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import akka.actor.typed.javadsl.Behaviors;

public class ManagerBehavior extends AbstractBehavior<ManagerBehavior.Command> {

    public static interface Command {}

    @Getter
    @AllArgsConstructor
    public static class MineBlockCommand implements Command {
        private Block block;
        private int difficulty;
        private ActorRef<HashResult> sender;
    }

    @AllArgsConstructor
    @Getter
    public static class HashResultCommand implements Command {
        private HashResult result;
    }

    private Block block;
    private int difficulty;
    private ActorRef<HashResult> sender;
    private int currentNounce = 0;
    private StashBuffer<Command> stashBuffer;

    private ManagerBehavior(ActorContext<Command> context, StashBuffer<Command> stashBuffer) {
        super(context);
        this.stashBuffer = stashBuffer;
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> Behaviors.withStash(5, stash -> new ManagerBehavior(context, stash)));
    }

    @Override
    public Receive<Command> createReceive() {
        return idleMessageHandler();
    }

    public Receive<Command> idleMessageHandler() {
        return newReceiveBuilder().onMessage(MineBlockCommand.class, command -> {
            this.block = command.getBlock();
            this.difficulty = command.getDifficulty();
            this.sender = command.getSender();
            IntStream.rangeClosed(0, 9).forEach(i -> this.startWorker());
            return activeMessageHandler();
        }).onSignal(Terminated.class, signal -> {
            return Behaviors.same();
        }).build();
    }

    private Receive<Command> activeMessageHandler() {
        return newReceiveBuilder().onMessage(MineBlockCommand.class, command -> {
            getContext().getLog().info("Delaying mining request as another mining operation is in progress");
            this.stashBuffer.stash(command);
            return Behaviors.same();
        }).onMessage(HashResultCommand.class, command -> {
            getContext().getChildren().forEach(getContext()::stop);
            this.sender.tell(command.result);
            return stashBuffer.unstashAll(idleMessageHandler());
        }).onSignal(Terminated.class, sig -> {
            this.startWorker();
            return Behaviors.same();
        }).build();
    }

    private void startWorker() {
        Behavior<WorkerBehavior.Command> workerBehavior = Behaviors.supervise(WorkerBehavior.create())
                .onFailure(SupervisorStrategy.resume());
        ActorRef<WorkerBehavior.Command> worker = getContext().spawn(workerBehavior, "Worker-" + currentNounce);
        getContext().watch(worker);
        worker.tell(new WorkerBehavior.Command(this.block, this.difficulty, this.currentNounce * 1000,
                getContext().getSelf()));
        this.currentNounce++;
    }
}
