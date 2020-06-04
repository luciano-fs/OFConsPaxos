package com.example;
import akka.actor.Actor.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.*;
import java.util.stream.Stream;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    public static final int f_array[] = {1, 4, 49};
    public static int N_array[] = {3, 10, 100};


    public static void main(String[] args) throws InterruptedException {

	for(int iteration = 0; iteration < 3; iteration++) {
	    int N = N_array[iteration];
	    int f = f_array[iteration];

	    for(int timeout = 500; timeout != 2500; timeout += 500) {
		// Instantiate an actor system
		final ActorSystem system = ActorSystem.create("system");
		system.log().info("System started with N=" + N );

		ArrayList<ActorRef> references = new ArrayList<ActorRef>();
		ArrayList<IsRunning> status = new ArrayList<IsRunning>();

		for (int i = 1; i <= N; i++) {
		    IsRunning s = new IsRunning();
		    // Instantiate processes
		    final ActorRef a = system.actorOf(Process.createActor(i, N, s).withDispatcher("prio-dispatcher"), "" + i);
		    references.add(a);
		    status.add(s);
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

		Thread.sleep(100);

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
	
		for (int i = 0; i < N; i++) {
		    while(status.get(i).get()) Thread.sleep(100);
		}

		system.terminate();
	    }

	}
    }
}
