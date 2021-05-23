package com.pk.akkademo.blockchain;

import com.pk.akkademo.blockchain.ManagerBehavior.Command;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Routers;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.PoolRouter;

public class MiningSystemBehavior extends AbstractBehavior<ManagerBehavior.Command>{

    private ActorRef<ManagerBehavior.Command> managers;

    public MiningSystemBehavior(ActorContext<Command> context) {
        super(context);
        PoolRouter<ManagerBehavior.Command> poolRouter = Routers.pool(5, ManagerBehavior.create());
        managers = getContext().spawn(poolRouter, "managers");
    }

    public static Behavior<ManagerBehavior.Command> create() {
        return Behaviors.setup(MiningSystemBehavior::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onAnyMessage(command -> {
                    managers.tell(command);
                    return Behaviors.same();
                })
                .build();
    }
    
}
