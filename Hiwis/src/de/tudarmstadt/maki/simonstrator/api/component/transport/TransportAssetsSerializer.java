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

package de.tudarmstadt.maki.simonstrator.api.component.transport;

import java.io.InputStream;
import java.io.OutputStream;

import de.tudarmstadt.maki.simonstrator.api.component.GlobalComponent;

/**
 * Enables access to an assets (de-)serializer for all Transport/Network related
 * objects
 * 
 * @author Bjoern Richerzhagen
 * 
 */
public interface TransportAssetsSerializer extends GlobalComponent {

	/**
	 * Serialize the given transInfo on the given OutputStream
	 * 
	 * @param out
	 * @param msg
	 */
	public void serializeTransInfo(OutputStream out, TransInfo transInfo);

	/**
	 * Recreate a TransInfo from the given input stream
	 * 
	 * @param in
	 * @return
	 */
	public TransInfo createTransInfo(InputStream in);

}
