package de.tudarmstadt.maki.simonstrator.util.core;



import org.omnetpp.simkernel.JMessage;
import org.omnetpp.simkernel.cMessage;

import de.tudarmstadt.maki.simonstrator.api.EventHandler;

public class TimeoutMsg extends JMessage implements Cloneable {
	
	protected EventHandler eventHandler;
	protected int type;
	protected Object cnt;
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getContent() {
		return cnt;
	}

	public void setContent(Object cnt) {
		this.cnt = cnt;
	}

	public TimeoutMsg(String name){
		super(name);
	}
	
	public void setEventHandler (EventHandler eventHandler){
		this.eventHandler=eventHandler;		
	}
	
	 public static TimeoutMsg cast(cMessage o) {
	        return (TimeoutMsg)JMessage.cast(o);
	    }
}
