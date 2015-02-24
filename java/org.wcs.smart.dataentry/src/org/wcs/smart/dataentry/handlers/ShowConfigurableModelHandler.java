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
package org.wcs.smart.dataentry.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.dataentry.dialog.ConfigurableModelPropertyDialog;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.internal.ca.properties.handlers.ShowDataModelPropertyPageHandler;

/**
 * Handler for "Show Configurable Model" command.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ShowConfigurableModelHandler {

	@Execute
	public void execute(Shell activeShell) throws ExecutionException {
		DataModelProgressMonitorDialog pd = new DataModelProgressMonitorDialog(activeShell);
		pd.run();
		if (pd.isEmptyDataModel() && MessageDialog.openQuestion(activeShell, Messages.ShowConfigurableModelHandler_NoDmDialog_Title, Messages.ShowConfigurableModelHandler_NoDmDialog_Message)) {
			ShowDataModelPropertyPageHandler handler = new ShowDataModelPropertyPageHandler();
			handler.execute(activeShell);
		} else {
			Dialog dialog = new ConfigurableModelPropertyDialog(activeShell);
			dialog.open();
		}
	}

	/*
	 * progress monitor dialog for loading data model.
	 */
	private class DataModelProgressMonitorDialog extends ProgressMonitorDialog {

		private DataModel dm;
		
		public DataModelProgressMonitorDialog(Shell shell) {
			super(shell);
		}

		public void run() {
			final Session session = HibernateManager.openSession();
			try {
				super.run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(Messages.ShowConfigurableModelHandler_LoadDataModel, 0);
						session.beginTransaction();
						dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
					}});
			} catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.ShowConfigurableModelHandler_LoadDataModel_Error, ex);
			} finally {
				if (session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
				session.close();
			}
		}
		
		public boolean isEmptyDataModel() {
			return dm == null || dm.getCategories() == null || dm.getCategories().size() == 0;
		}
	}
	
	//E3
	public static class ShowConfigurableModelHandlerWrapper extends DIHandler<ShowConfigurableModelHandler>{
		public ShowConfigurableModelHandlerWrapper(){
			super(ShowConfigurableModelHandler.class);
		}
	}
}
