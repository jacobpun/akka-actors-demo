package com.pk.akkademo.racesimulation;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class Controller extends AbstractBehavior <Controller.Command>{
    
    public static interface Command extends Serializable {}

    public static class StartCommand implements Command {}
    
    @AllArgsConstructor
    @Getter
    public static class RacerProgressCommand implements Command {
        private int progress;
        private ActorRef<Racer.Command> racer;
    }

    private static class TimerCommand implements Command {}

    private static final int RACE_LENGTH = 1000;
    private static final String TIMER_KEY = "TIMER_KEY";

    private Controller(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Controller::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return yetToStart();
    }

    private Receive<Command> yetToStart() {
        return newReceiveBuilder()
                .onMessage(StartCommand.class, command -> {
                    IntStream.rangeClosed(1, 2).forEach(i -> {
                        ActorRef<Racer.Command> racer = getContext().spawn(Racer.create(), "Racer-" + i);
                        racer.tell(new Racer.StartCommand(RACE_LENGTH, getContext().getSelf()));
                    });
                    return Behaviors.withTimers(timer -> {
                        timer.startTimerAtFixedRate(
                            TIMER_KEY, 
                            new TimerCommand(), 
                            Duration.ofSeconds(1)
                        );
                        return raceInProgress();
                    });
                })
                .build();
    }

    public Receive<Command> raceInProgress() {
        Map<ActorRef<Racer.Command>, Integer> progress = new HashMap<>();
        return newReceiveBuilder()
                .onMessage(RacerProgressCommand.class, command -> {
                    progress.put(command.racer, command.progress);
                    return Behaviors.same();
                })
                .onMessage(TimerCommand.class, command -> {
                    printRacersProgress(progress);
                    boolean raceFinished = progress.entrySet()
                            .stream()
                            .allMatch(entry -> entry.getValue() == RACE_LENGTH);

                    if (raceFinished) {
                        return complete();
                    }

                    progress.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() < RACE_LENGTH)
                    .forEach(entry -> {
                        entry.getKey().tell(new Racer.ProgressRequestCommand(getContext().getSelf()));
                    });
                    return Behaviors.same();
                    
                })
                .build();
    }

    private void printRacersProgress(Map<ActorRef<Racer.Command>, Integer> progress) {
        progress.entrySet()
            .stream()
            .forEach(entry -> {
                System.out.println(entry.getKey().path() + ": " + entry.getValue());
            });
        System.out.println("\r\n\r\n-----------------------------------------------------------\r\n\r\n");
    }

    private Receive<Command> complete() {
        return newReceiveBuilder()
                .onMessage(TimerCommand.class, command -> 
                    Behaviors.withTimers(timer -> {
                        timer.cancel(TIMER_KEY);
                        return Behaviors.same();
                    })
                )
                .build();
    }
}
