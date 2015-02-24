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
package org.wcs.smart.report.internal.ui;

import java.util.HashSet;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;

/**
 * Handler for creating a new report folder.
 * @author egouge
 * @since 1.0.0
 */
public class NewFolderHandler {

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection){
		if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) || ((IStructuredSelection)thisSelection).isEmpty() ){
			return;
		}
		
		Object obj = ((IStructuredSelection)thisSelection).getFirstElement();
		final Object o = obj;
		if (o instanceof ReportFolder || o instanceof RootReportFolder){
			Job job = new Job(Messages.NewFolderHandler_JobName) {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					ReportFolder newFolder = null;
					
					newFolder = new ReportFolder();
					Label lbl = new Label();
					lbl.setLanguage(SmartDB.getCurrentLanguage());
					lbl.setValue(Messages.NewFolderHandler_DefaultNewFolderName);
					lbl.setElement(newFolder);
					newFolder.setNames(new HashSet<Label>());
					newFolder.getNames().add(lbl);
					newFolder.setName(lbl.getValue());
					
					if (!SmartDB.getCurrentLanguage().equals(SmartDB.getCurrentConservationArea().getDefaultLanguage())){
						newFolder.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), Messages.NewFolderHandler_DefaultNewFolderName);
					}
					
					
					newFolder.setConservationArea(SmartDB.getCurrentConservationArea());
					
					if (o instanceof ReportFolder){
						ReportFolder parent = (ReportFolder)o;
						newFolder.setEmployee(parent.getEmployee());
						newFolder.setParentFolder(parent);
						parent.getChildren().add(newFolder);
					}else if (o instanceof RootReportFolder){
						if (o == RootReportFolder.CA_ROOT_FOLDER){
							newFolder.setEmployee(null);
						}else{
							newFolder.setEmployee(SmartDB.getCurrentEmployee());
						}
					}
					if (newFolder != null){
						Session s = HibernateManager.openSession();
						try{
							s.beginTransaction();
							s.saveOrUpdate(newFolder);
							s.getTransaction().commit();
						}catch (Exception ex){
							ReportPlugIn.displayLog(Messages.NewFolderHandler_Error_CouldNotAddFolder + ex.getLocalizedMessage(), ex);
							s.getTransaction().rollback();
							return Status.OK_STATUS;
						}finally{
							s.close();
						}
						
						ReportEventManager.getInstance().fireReportFolderAdded(newFolder);
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}
	
	public static class NewFolderHandlerWrapper extends DIHandler<NewFolderHandler>{
		public NewFolderHandlerWrapper(){
			super(NewFolderHandler.class);
		}
	}
}