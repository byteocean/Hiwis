package de.tudarmstadt.maki.simonstrator.util.core;

import org.omnetpp.simkernel.JSimpleModule;
import org.omnetpp.simkernel.Simkernel;
import org.omnetpp.simkernel.cModule;

public class Start extends JSimpleModule {

	public Start() {
		// TODO Auto-generated constructor stub
	}

	protected void initialize(){
		//RealtimeScheduler task = new RealtimeScheduler();
		System.out.println("Start initialized. Calling schedule in");
		//task.scheduleIn(100000, null, null, 0);
		TimeoutMsg timeout = new TimeoutMsg("timeout");
		send(timeout,"out");
	}

}
