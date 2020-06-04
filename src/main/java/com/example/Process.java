package com.example;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Process extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor
    private final int N;//number of processes
    private final int id;//id of current process
    private final double crashProb = 0.2;//probability of crashing
    private int ballot;
    private int proposal;
    private int readBallot;
    private int imposeBallot;
    private int estimate;
    private CoupleState states[];
    private Members processes;//other processes' references
    private boolean faultProne;//if true, the process may die
    private boolean dead;//process has died
    private boolean hold;
    private HashMap<Integer, Integer> ackCounter;
    private HashMap<Integer, Integer> readCounter;
    private int value;

    public Process(int id, int N) {
        this.N = N;
        this.id = id;
        ballot = id - N;
        proposal = -1; //NIL is represented by -1
        readBallot = 0;
        imposeBallot = id - N;
        estimate = -1;
        states = new CoupleState[N];
        for (int i = 0; i < N; i++)
            states[i] = new CoupleState();
        faultProne = false;
        dead = false;
        hold = false;
        ackCounter = new HashMap<Integer, Integer>();
        readCounter = new HashMap<Integer, Integer>();
    }

    private void propose(int v) {
        if (!hold) {
            proposal = v;
            ballot += N;
            for (CoupleState s : states) {
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

    private void firstProposal() {
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
              log.info(toString() + " received Read from Process " + sender.path().name() + " with ballot " + Integer.toString(r.ballot));
              if (readBallot >= r.ballot || imposeBallot >= r.ballot) 
                  sender.tell(new Abort(r.ballot), getSelf());
              else {
                  readBallot = r.ballot;
                  sender.tell(new Gather(r.ballot, imposeBallot, estimate), getSelf()); //Send Gather to pj
              }
          }
          if (message instanceof Abort) {
	      ActorRef sender = getSender();
              log.info(toString() + " received Abort from Process " + sender.path().name());
              propose(value); //Return abort
          }
          if (message instanceof Gather) {
              ActorRef sender = getSender();
              Gather g = (Gather) message;
              log.info(toString() + " received Gather from Process " + sender.path().name() + " with ballot " + Integer.toString(g.ballot));
              int senderID = Integer.parseInt(sender.path().name());
              states[senderID-1] = new CoupleState(g.est, g.estBallot);

              Integer cnt = readCounter.get(g.ballot);
              if (cnt == null)
                  cnt = 0;
              readCounter.put(g.ballot, cnt + 1);

              log.info(toString() + " received Gather from " + Integer.toString(cnt + 1) + " processes" );
              if (cnt + 1 > N/2) {
                  CoupleState highest = new CoupleState();
                  for (int i = 0; i < N; i++)
                      if(states[i].estBallot > highest.estBallot)
                          highest = states[i];
                  if (highest.estBallot > 0)
                      proposal = highest.est;
                  for (int i = 0; i < N; i++)
                      states[i] = new CoupleState();
                  for (ActorRef p : processes.references)
                      p.tell(new Impose(ballot, proposal), getSelf()); // Send IMPOSE to all
              }

          }
          if (message instanceof Impose) {
              ActorRef sender = getSender();
              Impose i = (Impose) message;
              log.info(toString() + " received Impose from Process " + sender.path().name() + " with ballot " + Integer.toString(i.ballot) + " and proposal " + Integer.toString(i.proposal));
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
              log.info(toString() + " received Ack from " + getSender() + " for ballot " + Integer.toString(a.ballot));
              Integer cnt = ackCounter.get(a.ballot);
              if (cnt == null)
                  cnt = 0;
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
	      log.info(toString() + " received HoldMsg");
	      hold = true;
	  }
    }

    private class CoupleState {
        public int est;
        public int estBallot;
        public CoupleState() {
            est = -1; 
            estBallot = 0;
        }
        public CoupleState(int est, int estBallot) {
            this.est = est; 
            this.estBallot = estBallot;
        }
    }
}
