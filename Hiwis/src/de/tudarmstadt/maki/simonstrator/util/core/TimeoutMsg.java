package de.tudarmstadt.maki.simonstrator.util.core;

import org.omnetpp.simkernel.JMessage;

import de.tudarmstadt.maki.simonstrator.api.EventHandler;

public class TimeoutMsg extends JMessage implements Cloneable {
	
	public EventHandler event;
	public long time;
	public Object cnt;
	public int type;
	public TimeoutMsg(EventHandler evnt,long time,Object c,int t){
		this.event = evnt;
		this.time = time;
		this.cnt = c;
		this.type = t;
	}
	public TimeoutMsg(String name){
		super(name);
	}
}
