/*
 * Copyright (c) 2005-2010 KOM – Multimedia Communications Lab
 *
 * This file is part of PeerfactSim.KOM.
 * 
 * PeerfactSim.KOM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * PeerfactSim.KOM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with PeerfactSim.KOM.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.tudarmstadt.maki.simonstrator.util.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import de.tudarmstadt.maki.simonstrator.api.Event;
import de.tudarmstadt.maki.simonstrator.api.EventHandler;
import de.tudarmstadt.maki.simonstrator.api.Message;
import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.Monitor.Level;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetID;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetInterface;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.Serializer;
import de.tudarmstadt.maki.simonstrator.api.component.transport.MessageBasedTransport;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransInfo;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransMessageCallback;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransMessageListener;
import de.tudarmstadt.maki.simonstrator.api.component.transport.service.FirewallService;
import de.tudarmstadt.maki.simonstrator.api.component.transport.service.MessageQueueService;
import de.tudarmstadt.maki.simonstrator.api.component.transport.service.MessageQueueService.MessageQueueCallback;
import de.tudarmstadt.maki.simonstrator.api.component.transport.service.PiggybackMessageService;
import de.tudarmstadt.maki.simonstrator.util.network.RealNetworkComponent;
import de.tudarmstadt.maki.simonstrator.util.network.RealNetworkComponent.RealNetID;

/**
 * Abstract base class for "real" transport protocol implementations, providing
 * some convenience Tasks
 * 
 * @author Bjorn Richerzhagen
 * @version 1.0 Aug 20, 2013
 * @since Aug 20, 2013
 * 
 */
