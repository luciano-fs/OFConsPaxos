package com.example;
import akka.actor.ActorRef;
import java.util.ArrayList;
/**
 * Class containing the processes' references
 */
public class Members {
            public final ArrayList<ActorRef> references;
            public final String data;

    public Members(ArrayList<ActorRef> references) {
	this.references = new ArrayList<ActorRef>();
	for (ActorRef actor : references) {
	    this.references.add(actor);
	}
	
        String s="[ ";
        for (ActorRef a : this.references){
            s+=a.path().name()+" ";
        }
        s+="]";    
        data=s;
    }
            
}
