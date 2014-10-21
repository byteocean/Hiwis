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

package de.tudarmstadt.maki.simonstrator.util.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.Monitor.Level;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetID;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetInterface;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetworkComponent;
import de.tudarmstadt.maki.simonstrator.util.network.RealNetworkInterfaceDetector.RealNetworkInterfaceDetectorCallback;

/**
 * 
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0 May 19, 2013
 * @since May 19, 2013
 */
public class RealNetworkComponent implements NetworkComponent {

    private final List<NetInterface> netInterfaces = new LinkedList<NetInterface>();

    private final Host host;

	private final RealNetworkInterfaceDetector detector;

	/**
	 * A new RealNetworkComponent. via the {@link RealNetworkInterfaceDetector},
	 * a platform implementation can filter and detect its communication
	 * interfaces, if automatic matching based on interface names can not be
	 * performed.
	 * 
	 * @param host
	 * @param detector
	 */
	public RealNetworkComponent(Host host, RealNetworkInterfaceDetector detector) {
        this.host = host;
		this.detector = detector;
    }

    @Override
    public void initialize() {
		NetworkInterface net;
		Enumeration<NetworkInterface> realNets = null;
		if (detector == null) {
			throw new AssertionError(
					"Configure (and register) a NetInterfaceDetector!");
		}
        try {
			realNets = NetworkInterface.getNetworkInterfaces();
			while (realNets.hasMoreElements()) {
				net = realNets.nextElement();
				detector.onUnknownNetworkInterface(net,
						new RealNetworkInterfaceDetectorCallbackImpl(net));
			}

        } catch (SocketException e) {
            throw new AssertionError(
                    "Network Interface Binding was not successful");
        }
    }

	/**
	 * 
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0 Aug 20, 2013
	 * @since Aug 20, 2013
	 * 
	 */
	private class RealNetworkInterfaceDetectorCallbackImpl implements
			RealNetworkInterfaceDetectorCallback {

		private final NetworkInterface net;

		public RealNetworkInterfaceDetectorCallbackImpl(NetworkInterface net) {
			this.net = net;
		}

		@Override
		public void useAs(NetInterfaceName name) {
			NetInterface netInterface = new RealNetworkInterface(host, net,
					name);
			netInterfaces.add(netInterface);
			Monitor.log(RealNetworkComponent.class, Level.INFO,
					"Network component %s bound.", netInterface);
		}
	}

    @Override
    public Host getHost() {
        return host;
    }

    @Override
    public Iterable<NetInterface> getNetworkInterfaces() {
        return netInterfaces;
    }

    @Override
    public NetInterface getByNetId(NetID netID) {
		for (NetInterface net : netInterfaces) {
			if (net.getLocalInetAddress().equals(netID)) {
				return net;
			}
		}
		return null;
    }

    @Override
    public NetInterface getByName(NetInterfaceName name) {
		for (NetInterface net : netInterfaces) {
			if (net.getName().equals(name)) {
				return net;
			}
		}
		return null;
    }

    /**
     * A real network address
     *
     * @author Bjoern Richerzhagen
     * @version 1.0 May 20, 2013
     * @since May 20, 2013
     */
    public static class RealNetID implements NetID {

        private final InetAddress realNetAddress;

		private final boolean isBroadcast;

        /**
         *
         */
        public RealNetID(InetAddress realNetAddress) {
			this(realNetAddress, false);
        }

		public RealNetID(InetAddress realNetAddress, boolean isBroadcast) {
			this.realNetAddress = realNetAddress;
			this.isBroadcast = isBroadcast;
		}

		public boolean isBroadcast() {
			return isBroadcast;
		}

        /**
         * @return the realNetAddress
         */
        public InetAddress getRealNetAddress() {
            return realNetAddress;
        }

        @Override
        public int getTransmissionSize() {
            return 4;
        }

        @Override
        public String toString() {
			return realNetAddress.getHostAddress();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RealNetID realNetID = (RealNetID) o;

            if (!realNetAddress.equals(realNetID.realNetAddress)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return realNetAddress.hashCode();
        }
    }

}
