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
package org.wcs.smart.connect.ui.server.configure;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;

/**
 * Interface for allowing plugins to add to the SMART Connect configuration options.
 * This page provides the ui for updating these options.
 * @author Emily
 *
 */
public interface IServerOptionsPanel {
	
	public static final String EXTENSION_ID = "org.wcs.smart.connect.configure.optionpanel"; //$NON-NLS-1$
	
	public static final String DESKTOP_ONLY_MESSAGE = Messages.IServerOptionsPanel_InfoMessage;
	
	
	/**
	 * If the panel is supported for a given 
	 * Conservation Area.
	 * @param ca
	 * @return
	 */
	public boolean isSupported(ConservationArea ca);
	
	/**
	 * The name of the panel
	 * @return
	 */
	public String getName();
	
	/**
	 * Initializes widgets on the panel with information from the
	 * connect server.
	 * 
	 * @param server
	 */
	public void initValues(ConnectServer server);
	
	/**
	 * Updates the connect server object with the items
	 * from the panel
	 * @param server
	 */
	public void updateServer(ConnectServer server);
	
	/**
	 * Function called after the server options are saved to the database.
	 * This allows for events to be fired, or jobs started as required.
	 */
	public void afterSave(ConnectServer server);
	
	/**
	 * 
	 * @return <code>true</code> if all fields are valid, false if an error exists
	 */
	public boolean isValid();
	
	/**
	 * Creates the option panel widgets
	 * @param parent
	 * @param isEditable
	 * @return
	 */
	public Composite createComposite(Composite parent, boolean isEditable);
	
	/**
	 * Adds a change listener that should be fired when an option on the panel
	 * has been modified.
	 * 
	 * @param listener
	 */
	public void addChangeListener(ModifyListener listener);
}
