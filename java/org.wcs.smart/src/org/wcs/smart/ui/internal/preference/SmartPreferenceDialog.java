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
package org.wcs.smart.ui.internal.preference;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Customized preference dialog that only
 * displays preference pages defined here. 
 * 
 * @author egouge
 *
 */
public class SmartPreferenceDialog extends PreferenceDialog {

	public SmartPreferenceDialog(Shell parentShell) {
		super(parentShell, createPreferenceManager());
	}
	
	/**
	 * Creates a preference manager that
	 * defines which pages to display.
	 * @return
	 */
	private static PreferenceManager createPreferenceManager(){
		PreferenceManager manager = new PreferenceManager();
		PreferenceManager platformPrefManager = PlatformUI.getWorkbench().getPreferenceManager();
		manager.addToRoot(platformPrefManager.find(GpsBabelPreferencePage.ID));
		manager.addToRoot(platformPrefManager.find(PlanConfigurationPreferencePage.ID));
		return manager;
	}
	
}