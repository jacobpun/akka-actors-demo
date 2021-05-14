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
                        command.getController().tell(new Controller.RacerProgressCommand(0, getContext().getSelf()));                        
                        return running(command.raceLength, 0, System.currentTimeMillis());
                    })
                    .onMessage(ProgressRequestCommand.class, command -> {
                        command.getController().tell(new Controller.RacerProgressCommand(0, getContext().getSelf()));
                        return Behaviors.same();
                    })
                    .build();
    }

    private Receive<Command> running(int raceLength, int progress, long lastStatusTimeMillis) {
        double speedPerSecond = Math.random();
        return this.newReceiveBuilder()
            .onMessage(ProgressRequestCommand.class, command -> {
                int newProgress = progress;
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - lastStatusTimeMillis;
                newProgress = (int) (newProgress + (elapsedTime * speedPerSecond));
                if (newProgress > raceLength) {
                    newProgress = raceLength;
                }
                command.getController().tell(new Controller.RacerProgressCommand(newProgress, getContext().getSelf()));
                return newProgress == raceLength? complete(raceLength): running(raceLength, newProgress, currentTime);
            })
            .build();
    }

    private Receive<Command> complete(int raceLength) {
        return this.newReceiveBuilder()
            .onMessage(ProgressRequestCommand.class, command -> {
                command.getController().tell(new Controller.RacerProgressCommand(raceLength, getContext().getSelf()));
                command.getController().tell(new Controller.RacerFinishedCommand(getContext().getSelf()));
                return Behaviors.ignore();
            })
            .build();
    }
}
