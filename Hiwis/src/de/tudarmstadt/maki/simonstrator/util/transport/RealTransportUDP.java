package de.tudarmstadt.maki.simonstrator.util.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.Monitor.Level;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetInterface;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.Serializer;
import de.tudarmstadt.maki.simonstrator.api.component.transport.protocol.UDP;
import de.tudarmstadt.maki.simonstrator.util.network.RealNetworkComponent;
import de.tudarmstadt.maki.simonstrator.util.network.RealNetworkComponent.RealNetID;

/**
 * Implementation of UDP-Behavior
 * 
 * Created by Bjoern Richerzhagen on 5/28/13.
 */
public class RealTransportUDP extends AbstractRealTransport implements UDP {

	// Used for sending AND for receiving!
	protected DatagramSocket socket;

	/**
	 * Allowed max datagram-size. Default: MTU of underlying net. Has to be set
	 * prior to host creation!
	 */
	protected static int MAX_DATAGRAM_SIZE = -1;

	/**
     *
     */
	public RealTransportUDP(NetInterface net, int port, Serializer serializer,
			RealTransportComponent transportComponent) {
		super(net, port, serializer, transportComponent);
		if (MAX_DATAGRAM_SIZE == -1) {
			MAX_DATAGRAM_SIZE = net.getMTU() * 10;
		}
		try {
			this.socket = new DatagramSocket(port);
			this.socket.setBroadcast(true);
			new MsgReceiverThread().start();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Allows users to modify the maximum datagram size, which might lead to IP
	 * fragmentation. Default Datagram size is the underlying net's MTU.
	 * 
	 * @param size
	 */
	public static void setMaxDatagramSize(int size) {
		if (MAX_DATAGRAM_SIZE != -1) {
			Monitor.log(RealTransportUDP.class, Level.WARN,
					"MaxDatagramSize can only be set ONCE!");
			return;
		}
		MAX_DATAGRAM_SIZE = size;
		Monitor.log(RealTransportUDP.class, Level.WARN,
				"Message size for Datagrams (UDP) was set to %s bytes", size);
	}

	public int getHeaderSize() {
		return 8;
	}

	@Override
	protected void doSendInThread(byte[] msg, RealNetID receiverNet,
			int receiverPort) {
		assert msg.length <= MAX_DATAGRAM_SIZE : "Message size is bigger then MAX_DATAGRAM_SIZE";

		MsgSenderRunnable send = new MsgSenderRunnable(msg, receiverNet,
				receiverPort);
		// (new Thread(send)).start();
		executor.execute(send);
	}

	/**
	 * Runnable sending a message on the UDP-Socket
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0 Aug 20, 2013
	 * @since Aug 20, 2013
	 * 
	 */
	private class MsgSenderRunnable implements Runnable {

		private byte[] data;

		private final RealNetID receiverNet;

		private final int receiverPort;

		public MsgSenderRunnable(byte[] data, RealNetID receiverNet,
				int receiverPort) {
			this.data = new byte[data.length];
			System.arraycopy(data, 0, this.data, 0, data.length);
			this.receiverNet = receiverNet;
			this.receiverPort = receiverPort;
		}

		@Override
		public void run() {
			DatagramPacket p = new DatagramPacket(data, data.length,
					receiverNet.getRealNetAddress(), receiverPort);
			try {
				socket.send(p);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Thread that waits for incoming messages.
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0 Aug 20, 2013
	 * @since Aug 20, 2013
	 * 
	 */
	private class MsgReceiverThread extends Thread {

		@Override
		public void run() {
			try {
				while (true) {
					byte[] data = new byte[MAX_DATAGRAM_SIZE];
					DatagramPacket recMsg = new DatagramPacket(data,
							data.length);
					// blocking
					socket.receive(recMsg);
					byte[] recdata = new byte[recMsg.getLength()];
					System.arraycopy(recMsg.getData(), 0, recdata, 0,
							recMsg.getLength());
					RealNetworkComponent.RealNetID netSender = new RealNetworkComponent.RealNetID(
							recMsg.getAddress());
					doReceive(recdata, netSender, recMsg.getPort());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