public abstract class AbstractRealTransport implements MessageBasedTransport,
		EventHandler {

	private final static boolean VERBOSE = false;

	protected RealTransportComponent transportComponent;

	protected final NetInterface net;

	protected final int port;

	protected final TransInfo ownTransInfo;

	protected int currentID;

	protected TransMessageListener listener;

	protected final Serializer serializer;

	protected final Map<Integer, TransMessageCallback> openCallbacks = new LinkedHashMap<Integer, TransMessageCallback>();

	protected final Executor executor;

	public static final byte BITMASK_HASPIGGYBACK = 0x02;

	/**
	 * 
	 * @param net
	 * @param port
	 * @param serializer
	 * @param transportComponent
	 */
	public AbstractRealTransport(NetInterface net, int port,
			Serializer serializer, RealTransportComponent transportComponent) {
		this.transportComponent = transportComponent;
		this.net = net;
		this.port = port;
		this.ownTransInfo = new RealTransportComponent.RealTransInfo(
				net.getLocalInetAddress(), port);
		this.serializer = serializer;
		this.executor = Executors.newFixedThreadPool(2, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "RealTransportPoolT");
			}
		});
	}

	@Override
	public void eventOccurred(Object content, int type) {
		/*
		 * Send and wait-timeouts (we hijack the type-field for the commID)
		 */
		TransMessageCallback cb = openCallbacks.remove(type);
		if (cb != null) {
			cb.messageTimeoutOccured(type);
		}
	}

	@Override
	public void setTransportMessageListener(TransMessageListener listener) {
		this.listener = listener;
	}

	/**
	 * Takes care of calling the serializer and sending the message
	 * 
	 * @param msg
	 * @param receiverNet
	 * @param receiverPort
	 * @param commID
	 * @return
	 */
	private int doSend(final Message msg, final NetID receiverNet,
			final int receiverPort, final int commID) {
		/*
		 * Check if the connection is blocked by a firewall component
		 */
		if (!transportComponent.getFirewalls().isEmpty()) {
			for (FirewallService fs : transportComponent.getFirewalls()) {
				if (!fs.allowOutgoingConnection(receiverNet, receiverPort, port)) {
					return commID;
				}
			}
		}

		/*
		 * If we have a MessageQueueService running, intercept the message here.
		 */
		MessageQueueService mqs = transportComponent.getQueueService();
		if (mqs != null) {
			MessageQueueCallback mqc = new MessageQueueCallback() {
				@Override
				public void sendMessage() {
					SerializeAndSend serialize = new SerializeAndSend(msg,
							(RealNetworkComponent.RealNetID) receiverNet,
							receiverPort, commID);
					executor.execute(serialize);
				}

				@Override
				public void dropMessage() {
					// we do nothing.
				}
			};
			mqs.onOutgoingMessage(msg, receiverNet, receiverPort, port, mqc);
			return commID;
		}

		SerializeAndSend serialize = new SerializeAndSend(msg,
				(RealNetworkComponent.RealNetID) receiverNet, receiverPort,
				commID);
		executor.execute(serialize);
		return commID;
	}
	
	/**
	 * Should start a Thread that finally sends the given bytes. It might be a
	 * good idea to copy the given array in a multi-threaded environment...
	 * 
	 * @param msg
	 * @param receiverNet
	 * @param receiverPort
	 */
	abstract protected void doSendInThread(byte[] msg,
			RealNetworkComponent.RealNetID receiverNet, int receiverPort);

	/**
	 * Call this to de-serialize and finally receive a message
	 * 
	 * @param msg
	 * @param senderNet
	 * @param senderPort
	 */
	protected void doReceive(byte[] msg,
			RealNetworkComponent.RealNetID senderNet, int senderPort) {
		UnserializeAndReceive receive = new UnserializeAndReceive(msg,
				senderNet, senderPort);
		// (new Thread(receive)).start();
		executor.execute(receive);
	}

	@Override
	public int send(Message msg, NetID receiverNet, int receiverPort) {
		int commID = currentID++;
		return doSend(msg, receiverNet, receiverPort, commID);
	}

	@Override
	public int sendAndWait(Message msg, NetID receiverNet, int receiverPort,
			TransMessageCallback senderCallback, long timeout) {
		int commID = currentID++;

		// Send and wait-timeout
		openCallbacks.put(commID, senderCallback);
		assert timeout > 0;
		Event.scheduleWithDelay(timeout, this, null, commID);

		return doSend(msg, receiverNet, receiverPort, commID);
	}

	@Override
	public int sendReply(Message reply, NetID receiverNet, int receiverPort,
			int commID) {
		return doSend(reply, receiverNet, receiverPort, commID);
	}

	@Override
	public int getLocalPort() {
		return port;
	}

	@Override
	public NetInterface getNetInterface() {
		return net;
	}

	@Override
	public TransInfo getTransInfo() {
		return ownTransInfo;
	}

	@Override
	public TransInfo getTransInfo(NetID net) {
		return new RealTransportComponent.RealTransInfo(net, port);
	}

	@Override
	public TransInfo getTransInfo(NetID net, int port) {
		return new RealTransportComponent.RealTransInfo(net, port);
	}

	/**
	 * Runnable to de-serialize and then receive a given message.
	 * 
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0 Aug 20, 2013
	 * @since Aug 20, 2013
	 * 
	 */
	private class UnserializeAndReceive implements Runnable {

		private final byte[] msg;

		private final RealNetID senderNet;

		private final int senderPort;

		public UnserializeAndReceive(byte[] msg, RealNetID senderNet,
				int senderPort) {
			this.msg = msg;
			this.senderNet = senderNet;
			this.senderPort = senderPort;
		}

		@Override
		public void run() {
			TransInfo sender = new RealTransportComponent.RealTransInfo(
					senderNet, senderPort);

			boolean ignore = false;
			if (!transportComponent.getFirewalls().isEmpty()) {
				for (FirewallService fs : transportComponent.getFirewalls()) {
					if (!fs.allowIncomingConnection(sender, port)) {
						ignore = true;
						break;
					}
				}
			}

			if (!ignore) {
				ByteArrayInputStream bin = new ByteArrayInputStream(msg);
				// Extract comm-ID
				byte[] commIDBytes = new byte[5];
				bin.read(commIDBytes, 0, 5);
				ByteBuffer bb = ByteBuffer.wrap(commIDBytes);
				int commID = bb.getInt();
				int protocolByte = bb.get();

				if ((protocolByte & BITMASK_HASPIGGYBACK) != 0) {
					// piggybacking mechanism handling
					int numPigs = bin.read();
					for (int pig = 0; pig < numPigs; pig++) {
						byte serviceID = (byte) bin.read();
						PiggybackMessageService ps = transportComponent
								.getPiggybackers().get(serviceID);
						if (ps == null) {
							throw new AssertionError(
									"Unknown piggyback-service!!");
						}
						/*
						 * FIXME: might be a source of concurrency-problems (see
						 * ReceiveEvent below). Maybe alter this lateron.
						 */
						ps.onReceivedPiggybackedMessage(ps.create(bin), sender);
					}
				}

				TransMessageCallback cb = openCallbacks.remove(commID);
				Message msg = serializer.create(bin);
				if (VERBOSE) {
					Monitor.log(RealTransportUDP.class, Level.INFO,
							"Received %s from %s port %s (CommID: %s)", msg,
							senderNet, senderPort, commID);
				}
				ReceiveEvent re = new ReceiveEvent(msg, sender, commID, cb,
						listener);
				Event.scheduleImmediately(re, null, 0);
			}
		}
	}

	/**
	 * Add receive-Events to the queue to get rid of (some) concurrency
	 * problems.
	 * 
	 * @author Bjoern Richerzhagen
	 * 
	 */
	private class ReceiveEvent implements EventHandler {

		private final Message msg;

		private final TransInfo sender;

		private final int commID;

		private final TransMessageCallback cb;

		private final TransMessageListener ls;

		public ReceiveEvent(Message msg, TransInfo sender, int commID,
				TransMessageCallback cb, TransMessageListener ls) {
			this.msg = msg;
			this.sender = sender;
			this.commID = commID;
			this.cb = cb;
			this.ls = ls;
		}

		@Override
		public void eventOccurred(Object content, int type) {
			if (cb != null) {
				cb.receive(msg, sender, commID);
			} else if (ls != null) {
				ls.messageArrived(msg, sender, commID);
			} else {
				throw new AssertionError("No Handler!");
			}
		}
	}

	/**
	 * Runnable that takes care of serialization and then calls
	 * doSendInThread().
	 * 
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0 Aug 20, 2013
	 * @since Aug 20, 2013
	 * 
	 */
	private class SerializeAndSend implements Runnable {

		private final Message msg;

		private final RealNetworkComponent.RealNetID receiverNet;

		private final int receiverPort;

		private final int commID;

		SerializeAndSend(Message msg,
				RealNetworkComponent.RealNetID receiverNet, int receiverPort,
				int commID) {
			this.msg = msg;
			this.receiverNet = receiverNet;
			this.receiverPort = receiverPort;
			this.commID = commID;
		}

		@Override
		public void run() {
			try {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();

				/*
				 * add piggybacking-mechanism and write data to beginning of the
				 * stream
				 */
				int numPigMessages = 0;
				ByteArrayOutputStream pigOut = new ByteArrayOutputStream();
				if (!transportComponent.getPiggybackers().isEmpty()) {
					Message piggyback = null;
					for (Map.Entry<Byte, PiggybackMessageService> entry : transportComponent
							.getPiggybackers().entrySet()) {
						piggyback = entry.getValue().piggybackOnSendMessage(
								receiverNet, receiverPort,
								AbstractRealTransport.this);
						/*
						 * FIXME: check, if AbstractRealTransport supports an
						 * extended implementation!
						 */
						if (piggyback != null) {
							// Service-ID
							numPigMessages++;
							pigOut.write(entry.getKey());
							entry.getValue().serialize(pigOut, piggyback);
						}
					}
					if (numPigMessages > 0) {
						bout.write(numPigMessages);
						pigOut.writeTo(bout);
					}
				}

				serializer.serialize(bout, msg);
				byte[] serialized = bout.toByteArray();
				ByteBuffer bb = ByteBuffer.allocate(5 + serialized.length);
				// Comm ID (4 byte)
				bb.putInt(commID);
				// Protocol Byte (1 byte)
				// 01234567 with 0 = reserved 1,2,3,4,5,7 = reserved, and 6 =
				// piggyback bit
				int protocolByte = 0;
				if (numPigMessages > 0) {
					protocolByte = protocolByte | BITMASK_HASPIGGYBACK;
				}
				bb.put((byte) protocolByte);

				bb.put(serialized);
				byte[] data = bb.array();
				if (VERBOSE) {
					Monitor.log(
							AbstractRealTransport.class,
							Level.INFO,
							"Sending serialized %s with size %s bit real and %s bit virtual to %s port %s. (CommID: %s)",
							msg, data.length, msg.getSize() * 8, receiverNet,
							receiverPort, commID);
				}
				doSendInThread(data, receiverNet, receiverPort);
			} catch (IOException e) {
				Monitor.log(RealTransportUDP.class, Level.ERROR,
						"Send failed - Socket dead?");
				e.printStackTrace();
			}
		}
	}

}
