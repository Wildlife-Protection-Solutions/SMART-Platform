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
package org.wcs.smart.connect.dataqueue.ui;

import java.util.HashMap;

import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.connect.dataqueue.internal.Messages;
import org.wcs.smart.connect.dataqueue.model.DataQueueProcessingOption;

/**
 * Interface for allowing plugins to add to the SMART Connect DataQueue processing
 * configuration options.
 * This page provides the ui for updating these options.
 * @author Emily
 *
 */
public interface IProcessingOptionPanel {
	
	public static final String EXTENSION_ID = "org.wcs.smart.connect.dataqueue.processor.option"; //$NON-NLS-1$
	
	public static final String DESKTOP_ONLY_MESSAGE = Messages.IProcessingOptionPanel_InfoMessage;
	
	/**
	 * The name of the panel
	 * @return
	 */
	public String getName();
	
	/**
	 * Initializes widgets on the panel.
	 * 
	 * @param server
	 */
	public void initValues(HashMap<String, DataQueueProcessingOption> options);
	
	/**
	 * Updates the database options represented by this panel.
	 * @param session database session in active transaction
	 */
	public void update(Session session);

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
	public Composite createComposite(Composite parent);
	
	/**
	 * Adds a change listener that should be fired when an option on the panel
	 * has been modified.
	 * 
	 * @param listener
	 */
	public void addChangeListener(ModifyListener listener);
	
	
	public static interface ModifyListener{
		public void widgetChanged();
	}
}
