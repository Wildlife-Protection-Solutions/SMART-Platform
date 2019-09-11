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
package org.wcs.smart.cybertracker.navigation.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.cybertracker.model.NavigationTarget;

/**
 * Navigation layer target provider.
 * 
 * @author Emily
 *
 */
public interface INavigationLayerTargetProvider {

	/**
	 * 
	 * @return the name of the target types
	 */
	public String getTypeName();
	
	/**
	 * the image associated with the target provider or null 
	 * @return
	 */
	public Image getImage();
	
	/**
	 * 
	 * @return the wizard pages necessary to collect the 
	 * data required to get targets
	 */
	public List<WizardPage> getPages();
	
	/**
	 * Gets the navigation targets specified by the data from the wizard pages
	 * 
	 * @return
	 */
	public List<NavigationTarget> getTargets(IProgressMonitor monitor);

	/**
	 * 
	 * @return if can finish and get targets
	 */
	public boolean canFinish() ;
}
