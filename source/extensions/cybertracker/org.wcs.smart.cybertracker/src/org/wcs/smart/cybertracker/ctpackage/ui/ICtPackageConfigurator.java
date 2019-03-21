/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.ctpackage.ui;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.cybertracker.model.ICtPackage;

/**
 * Extension point for adding items to CyberTracker package
 * @author Emily
 *
 */
public interface ICtPackageConfigurator {

	/**
	 * Create gui element for configuring contents
	 * @param parent
	 * @param ctpackage
	 */
	public void createGui(Composite parent, ICtPackage ctpackage, Consumer<String> validate);
	
	/**
	 * Create the details section for describing the settings
	 * @param parent
	 * @param ctpackage
	 * @return
	 */
	public Composite createDetails(Composite parent, ICtPackage ctpackage, List<ICtPackagePropertyProvider> properties);
	
	/**
	 * Saves the changes on the GUI to the database
	 */
	public void save() throws Exception;
}
