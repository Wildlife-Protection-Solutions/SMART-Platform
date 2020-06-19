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
package org.wcs.smart.report.internal.ui;

import java.nio.file.Files;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.ui.SmartStyledInputDialog;

/**
 * Handler for creating a new SMART report from existing report
 * 
 * @author egouge
 * @since 7.0.0
 */
@SuppressWarnings("restriction")
public class SaveAsReportHandler {

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, final Shell activeShell){
		if (thisSelection == null) return;
		Report tocopy = null;
		if (thisSelection instanceof Report) {
			tocopy = (Report)thisSelection;
		}else if (thisSelection instanceof IStructuredSelection) {
			Object x = ((IStructuredSelection) thisSelection).getFirstElement();
			if (x instanceof Report) tocopy = (Report)x;
		}
		
		if (tocopy == null) return;
		
		InputDialog id = new SmartStyledInputDialog(activeShell, Messages.SaveAsReportHandler_DialogTitle, Messages.SaveAsReportHandler_DialogMsg, tocopy.getName() + Messages.SaveAsReportHandler_Copy, new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText.strip().isEmpty()) return Messages.SaveAsReportHandler_NameRequiredMsg;
				return null;
			}
		});
		if (id.open() != Window.OK) return;
		
		Report report = new Report();
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			
			tocopy = session.get(Report.class, tocopy.getUuid());
			if (tocopy == null) return;
			
			report.setConservationArea(SmartDB.getCurrentConservationArea());
			String name = id.getValue();
			report.updateName(SmartDB.getCurrentLanguage(), name);
			report.setName(name);
			report.setShared(tocopy.getShared());
			report.setFolder(tocopy.getFolder());
			report.setOwner(tocopy.getOwner());
			
			try {
				
				report.setId(ReportManager.generateReportId(SmartDB.getCurrentConservationArea(), session));
				report.setFilename(ReportManager.generateFilename(report));
				
				session.save(report);
				
				Files.copy(tocopy.getFullPath(), report.getFullPath());
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				ReportPlugIn.displayLog(Messages.NewReportHandler_Error_CouldNotCreateReport + ex.getLocalizedMessage(), ex);
				return;
			}

		}
	
		ReportEventManager.getInstance().fireReportAdded(report);	
		ReportManager.editReport(report);
	}

	
	public static class SaveAsReportHandlerWrapper extends DIHandler<SaveAsReportHandler>{
		public SaveAsReportHandlerWrapper(){
			super(SaveAsReportHandler.class);
		}
	}
}
