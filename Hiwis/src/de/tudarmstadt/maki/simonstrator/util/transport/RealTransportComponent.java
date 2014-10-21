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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.maki.simonstrator.api.Binder;
import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.Monitor.Level;
import de.tudarmstadt.maki.simonstrator.api.component.ComponentNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetID;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.Serializer;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.SerializerComponent;
import de.tudarmstadt.maki.simonstrator.api.component.transport.ProtocolNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.transport.ServiceNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransInfo;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransportAssetsSerializer;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransportComponent;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransportProtocol;
import de.tudarmstadt.maki.simonstrator.api.component.transport.protocol.TCPMessageBased;
import de.tudarmstadt.maki.simonstrator.api.component.transport.protocol.UDP;
import de.tudarmstadt.maki.simonstrator.api.component.transport.service.FirewallService;
import de.tudarmstadt.maki.simonstrator.api.component.transport.service.MessageQueueService;
import de.tudarmstadt.maki.simonstrator.api.component.transport.service.PiggybackMessageService;
import de.tudarmstadt.maki.simonstrator.api.component.transport.service.TransportService;

/**
 * Use overlay-serializer-interface for compatibility with other runtimes
 * (i.e., shared android + simulator deployment)
 *
 * @author Bjoern Richerzhagen
 * @version 1.0 May 19, 2013
 * @since May 19, 2013
 */
public class RealTransportComponent implements TransportComponent, SerializerComponent {

    private final Host host;

	protected final Map<Integer, Serializer> serializers = new HashMap<Integer, Serializer>();

    private final List<FirewallService> firewalls = new LinkedList<FirewallService>();

	private final Map<Byte, PiggybackMessageService> piggybackers = new LinkedHashMap<Byte, PiggybackMessageService>();

	private MessageQueueService queueService = null;

    /**
     *
     */
    public RealTransportComponent(Host host) {
		/*
		 * Ensure Assets-Serializer as well as Serializer Component is present
		 * in the Binder
		 */
		try {
			Binder.getComponent(TransportAssetsSerializer.class);
		} catch (ComponentNotAvailableException e) {
			Binder.registerComponent(new RealTransportAssetsSerializer());
		}
		try {
			Binder.getComponent(SerializerComponent.class);
		} catch (ComponentNotAvailableException e) {
			Binder.registerComponent((SerializerComponent) this);
		}
        this.host = host;
    }

    @Override
    public void initialize() {
        //
    }

    @Override
    public Host getHost() {
        return host;
    }

    public List<FirewallService> getFirewalls() {
        return firewalls;
    }

    public Map<Byte, PiggybackMessageService> getPiggybackers() {
        return piggybackers;
    }

	/**
	 * We allow only one of those, as otherwise there will be message
	 * duplicates...
	 * 
	 * @return
	 */
	public MessageQueueService getQueueService() {
		return queueService;
	}

    @Override
    public <T extends TransportProtocol> T getProtocol(
			Class<T> protocolInterface, NetID localAddress, int localPort)
            throws ProtocolNotAvailableException {
        /*
		 * Multi-Instantiation is a problem here... an
		 * overlay should ALWAYS cache this object!
		 */
		Monitor.log(RealTransportComponent.class, Level.INFO,
				"Protocol %s on local IP %s and port %s.",
				protocolInterface.getSimpleName(), localAddress, localPort);
        if (protocolInterface.equals(TCPMessageBased.class)) {
			Serializer serializer = serializers.get(localPort);
			if (serializer == null) {
				serializer = new DefaultSerializer();
			}
			return protocolInterface.cast(new RealTransportTCP(host
					.getNetworkComponent().getByNetId(localAddress), localPort,
					serializer, this));
        } else if (protocolInterface.equals(UDP.class)) {
            Serializer serializer = serializers.get(localPort);
            if (serializer == null) {
                serializer = new DefaultSerializer();
            }
            return protocolInterface
                    .cast(new RealTransportUDP(host.getNetworkComponent().getByNetId(
                            localAddress), localPort, serializer, this));
        } else {
            throw new ProtocolNotAvailableException();
        }
    }

    @Override
    public <T extends TransportService> void registerService(Class<T> serviceInterface,
             T serviceImplementation) throws ServiceNotAvailableException {
        if (serviceInterface.equals(FirewallService.class)) {
            firewalls.add((FirewallService) serviceImplementation);
        } else if (serviceInterface.equals(PiggybackMessageService.class)) {
            PiggybackMessageService pms = (PiggybackMessageService) serviceImplementation;
            if (piggybackers.containsKey(pms.getPiggybackServiceID())) {
                throw new AssertionError("Duplicate usage of a serviceID for the Piggybacking-Service");
            }
			piggybackers.put(pms.getPiggybackServiceID(), pms);
		} else if (serviceInterface.equals(MessageQueueService.class)) {
			MessageQueueService mqs = (MessageQueueService) serviceImplementation;
			queueService = mqs;
        } else {
            throw new ServiceNotAvailableException();
        }
    }

    @Override
	public void addSerializer(Serializer serializer, int port) {
        serializers.put(port, serializer);
    }

    /**
     * Trans-Info implementation
     *
     * @author Bjoern Richerzhagen
     * @version 1.0 May 20, 2013
     * @since May 20, 2013
     */
    public static class RealTransInfo implements TransInfo {

        private final NetID net;

		private final int port;

        /**
         *
         */
		public RealTransInfo(NetID net, int port) {
            this.net = net;
            this.port = port;
        }

        @Override
        public int getTransmissionSize() {
            return 8;
        }

        @Override
		public int getPort() {
            return port;
        }

        @Override
        public NetID getNetId() {
            return net;
        }

        @Override
        public String toString() {
            return net.toString() + " port: " + port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RealTransInfo that = (RealTransInfo) o;

            if (port != that.port) return false;
            if (!net.equals(that.net)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = net.hashCode();
            result = 31 * result + (int) port;
            return result;
        }
    }

	@Override
	public <T extends de.tudarmstadt.maki.simonstrator.api.component.transport.TransportService> void registerService(
			Class<T> serviceInterface, T serviceImplementation)
			throws ServiceNotAvailableException {
		// TODO Auto-generated method stub
		
	}

}
