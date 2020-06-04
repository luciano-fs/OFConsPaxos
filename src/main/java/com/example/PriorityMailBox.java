package com.example;
import akka.dispatch.PriorityGenerator;
import akka.dispatch.UnboundedStablePriorityMailbox;
import com.typesafe.config.Config;
import akka.actor.ActorSystem;

public class PriorityMailBox extends UnboundedStablePriorityMailbox {
        public PriorityMailBox(ActorSystem.Settings settings, Config config) {
          // Create a new PriorityGenerator, lower prio means more important
          super(
              new PriorityGenerator() {
                @Override
                public int gen(Object message) {
                  if (message instanceof CrashMsg)
                    return 0; // Crash has highest priority
                  if (message instanceof Members)
                    return 0; // Knowing the system has highest priority
                  if (message instanceof LaunchMsg)
                    return 1; // Lauch comes just after
                  else if (message instanceof Decide)
                    return 1; // Decision is only less priority than knowing the system
                  else if (message instanceof HoldMsg)
                    return 2; // Hold comes first than the other progress messages
                  else if (message instanceof Abort)
                    return 3; // Let the process abort as soon as possible
                  else if (message instanceof Ack)
                    return 4; // From now on, the priority is for messages that will allow faster processes to decide
                  else if (message instanceof Impose)
                    return 5;
                  else if (message instanceof Gather)
                    return 6;
                  else if (message instanceof Read)
                    return 7;
                  else return 8; // By default 
                }
              });
        }
}
