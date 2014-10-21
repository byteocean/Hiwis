/*
 * Copyright (c) 2005-2010 KOM – Multimedia Communications Lab
 *
 * This file is part of Simonstrator.KOM.
 * 
 * Simonstrator.KOM is free software: you can redistribute it and/or modify
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

package de.tudarmstadt.maki.simonstrator.api;

import java.util.List;

import de.tudarmstadt.maki.simonstrator.api.component.ComponentNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.HostComponent;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetworkComponent;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransportComponent;

/**
 * Host Interface for the Simonstrator, handles binding of per-host components
 * 
 * @author Bjoern Richerzhagen
 * 
 */
public interface Host {

	/**
	 * Returns the given component (only per-host components!). Global
	 * components need to be accessed via {@link Global}. Ensure, that you cache
	 * the result of this method locally (i.e., if you query for the transport
	 * layer, save it as an instance variable!), as too frequent calls to this
	 * method might result in degraded performance.
	 * 
	 * @param componentClass
	 * @throws ComponentNotAvailableException
	 *             if the component is not available
	 * @return
	 */
	public <T extends HostComponent> T getComponent(Class<T> componentClass)
			throws ComponentNotAvailableException;

	/**
	 * Register a host component (i.e., a component that is instantiated per
	 * host)
	 * 
	 * @param component
	 */
	public <T extends HostComponent> void registerComponent(T component);

	/**
	 * Returns a list of all components providing the given API
	 * 
	 * @param componentClass
	 * @return
	 * @throws ComponentNotAvailableException
	 *             if no component with the given API is available
	 */
	public <T extends HostComponent> List<T> getComponents(
			Class<T> componentClass) throws ComponentNotAvailableException;

	/*
	 * Convenience Methods
	 */

	/**
	 * Use the {@link TransportComponent} to send and receive messages. Binding
	 * should be done once in the initialize()-Method of the respective overlay
	 * node.
	 * 
	 * @return
	 */
	public TransportComponent getTransportComponent();

	/**
	 * The {@link NetworkComponent} enables access to all phyiscal connectivity
	 * options provided by the host.
	 * 
	 * @return
	 */
	public NetworkComponent getNetworkComponent();

	/**
	 * This method has to return a unique id for a host. Uniqueness even in a
	 * distributed system should be ensured with high probability.
	 * 
	 * @return
	 */
	public long getHostId();

}
