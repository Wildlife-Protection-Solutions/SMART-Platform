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
package org.wcs.smart.plan.report;

import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.activity.NotificationEvent;
import org.eclipse.birt.report.model.api.command.ContentEvent;
import org.eclipse.birt.report.model.api.core.Listener;
import org.eclipse.birt.report.model.elements.OdaDataSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.birt.BirtSmartUtils;
import org.wcs.smart.birt.ui.IReportEditorManager;
import org.wcs.smart.birt.ui.RCPMultiPageReportEditor;

/**
 * Report plan manager for editing plan template
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class ReportPlanManager implements IReportEditorManager {

	private RCPMultiPageReportEditor editor;
	
	// This listener is a hack to name the
		// smart query datasources with the query name
		// I couldn't figure out how to do this any other way.
		private Listener nameChangeListener = new Listener() {
			@Override
			public void elementChanged(DesignElementHandle focus,
					NotificationEvent ev) {
				if (ev.getEventType() == NotificationEvent.CONTENT_EVENT) {
					// auto link start and end data parameters to report parameters
					ContentEvent ce = (ContentEvent) ev;
					if (ce.getAction() == ContentEvent.ADD
							&& ce.getContent() instanceof OdaDataSet) {
						OdaDataSet ds = (OdaDataSet) ce.getContent();
						OdaDataSetHandle handle = (OdaDataSetHandle) (ds)
								.getHandle(ev.getTarget().getRoot());
						if (ce.getAction() == ContentEvent.ADD
								&& handle.getExtensionID().startsWith("org.wcs.smart")) { //$NON-NLS-1$
							//setup column names correctly
							BirtSmartUtils.updateDatasetConfiguration(handle);
						}
					}
				}
			}

		};
		
	public ReportPlanManager() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		ModuleHandle initialModel = editor.getModel();

		editor.doSaveParent(monitor);
		
		try {
			//on the xml page saving changes the model so we need to reconfigure
			//the listeners
			if (!editor.getModel().equals(initialModel)){
				initialModel.removeListener(nameChangeListener);
				editor.getModel().addListener(nameChangeListener);
			}
			editor.refreshMarkers(editor.getEditorInput());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void dispose() {
		editor.getModel().removeListener(nameChangeListener);
		editor = null;
	}

	@Override
	public void addPages() {
		editor.getModel().addListener(nameChangeListener);
	}
	@Override
	public void init(RCPMultiPageReportEditor editor) {
		this.editor = editor;
	}

}
