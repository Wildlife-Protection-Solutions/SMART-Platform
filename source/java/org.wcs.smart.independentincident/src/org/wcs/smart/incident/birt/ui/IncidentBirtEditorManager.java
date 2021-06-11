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
package org.wcs.smart.incident.birt.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.birt.ui.IReportEditorManager;
import org.wcs.smart.birt.ui.RCPMultiPageReportEditor;

/**
 * Report plan manager for editing plan template
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class IncidentBirtEditorManager implements IReportEditorManager {

	private RCPMultiPageReportEditor editor;
	
	public IncidentBirtEditorManager() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		editor.doSaveParent(monitor);
	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void dispose() {
	}

	@Override
	public void addPages() {
	}

	@Override
	public void init(RCPMultiPageReportEditor editor) {
		this.editor = editor;
	}

}
