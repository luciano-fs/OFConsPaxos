package com.example;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.*;
import java.util.stream.Stream;

public class Main {

    public static final int f = 2;
    public static final long timeout = 300000;

    public static int N = 10;


    public static void main(String[] args) throws InterruptedException {

        // Instantiate an actor system
        final ActorSystem system = ActorSystem.create("system");
        system.log().info("System started with N=" + N );

        ArrayList<ActorRef> references = new ArrayList<>();

        for (int i = 1; i < N; i++) {
            // Instantiate processes
            final ActorRef a = system.actorOf(Process.createActor(i, N), "" + i);
            references.add(a);
        }

        //give each process a view of all the other processes
        Members m = new Members(references);
        for (ActorRef actor : references) {
            actor.tell(m, ActorRef.noSender());
        }

	CrashMsg crash = new CrashMsg();
	
        Collections.shuffle(references);
	for (int i = 0; i < f; i++) {
	    references.get(i).tell(crash, ActorRef.noSender());
	}

	LaunchMsg launch = new LaunchMsg();

	for (ActorRef actor : references) {
	    actor.tell(launch, ActorRef.noSender());
	}

	Thread.sleep(timeout);
	
        int leaderNumber = ThreadLocalRandom.current().nextInt(f, references.size());
	ActorRef leader = references.get(leaderNumber);
	HoldMsg hold = new HoldMsg();
        for (ActorRef actor : references) {
	    if (actor != leader) {
		actor.tell(hold, ActorRef.noSender());
	    }
	}
    }
}
