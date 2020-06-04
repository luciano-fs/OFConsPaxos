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
    private boolean hold;
    private HashMap<Integer, Integer> ackCounter;
    private int value;

    public Process(int id, int N) {
        this.N = N;
        this.id = id;
        ballot = id - N;
        proposal = -1; //NIL is represented by -1
        readBallot = 0;
        imposeBallot = id - N;
        estimate = -1;
        states = new coupleState[N];
        for (s : states)
            s = new coupleState();
        faultProne = false;
        dead = false;
        hold = false;
        ackCounter = new HashMap<Integer, Integer>();
    }

    private void propose(int v) {
        if (!hold) {
            proposal = v;
            ballot += N;
            for (coupleState s : states) {
                s.est = -1;
                s.estBallot = 0;
            }
            for (ActorRef p : processes.references)
                p.tell(new Read(ballot), getSelf());
        }
    }
    
    public String toString() {
        return "Process{" + "id=" + id ;
    }

    private firstProposal() {
        if (Math.random() < 0.5) {
            value = 0;
            propose(value);
        }
        else {
            value = 1;
            propose(value);
        }
    }

    /**
     * Static function creating actor
     */
    public static Props createActor(int ID, int nb) {
        return Props.create(Process.class, () -> {
            return new Process(ID, nb);
        });
    }
    
    public void onReceive(Object message) throws Throwable {
	
          if (dead) return;

          if (faultProne) {
              if (Math.random() < crashProb) {
                  dead = true;
                  log.info(toString() + " has died!!!!");
                  return;
              }
          }


          if (message instanceof Members) {//save the system's info
              Members m = (Members) message;
              processes = m;
              log.info(toString() + " received processes info");
          }
          if (message instanceof OfconsProposerMsg) {//Broadcast read
              log.info(toString() + " received OfConsMSG");
              for (ActorRef p : processes.references)
                  p.tell(new Read(ballot), getSelf()); // Send READ to all
          }
          if (message instanceof Read) {//Broadcast read
              ActorRef sender = getSender();
              Read r = (Read) message;
              log.info(toString() + " received Read from " + sender.path().name() + " with ballot " + Integer.toString(r.ballot));
              if (readBallot >= r.ballot || imposeBallot >= r.ballot) 
                  sender.tell(new Abort(r.ballot), getSelf());
              else {
                  readBallot = r.ballot;
                  sender.tell(new Gather(r.ballot, imposeBallot, estimate), getSelf()); //Send Gather to pj
              }
          }
          if (message instanceof Abort) {
              log.info(toString() + " received Abort from " + getSender());
              propose(value); //Return abort
          }
          if (message instanceof Gather) {
              ActorRef sender = getSender();
              Gather g = (Gather) message;
              log.info(toString() + " received Gather from " + sender.path().name() + " with ballot " + Integer.toString(g.ballot));
              int senderID = Integer.parseInt(sender.path().name());
              states[senderID-1] = new coupleState(g.est, g.estBallot);
              int nbStates = 0;
              coupleState highest = new coupleState();
              for (coupleState s : states) {
                  if (s.est != -1)
                      nbStates ++;
                  if (s.estBallot > highest.estBallot)
                      highest = s;
              }
              if (nbStates > N/2) {
                  if (highest.estBallot > 0)
                      proposal = highest.est;
                  for (coupleState s : states)
                      s = new coupleState();
                  for (ActorRef p : processes.references)
                      p.tell(new Impose(ballot, proposal), getSelf()); // Send IMPOSE to all
              }
          }
          if (message instanceof Impose) {
              ActorRef sender = getSender();
              Impose i = (Impose) message;
              log.info(toString() + " received Impose from " + sender.path().name() + " with ballot " + Integer.toString(i.ballot) + " and proposal " + Integer.toString(i.proposal));
              if (readBallot > i.ballot || imposeBallot > i.ballot)
                  sender.tell(new Abort(i.ballot), getSelf()); // Send abort to pj
              else {
                  estimate = i.proposal;
                  imposeBallot = i.ballot;
                  sender.tell(new Ack(i.ballot), getSelf()); // Send ack to pj
              }
          }
          if (message instanceof Ack) {
              Ack a = (Ack) message;
              log.info(toString() + " received Ack from " + getSender() + " for ballot " + Integeger.toString(a.ballot));
              Integer cnt = ackCounter.get(a.ballot);
              if (cnt == null)
                  ackCounter.put(a.ballot, 1);
              else
                  ackCounter.put(a.ballot, cnt + 1);
              if (cnt + 1 > N/2)
                  for (ActorRef p : processes.references)
                      p.tell(new Decide(proposal), getSelf()); //Send decide to all
          }
          if (message instanceof Decide) {
              Decide d = (Decide) message;
              for (ActorRef p : processes.references)
                  p.tell(new Decide(d.value), getSelf()); // Send decide to all
              log.info(toString() + " decided " + Integer.toString(d.value));
          }

          if (message instanceof CrashMsg) {
              log.info(toString() + " received CrashMsg");
              faultProne = true;
          }
          if (message instanceof LaunchMsg) {
              log.info(toString() + " received LauchMsg");
	      firstProposal();
          }
	  if (message instanceof HoldMsg) {
	      hold = true;
	  }
    }
}

private class coupleState {
    public int est;
    public int estBallot;
    public completeState() {
        est = -1; 
        estBallot = 0;
    }
    public completeState(int est, int estBallot) {
        this.est = est; 
        this.estBallot = estBallot;
    }
}
