package com.pk.akkademo.racesimulation;

import java.io.Serializable;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class Racer extends AbstractBehavior<Racer.Command> {

    public interface Command extends Serializable {}

    @AllArgsConstructor
    @Getter
    public static class StartCommand implements Command {
        private int raceLength;
        private ActorRef<Controller.Command> controller;
    }

    @AllArgsConstructor
    @Getter
    public static class ProgressRequestCommand implements Command {
        private ActorRef<Controller.Command> controller;
    }

    private int progress;
    private long lastStatusTimeMillis;
    private double speedPerSecond;

    private Racer(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Racer::new);
    }


    @Override
    public Receive<Command> createReceive() {
        return this.yetToStart();
    }

    private Receive<Command> yetToStart() {
        return this.newReceiveBuilder()
                    .onMessage(StartCommand.class, command -> {
                        this.progress = 0;
                        this.lastStatusTimeMillis = System.currentTimeMillis();
                        this.speedPerSecond = Math.random();
                        command.getController().tell(new Controller.RacerProgressCommand(this.progress, getContext().getSelf()));                        
                        return running(command.raceLength);
                    })
                    .onMessage(ProgressRequestCommand.class, command -> {
                        command.getController().tell(new Controller.RacerProgressCommand(0, getContext().getSelf()));
                        return Behaviors.same();
                    })
                    .build();
    }

    private Receive<Command> running(int raceLength) {
        return this.newReceiveBuilder()
            .onMessage(ProgressRequestCommand.class, command -> {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - this.lastStatusTimeMillis;
                this.progress = (int) (this.progress + (elapsedTime * this.speedPerSecond));
                if (this.progress > raceLength) {
                    this.progress = raceLength;
                }
                this.lastStatusTimeMillis = currentTime;
                command.getController().tell(new Controller.RacerProgressCommand(this.progress, getContext().getSelf()));
                return this.progress == raceLength? complete(raceLength): Behaviors.same();
            })
            .build();
    }

    private Receive<Command> complete(int raceLength) {
        return this.newReceiveBuilder()
            .onMessage(ProgressRequestCommand.class, command -> {
                command.getController().tell(new Controller.RacerProgressCommand(raceLength, getContext().getSelf()));
                return Behaviors.same();
            })
            .build();
    }
}
