package de.tudarmstadt.maki.simonstrator.util.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.omnetpp.simkernel.JMessage;
import org.omnetpp.simkernel.JSimpleModule;
import org.omnetpp.simkernel.SimTime;
import org.omnetpp.simkernel.cMessage;

import de.tudarmstadt.maki.simonstrator.api.EventHandler;
import de.tudarmstadt.maki.simonstrator.api.core.SchedulerComponent;
import de.tudarmstadt.maki.simonstrator.api.core.TimeComponent;
import de.tudarmstadt.maki.simonstrator.util.core.RealtimeScheduler.SchedulerTask;


/**
 * A realtime-scheduler
 * 
 * Created by Bjoern Richerzhagen on 5/27/13.
 */
public class RealtimeScheduler extends JSimpleModule implements SchedulerComponent, TimeComponent {

	private final ScheduledExecutorService scheduler;

    private long startTimestampMicrosecond;

    private boolean initialized = false;
    
    protected cMessage event;
    
    protected HashMap<Long,ArrayList<SchedulerTask>> Handler;
    
    

	/**
	 * 
	 */
	public RealtimeScheduler() {
		scheduler = Executors
				.newSingleThreadScheduledExecutor(new ThreadFactory() {

					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r, "Scheduler-Thread");
					}
				});
		event = new cMessage("timeout");
        Handler = new HashMap<Long,ArrayList<SchedulerTask>>();
	}

    @SuppressWarnings("static-access")
    protected void initialize() {
        if (initialized) {
            return;
        }
        System.out.println("RealtimeScheduler initialized");
        //simTime().setScaleExp(-6);
        startTimestampMicrosecond = simTime().getBaseCPtr();
		// timer = new Timer("RealtimeCoreTimer");
        initialized = true;
        
        
        
    }
    
	protected void handleMessage(cMessage msg){
    	if(msg.sameAs(event)){
    		long time = simTime().getBaseCPtr();
    		Iterator<Entry<Long, ArrayList<SchedulerTask>>> it = Handler.entrySet().iterator();
    		while(it.hasNext()){
    			Map.Entry<Long, ArrayList<SchedulerTask> > pairs = (Map.Entry<Long, ArrayList<SchedulerTask>>) it.next();
    			if(pairs.getKey() <= time){
    				if(pairs.getValue().size() != 1){
    					pairs.getValue().get(0).handler.eventOccurred(pairs.getValue().get(0).content, pairs.getValue().get(0).type);
    					pairs.getValue().remove(0);
    				}
    				else
    				{
    					pairs.getValue().get(0).handler.eventOccurred(pairs.getValue().get(0).content, pairs.getValue().get(0).type);
    					Handler.remove(pairs.getKey());
    				}
    			}
    		}
    	}
    }
    @Override
    public void scheduleIn(long time, EventHandler handler, Object content, int type) {
    	System.out.println("scheduleIn Called");
        SchedulerTask task = new SchedulerTask(handler, content, type);
        if(Handler.containsKey(simTime().getBaseCPtr() + time)){
        	Handler.get(simTime().getBaseCPtr() + time).add(task);
        }
        else
        {
        	ArrayList<SchedulerTask> task1 = new ArrayList<SchedulerTask>();
        	task1.add(task);
        	Handler.put(simTime().getBaseCPtr() + time, task1);
        }
		//scheduler.schedule(task, time, TimeUnit.MICROSECONDS);
        TimeoutMsg timeoutmsg = new TimeoutMsg("timeout");
        //timeoutmsg.setTimestamp();
        //scheduleAt(simTime().add(new SimTime(time)),timeoutmsg);
        scheduleAt(simTime().add(new SimTime(time)), timeoutmsg);
    }

    

    /**
     * A Task
     *
     * @author Bjoern Richerzhagen
     * @version 1.0 May 20, 2013
     * @since May 20, 2013
     */
	protected class SchedulerTask implements Runnable,Cloneable {

        private final EventHandler handler;
        private final Object content;
        private final int type;

        /**
         *
         */
        public SchedulerTask(EventHandler handler, Object content, int type) {
            super();
            this.handler = handler;
            this.content = content;
            this.type = type;
        }

        @Override
        public void run() {
            handler.eventOccurred(content, type);
        }

    }



	@Override
	public long getCurrentTime() {
		// TODO Auto-generated method stub
		return 0;
	}
}
