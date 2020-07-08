/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.incident;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.incident.internal.Messages;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Delete incident action.  Applicable for
 * IncidentDataProvider.
 * 
 * @author Emily
 *
 */
public class DeleteIncidentAction implements IQaAction {

	@Override
	public boolean doAction(List<QaError> items) {
		List<QaError> toProcess = new ArrayList<>();
		for (QaError e : items){
			if (e.getDataProviderId().equals(IncidentDataProvider.ID)){
				toProcess.add(e);
			}
		}
		if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), Messages.DeleteIncidentAction_DeleteDialogTitle, MessageFormat.format(Messages.DeleteIncidentAction_DeleteDialogConfirmMsg, toProcess.size()))){
			return false;
		}
		
		List<QaError> deleted = new ArrayList<>();
		List<Waypoint> wpDeleted = new ArrayList<>();
		try(Session s = HibernateManager.openSession()){
			try{
				s.beginTransaction();
				
				for (QaError item : toProcess){
					boolean found = false;
					for (Waypoint wp : wpDeleted){
						if (wp.getUuid().equals(item.getSourceId())){
							//previously deleted
							deleted.add(item);
							found = true;
						}
					}
					if (found) continue;
					
					Waypoint pw = (Waypoint) s.get(Waypoint.class, item.getSourceId());
					
					if (pw == null){
						item.setStatus(QaError.Status.DELETED);
						item.setFixMessage(Messages.DeleteIncidentAction_DeleteErrorWpNotFound + (item.getFixMessage() == null ? "" : " - " + item.getFixMessage()));  //$NON-NLS-1$//$NON-NLS-2$
					}else{
						s.delete(pw);
						deleted.add(item);
						wpDeleted.add(pw);
					}
				}
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				QaPlugIn.displayLog(Messages.DeleteIncidentAction_DeleteError + "\n\n", ex); //$NON-NLS-1$
				return false;
			}
		}

		for (QaError item : deleted){
			item.setFixMessage(Messages.DeleteIncidentAction_DeleteMsg);
			item.setStatus(QaError.Status.DELETED);
		}
		
		//delete filestore and fire events
		wpDeleted.forEach((w)->{
			Path f = Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
					.resolve(IndepedentIncidentSource.FILESTORE_LOC)
					.resolve(UuidUtils.getDirectoryPath(w.getUuid()));
			if (Files.exists(f)){
				try{
					SmartUtils.deleteDirectory(f);
				}catch(Exception ex){
					QaPlugIn.log(Messages.DeleteIncidentAction_FilestoreDeleteError + ex.getMessage(), ex);
				}
			}
			
			WaypointEventManager.getInstance().waypointDeleted(w);
			IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_DELETED, w);
		});
		return true;

	}

	@Override
	public boolean supportsMultiple() {
		return true;
	}

	@Override
	public String getId() {
		return DELETE_ACTION_ID;
	}

	@Override
	public String getName(Locale l) {
		return Messages.DeleteIncidentAction_ActionName;
	}
}
