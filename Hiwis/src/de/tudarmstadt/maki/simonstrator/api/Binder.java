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

import java.util.LinkedList;
import java.util.List;

import de.tudarmstadt.maki.simonstrator.api.core.ComponentNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.core.GlobalComponent;

/**
 * Components have to register at the binder to be accessible from the other
 * side of the API.
 * 
 * @author Bjoern Richerzhagen
 * 
 */
public class Binder {

	private static final List<de.tudarmstadt.maki.simonstrator.api.core.GlobalComponent> components = new LinkedList<GlobalComponent>();

	/**
	 * Returns the given component (only global components!)
	 * 
	 * @param componentClass
	 * @return
	 */
	public static <T extends GlobalComponent> T getComponent(
			Class<T> componentClass) throws ComponentNotAvailableException {
		for (GlobalComponent component : components) {
			if (componentClass.isInstance(component)) {
				return componentClass.cast(component);
			}
		}
		throw new ComponentNotAvailableException();
	}

	// /**
	// * Returns all global components matching the interface
	// *
	// * @param componentClass
	// * @return
	// */
	// public static <T extends GlobalComponent> List<T> getComponents(
	// Class<T> componentClass) throws ComponentNotAvailableException {
	// List<T> match = new LinkedList<T>();
	// for (GlobalComponent component : components) {
	// if (componentClass.isInstance(component)) {
	// match.add(componentClass.cast(component));
	// }
	// }
	// if (match.isEmpty()) {
	// throw new ComponentNotAvailableException();
	// } else {
	// return match;
	// }
	// }

	/**
	 * Register a global component (i.e., a component that is not instantiated
	 * on a per-host basis)
	 * 
	 * @param component
	 */
	public static <T extends de.tudarmstadt.maki.simonstrator.api.core.GlobalComponent> void registerComponent(T component) {
		if (!components.contains(component)) {
			components.add(component);
		} else {
			throw new UnsupportedOperationException("The component "
					+ component.toString() + " is already registered!");
		}
	}

}
