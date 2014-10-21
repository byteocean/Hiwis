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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import de.tudarmstadt.maki.simonstrator.api.component.network.ComponentNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.network.core.MonitorComponent;
import de.tudarmstadt.maki.simonstrator.api.component.network.core.MonitorComponent.Analyzer;
import de.tudarmstadt.maki.simonstrator.api.component.network.core.MonitorComponent.AnalyzerNotAvailableException;

/**
 * Bridge to local Monitoring on each Runtime, providing Analyzers in a
 * Proxy-Fashion as well as logging capabilities.
 * 
 * @author Bjoern Richerzhagen
 * 
 */
public final class Monitor {

	public enum Level {
		INFO, WARN, ERROR, DEBUG
	}

	private static MonitorComponent monitor = null;

	private static MonitorComponent getMonitor() {
		if (monitor == null) {
			try {
				monitor = Binder.getComponent(MonitorComponent.class);
			} catch (ComponentNotAvailableException e) {
				throw new AssertionError();
			}
		}
		return monitor;
	}

	/**
	 * Logging, using printf semantics. <strong>For performance reasons: do
	 * never build a complete string but instead use the placeholder semantics.
	 * This way, strings are only built if logging is enabled.</strong>
	 * 
	 * @param subject
	 * @param level
	 * @param message
	 *            should be a single string using placeholders
	 * @param data
	 *            data objects (toString will be called on non-primitives)
	 */
	public static void log(Class<?> subject, Level level, String message,
			Object... data) {
		getMonitor().log(subject, level, message, data);
	}

	/**
	 * Retrieve an analyzing-Interface (transparent multiplexing is performed,
	 * if multiple analyzers with the same interface are registered)
	 * 
	 * @param analyzerType
	 * @return
	 * @throws AnalyzerNotAvailableException
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Analyzer> A get(Class<A> analyzerType)
			throws AnalyzerNotAvailableException {
		List<A> analyzers = getMonitor().getAnalyzers(analyzerType);
		if (analyzers.size() == 1) {
			// Do not depend on proxy
			return analyzers.iterator().next();
		}
		// create proxy
		Class<?>[] proxyInterfaces = new Class[]{analyzerType};
		A proxy = (A) Proxy.newProxyInstance(analyzerType.getClassLoader(),
				proxyInterfaces, new Delegator<A>(analyzers));
		return proxy;
	}

	/**
	 * Register a new analyzer
	 * 
	 * @param analyzer
	 */
	public static <A extends Analyzer> void registerAnalyzer(A analyzer) {
		getMonitor().registerAnalyzer(analyzer);
	}

	/**
	 * Transparent Proxy for calls to the analyzers
	 * 
	 * @author bjoern
	 * 
	 */
	protected static class Delegator<A extends Analyzer> implements
			InvocationHandler {

		private final List<A> analyzers;

		public Delegator(List<A> analyzers) {
			this.analyzers = analyzers;
		}

		@Override
		public Object invoke(Object proxy, Method m, Object[] args)
				throws Throwable {
			for (A analyzer : analyzers) {
				m.invoke(analyzer, args);
			}
			return null;
		}

	}

}
