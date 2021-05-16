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

    @AllArgsConstructor
    @Getter
    public static class RacerFinishedCommand implements Command {
        private ActorRef<Racer.Command> racer;
    }

    private static class TimerCommand implements Command {}

    private static final int RACE_LENGTH = 1000;
    private static final String TIMER_KEY = "TIMER_KEY";
    private static final int RACERS_COUNT = 5;
    private Map<ActorRef<Racer.Command>, Integer> currentPositions;
    private Map<ActorRef<Racer.Command>, Long> timeTakenByRacer;


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
                    IntStream.rangeClosed(1, RACERS_COUNT).forEach(i -> {
                        ActorRef<Racer.Command> racer = getContext().spawn(Racer.create(), "Racer-" + i);
                        this.currentPositions = new HashMap<>();
                        this.timeTakenByRacer = new HashMap<>();
                        racer.tell(new Racer.StartCommand(RACE_LENGTH, getContext().getSelf()));
                    });
                    return Behaviors.withTimers(timer -> {
                        timer.startTimerAtFixedRate(
                            TIMER_KEY, 
                            new TimerCommand(), 
                            Duration.ofSeconds(1)
                        );
                        return raceInProgress(System.currentTimeMillis());
                    });
                })
                .build();
    }

    public Receive<Command> raceInProgress(long raceStartTime) {
        return newReceiveBuilder()
                .onMessage(RacerProgressCommand.class, command -> {
                    this.currentPositions.put(command.racer, command.progress);
                    this.logRacersProgress(this.currentPositions);
                    return Behaviors.same();
                })
                .onMessage(RacerFinishedCommand.class, command -> {
                    this.timeTakenByRacer.put(command.racer, System.currentTimeMillis() - raceStartTime);
                    return this.timeTakenByRacer.size() == RACERS_COUNT? completeRace(): Behaviors.same();
                })
                .onMessage(TimerCommand.class, command -> {
                    this.currentPositions.keySet().forEach(racer -> racer.tell(new Racer.ProgressRequestCommand(getContext().getSelf())));
                    return Behaviors.same();
                })
                .build();
    }

    private Receive<Command> completeRace() {
        return newReceiveBuilder()
                .onMessage(TimerCommand.class, command -> {
                    return Behaviors.withTimers(timers -> {
                        timers.cancelAll();
                        this.currentPositions.keySet().forEach(getContext()::stop);
                        this.logResult();
                        return Behaviors.stopped();
                    });
                })
                .build();
    }

    private void logRacersProgress(Map<ActorRef<Racer.Command>, Integer> currentPositions) {
        currentPositions.forEach((racer, position) -> {
                getContext().getLog().info("{}: {}", racer.path(), position);
            });
        getContext().getLog().info("-----------------------------------------------------------\r\n\r\n");
    }
    
    private void logResult() {
        getContext().getLog().info("FINAL RESULT: ");
        this.timeTakenByRacer.forEach((racer, time) -> {
                getContext().getLog().info("{}: {} milliseconds", racer.path(), time);
            });
        getContext().getLog().info("-----------------------------------------------------------\r\n\r\n");
    }
}
