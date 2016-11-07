/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.query;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IStateListener;
import org.eclipse.core.commands.State;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.wcs.smart.query.common.model.QueryFilterConfiguration;

/**
 * Provides information about query filter settings.
 * 
 * @author elitvin
 * @since 4.1.0
 */
public class QueryFilterConfigManager {
	
	private QueryFilterConfiguration config;

	private final static QueryFilterConfigManager INSTANCE = new QueryFilterConfigManager();
	
	//registered listeners
	private Collection<IConfigurationChangeListener> listeners = new HashSet<>();
	
	public QueryFilterConfigManager() {
		State state = getShowInavtiveItemsState();
		config = new QueryFilterConfiguration();
		config.setShowInactiveItems(Boolean.TRUE.equals(state.getValue()));
		
		state.addListener(new IStateListener() {
			@Override
			public void handleStateChange(State st, Object oldValue) {
				config.setShowInactiveItems(Boolean.TRUE.equals(st.getValue()));
				if (oldValue == null || !oldValue.equals(config.isShowInactiveItems())) {
					fireListeners();
				}
			}
		});
	}
	
	public static final State getShowInavtiveItemsState() {
		ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = service.getCommand("org.wcs.smart.query.showinactive"); //$NON-NLS-1$
		State state = command.getState("org.wcs.smart.query.state.showinactive"); //$NON-NLS-1$
		return state;
	}
	
	/**
	 * 
	 * @return the query filter configuration manager instance
	 */
	public static QueryFilterConfigManager getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Adds a listener for a configuration change event
	 * 
	 * @param listener listener
	 */
	public void addChangeListener(IConfigurationChangeListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Removes a listener for a configuration change event
	 * 
	 * @param listener listener
	 */
	public void removeChangeListener(IConfigurationChangeListener listener) {
		listeners.remove(listener);
	}
	
	protected void fireListeners() {
		for (IConfigurationChangeListener lst : listeners) {
			lst.configurationChanged(config);
		}
	}
	
	public QueryFilterConfiguration getCurrentConfig() {
		return config;
	}
	
	/**
	 * Interface to track configuration change events.
	 * 
	 * @author elitvin
	 * @since 4.1.0
	 */
	public static interface IConfigurationChangeListener {
		public void configurationChanged(QueryFilterConfiguration config);
	}

}