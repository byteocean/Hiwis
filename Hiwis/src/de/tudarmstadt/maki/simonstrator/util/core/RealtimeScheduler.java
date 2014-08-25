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
    
    private TimeoutMsg timeoutmsg;
    private long scale;

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
		
        //scheduleIn(1000,null,null,0);
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
        event = new cMessage("timeout");
		
        Handler = new HashMap<Long,ArrayList<SchedulerTask>>();
        System.out.println("Time Scale "+simTime().getScaleExp());
        scale = simTime().getScaleExp() * (-1);
    }
    
	protected void handleMessage(cMessage msg){
		TimeoutMsg message = (TimeoutMsg) JMessage.cast(msg);
		System.out.println("handleMessage called");
    	if(message.isSelfMessage()){
    		
    		System.out.println("Timeout message arrived");
    		long time = simTime().raw();
    		System.out.println("Current time is "+time);
			
			//System.out.println("Current scale is "+(10 ^ scale));
			time = (long) (time / (Math.pow(10,scale)));
    		System.out.println("Current time is "+time);
    		Iterator<Entry<Long, ArrayList<SchedulerTask>>> it = Handler.entrySet().iterator();
    		while(it.hasNext()){
    			Map.Entry<Long, ArrayList<SchedulerTask> > pairs = (Map.Entry<Long, ArrayList<SchedulerTask>>) it.next();
    			System.out.println("Stored time "+pairs.getKey());
    			if(pairs.getKey() <= time){
    				if(pairs.getValue().size() != 1){
    					if(pairs.getValue().get(0).handler != null)
    					{
    						pairs.getValue().get(0).handler.eventOccurred(pairs.getValue().get(0).content, pairs.getValue().get(0).type);
    						pairs.getValue().remove(0);
    					}
    					else
    					{
    						System.out.println("Handler is null");
    					}
    				}
    				else
    				{
    					if(pairs.getValue().get(0).handler != null)
    					{

    						pairs.getValue().get(0).handler.eventOccurred(pairs.getValue().get(0).content, pairs.getValue().get(0).type);
    						Handler.remove(pairs.getKey());
    					}
    					else
    					{
    						System.out.println("Handler is null");
    					}
    				}
    			}
    		}
    	}
    	else
    	{
    		
    		scheduleIn(message.time,message.event,message.cnt,message.type);
    	}
    }
    @Override
    public void scheduleIn(long time, EventHandler handler, Object content, int type) {
    	System.out.println("scheduleIn Called");
        SchedulerTask task = new SchedulerTask(handler, content, type);
        
        if(Handler.containsKey((long) (simTime().raw() / (Math.pow(10,scale))) + time)){
        	Handler.get((long) (simTime().raw() / (Math.pow(10,scale))) + time).add(task);
        }
        else
        {
        	System.out.println("Adding to the list");
        	ArrayList<SchedulerTask> task1 = new ArrayList<SchedulerTask>();
        	task1.add(task);
        	System.out.println("Current time "+simTime().raw());
        	Handler.put((long) (simTime().raw() / (Math.pow(10,scale))) + time, task1);
        	
        }
		//scheduler.schedule(task, time, TimeUnit.MICROSECONDS);
        timeoutmsg = new TimeoutMsg("timeout");
        //timeoutmsg.setTimestamp();
        //scheduleAt(simTime().add(new SimTime(time)),timeoutmsg);
        System.out.println("Schedule At");
        scheduleAt(simTime().add(time), timeoutmsg);
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
