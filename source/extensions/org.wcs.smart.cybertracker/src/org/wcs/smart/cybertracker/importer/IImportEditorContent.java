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
package org.wcs.smart.cybertracker.importer;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.cybertracker.model.ICyberTrackerData;

/**
 * Provides functionality and GUI that is specific for type of object 
 * (patrol, survey, ect) that my be imported from CyberTracker.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public interface IImportEditorContent {
	
	/**
	 * Method is called once and is used to create details panel
	 * 
	 * @param parent
	 * @param toolkit
	 * @return
	 */
	public Composite createDetailsComposite(Composite parent, FormToolkit toolkit);

	/**
	 * Fired when selection in table is changed.
	 * Typically used to update content for details panel
	 * 
	 * @param selection
	 */
	public void inputChanged(Object selection);

	/**
	 * @return list of objects that were added
	 */
	public List<ICyberTrackerData> handleAdd(Shell shell, final IStructuredSelection selection);


}
