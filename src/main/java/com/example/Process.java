package com.example;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Process extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor
    private final int N;//number of processes
    private final int id;//id of current process
    private final float crashProb = 0.2;//probability of crashing
    private int ballot;
    private int proposal;
    private int readBallot;
    private int imposeBallot;
    private int estimate;
    private coupleState states[];
    private Members processes;//other processes' references
    private boolean faultProne;//if true, the process may die
    private boolean dead;//process has died

    public Process(int ID, int nb) {
        N = nb;
        id = ID;
        ballot = id - N;
        proposal = -1;
        readBallot = 0;
        imposeBallot = id - N;
        estimate = -1;
        states = new coupleState[N];
        faultProne = false;
        dead = false;
        for (s : states)
            s = new coupleState();
    }

    private propose(v) {
        proposal = v;
        ballot += N;
        for (s : states) {
            s.est = -1;
            s.estBallot = 0;
        }
        for (ActorRef p : processes.references)
            p.tell(new Read(ballot), getSelf());
    }
    
    public String toString() {
        return "Process{" + "id=" + id ;
    }

    private 

    /**
     * Static function creating actor
     */
    public static Props createActor(int ID, int nb) {
        return Props.create(Process.class, () -> {
            return new Process(ID, nb);
        });
    }
    
    public void onReceive(Object message) throws Throwable {
	
	  if (faultProne) {
              if (Math.random() < crashProb) {
		  log.info("p" + self().path().name() + " has died!!!!");
		  dead = true;
	      }
	  }

	  if (dead) return;
    	
          if (message instanceof Members) {//save the system's info
              Members m = (Members) message;
              processes = m;
              log.info("p" + self().path().name() + " received processes info");
          }
          if (message instanceof OfconsProposerMsg) {//Broadcast read
              log.info("p" + self().path().name() + " received OfConsMSG");
              for (ActorRef p : processes.references)
                  p.tell(new Read(), getSelf());
          }
          if (message instanceof Read) {//Broadcast read
              ActorRef 
              Read r = (Read) message;
              if (readBallot >= r.ballot || imposeBallot >= r.ballot) 

          }
	  if (message instanceof CrashMsg) {
	      log.info("p" + self().path().name() + " received CrashMsg");
	      faultProne = true;
	  }
	  if (message instanceof LaunchMsg) {
	      log.info("p" + self().path().name() + " received LauchMsg");
              if (Math.random() < 0.5) {
		  propose(0);
	      }
	      else {
		  propose(1);
	      }
	  }
	  
    }
}

private class coupleState {
    public int est;
    public int estBallot;
    public completeState() {
        est = -1; //NIL is represented by -1
        estBallot = 0;
}
