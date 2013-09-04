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
package org.wcs.smart.birt.ui;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface for report edit manager.  The report edit manager
 * is responsible for managin the report edit session.  It 
 * deals with the save, saveAs events.  Additional functionality can be added to the init, 
 * addPages, and dispose events as well.
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public interface IReportEditorManager {

	public static final String EXTENSION_ID="org.wcs.smart.birt.reportEditorManager"; //$NON-NLS-1$
	
	/**
	 * Called when the editor save function is called.
	 * 
	 * @param monitor
	 */
	void doSave(IProgressMonitor monitor);
	
	/**
	 * Called when the editor saveAs function is called.
	 */
	void doSaveAs();
	
	/**
	 * Called before the editor dispose function is called.
	 */
	void dispose();
	
	/**
	 * Called after the editor addPages() function is called
	 */
	void addPages();
	
	/**
	 * Called when the editor init function called 
	 * @param editor
	 */
	void init(RCPMultiPageReportEditor editor);
}
