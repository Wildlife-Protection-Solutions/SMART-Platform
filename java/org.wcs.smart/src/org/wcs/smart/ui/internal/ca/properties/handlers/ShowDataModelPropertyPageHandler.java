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
package org.wcs.smart.ui.internal.ca.properties.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.internal.ca.properties.DataModelPropertyPage;
import org.wcs.smart.ui.internal.ca.properties.InitCaDataModelDialog;

/**
 * Handler for displaying data model property dialog.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ShowDataModelPropertyPageHandler extends ShowPropertyPageHandler {
	/**
	 * @param page
	 */
	public ShowDataModelPropertyPageHandler() {
		super(DataModelPropertyPage.class);
	}
	DataModelPropertyPage dialog = null;
	Session loadedSession = null;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		
		dialog = new DataModelPropertyPage( Display.getCurrent().getActiveShell());
		
		DataModelProgressMonitorDialog ppd = new DataModelProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			ppd.run();
		} catch (Exception ex) {
			loadedSession.close();
			SmartPlugIn.displayLog(HandlerUtil.getActiveShell(event), "Could not load data model", ex);
			return null;
		}
		
		DataModel dataModel = ppd.dm;
		if ( dataModel == null || dataModel.getCategories() == null || dataModel.getCategories().size() == 0 ){
			//no datamodel; ask user to init
			InitCaDataModelDialog dd = new InitCaDataModelDialog(loadedSession);
			if (dd.open() == Window.CANCEL){
				loadedSession.close();
				return null;
			}
			dataModel = dd.getDataModel();
		}
		dialog.setDataModel(dataModel);
		dialog.open();
		return null;
	}

	/*
	 * progress monitor dialog for loading data model.
	 */
	class DataModelProgressMonitorDialog extends ProgressMonitorDialog {

		protected DataModel dm;
		
		public DataModelProgressMonitorDialog(Shell shell) {
			super(shell);
		}

		public void run() throws InvocationTargetException,
				InterruptedException {
			super.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Loading data model...", 0);
					loadedSession = dialog.getSession();
					loadedSession.beginTransaction();
					dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), loadedSession);
					loadedSession.getTransaction().rollback();
					monitor.done();					
				}
			});
		}
	}

}
