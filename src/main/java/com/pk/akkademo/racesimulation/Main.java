package com.pk.akkademo.racesimulation;

import akka.actor.typed.ActorSystem;

public class Main {
    public static void main(String[] args) {
        ActorSystem<Controller.Command> actorSystem = ActorSystem.create(Controller.create(), "RacerController");
        actorSystem.tell(new Controller.StartCommand());
    }
}
