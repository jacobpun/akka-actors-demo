package com.pk.akkademo;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Behaviors;

public class FirstSimpleBehavior extends AbstractBehavior<String> {

    private FirstSimpleBehavior(ActorContext<String> context) {
        super(context);
    }

    public static Behavior<String> create() {
        return Behaviors.setup(FirstSimpleBehavior::new);
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                    .onMessageEquals("Say Hello", () -> {
                        System.out.println("Hello!!");
                        return this;
                    })
                    .onMessageEquals("Who are you?", () -> {
                        System.out.println(getContext().getSelf().path());
                        return this;
                    })
                    .onMessageEquals("create child", () -> {
                        ActorRef<String> child = getContext().spawn(FirstSimpleBehavior.create(), "ChildActor");
                        child.tell("Who are you?");
                        return this;
                    })
                    .onAnyMessage(message ->{
                        System.out.println("Received Message: " + message);
                        return this;
                    })
                    .build();
    }

    public static void main(String[] args) {
        ActorSystem<String> actorSystem = ActorSystem.create(FirstSimpleBehavior.create(), "FirstActorSystem");
    
        actorSystem.tell("Hello World!!");
        actorSystem.tell("Say Hello");
        actorSystem.tell("Who are you?");
        actorSystem.tell("create child");
    }
}
