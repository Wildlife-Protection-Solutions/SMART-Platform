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
package org.wcs.smart.cybertracker.navigation;

import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackagePropertyProvider.IPropertyListener;
import org.wcs.smart.cybertracker.model.NavigationLayer;

/**
 * Navigation target property contribution.
 * 
 * @author Emily
 *
 */
public interface INavigationLayerProperty {
	
	public static final String EXT_NAME = "navigationlayerproperty"; //$NON-NLS-1$
	
	/**
	 * Reload property values
	 */
	public void refresh();
	
	/**
	 * 
	 * @return the property name
	 */
	public String getName();
	
	/**
	 * 
	 * @param layer
	 * @return the property value for the given navigationlayer
	 */
	public String getValue(NavigationLayer layer);
	
	/**
	 * Listens for updates to the property values
	 * 
	 * @param listener
	 */
	public void addPropertyUpdatedListener(IPropertyListener listener);
}
