package de.tudarmstadt.maki.simonstrator.util.core;

import org.omnetpp.simkernel.JMessage;

public class TimeoutMsg extends JMessage implements Cloneable {
	public TimeoutMsg(String name){
		super(name);
	}
}
