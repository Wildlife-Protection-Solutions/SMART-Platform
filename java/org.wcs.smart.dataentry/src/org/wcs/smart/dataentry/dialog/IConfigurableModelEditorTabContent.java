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
package org.wcs.smart.dataentry.dialog;

import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Anyone who want to provide additional tab to {@link ConfigurableModel} editor
 * should implement this interface and register it as extension point "".
 * 
 * @author elitvin
 * @since 4.0.0
 */
public interface IConfigurableModelEditorTabContent {
	
	/**
	 * Setter will be executed ones before other actions.
	 * It is used to inject  a reference to the main dialog. Dialog can later be used to obtain model, session or other required components for the tab.
	 * @param dialog
	 */
	public void setDialog(ConfigurableModelEditDialog dialog);

	/**
	 * Name of the tab
	 * @return
	 */
	public String getTabName();
	
	/**
	 * Method is responsible for creating tab content
	 * @param parent
	 * @return
	 */
	public Composite createTabContent(Composite parent);

	/**
	 * Method is responsible for saving any additional data (if needed) to database.
	 * Configurable model will be saved by hosting dialog. 
	 */
	public void performSave(Session s);

	/**
	 * Method is called after save operation successfully completed.
	 * This may be required by some tabs to update their state.
	 */
	public default void postSave() {
		//nothing
	}

	/**
	 * Index that identifies the position of the tab.
	 * @return index that indicates in which order tabs are displayed
	 */
	public int getTabIndex();
	
}
