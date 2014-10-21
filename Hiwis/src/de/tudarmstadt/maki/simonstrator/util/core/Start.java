package de.tudarmstadt.maki.simonstrator.util.core;

import org.omnetpp.simkernel.JSimpleModule;
import org.omnetpp.simkernel.Simkernel;
import org.omnetpp.simkernel.cMessage;
import org.omnetpp.simkernel.cModule;

import de.tudarmstadt.maki.simonstrator.api.EventHandler;


public class Start extends JSimpleModule {

	public class TestHandler implements EventHandler{
		
		@Override
		public void eventOccurred(Object content, int type) {
			// TODO Auto-generated method stub
						System.out.println("Event occurred!");	
		}	
	}
	
	public Start() {
		// TODO Auto-generated constructor stub
	}

	protected void initialize(){
		//RealtimeScheduler task = new RealtimeScheduler();
		System.out.println("Start initialized. Calling schedule in");
		//task.scheduleIn(100000, null, null, 0);
		TimeoutMsg timeout = new TimeoutMsg("timeout");
		TestHandler handler=new TestHandler();
		timeout.setEventHandler(handler);	
		timeout.type=19;
		timeout.cnt =  new Object();
		long t = 1000;
		RealtimeScheduler r = (RealtimeScheduler) JSimpleModule.cast(getParentModule().getSubmodule("tic"));
		
		send(timeout,"out");
	}

}
