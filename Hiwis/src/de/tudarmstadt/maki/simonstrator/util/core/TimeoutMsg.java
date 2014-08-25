package de.tudarmstadt.maki.simonstrator.util.core;

import org.omnetpp.simkernel.JMessage;

import de.tudarmstadt.maki.simonstrator.api.EventHandler;

public class TimeoutMsg extends JMessage implements Cloneable {
	
	
	public TimeoutMsg(String name){
		super(name);
	}
}
