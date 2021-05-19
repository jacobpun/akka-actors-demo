package com.pk.akkademo.bigprimev2;

import java.math.BigInteger;
import java.time.Duration;
import java.util.SortedSet;
import java.util.concurrent.CompletionStage;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        ActorSystem<ManagerBehavior.Command> actorSystem = ActorSystem.create(ManagerBehavior.create(), "ManagerBehavior");
        CompletionStage<SortedSet<BigInteger>> result = AskPattern.ask(
            actorSystem, 
            (ActorRef<SortedSet<BigInteger>> me) -> new ManagerBehavior.InstructionCommand("start", me), 
            Duration.ofSeconds(60), 
            actorSystem.scheduler()
        );

        result.whenComplete((reply, error) -> {
            if (reply != null) {
                reply.forEach(i -> log.info("Got response: {}.", i));
            } else {
                log.error("Did not get response");
            }
            actorSystem.terminate();
        });
    }
}
