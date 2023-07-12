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
package org.wcs.smart.cybertracker.export;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.model.ICtPackage;

/**
 * Extension for contributing additional items to the Cybertracker export
 * packages.  This includes the ability to add items to the ui (package export
 * dialog).  This applies to both patrols
 * and surveys.
 * 
 * @author Emily
 *
 */
public interface IPackageUiContribution {
	
	/**
	 * Create the ui component to add to the export dialog.  Can return null
	 * if no ui component.
	 * 
	 * @param parent
	 * @param ctpackage cybertracker package, can be null if not applicable
	 * @param onModified function to call when element is modified 
	 * @param onInitialized function to call when element is initialized (must be called in display thread)
	 * @return
	 */
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified, Runnable onInitilized);
	
	/**
	 * 
	 * @return null if components are valid or an error message if invalid
	 */
	public String isValid();
	
	/**
	 * If this contribution requires it's own tab
	 * @return
	 */
	public default boolean isTab() { return false; }
	
	/**
	 * 
	 * @return the tab name or null if no tab
	 */
	public default String getTabName() { return null; }
	
	/**
	 * Updates the contents of the package with the settings on this page
	 * @param ctpackage
	 */
	public void updatePackage(ICtPackage ctpackage, Session session) throws Exception;
}
