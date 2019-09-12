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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.NavigationLayer;

/**
 * Exports a cybertracker package.
 * 
 * @author Emily
 *
 */
public interface ICtExportAction {

	public static final String EXT_NAME = "ctexportaction"; //$NON-NLS-1$
	
	/**
	 * Performs the export action
	 */
	public void doAction(List<ICtPackage> ctpackage, List<NavigationLayer> layers, IEclipseContext context);
	
	/**
	 * 
	 * @return the name of the export action
	 */
	public String getName();
	
	/**
	 * 
	 * @return image representing the export action; can return null
	 */
	public Image getIcon();
}
