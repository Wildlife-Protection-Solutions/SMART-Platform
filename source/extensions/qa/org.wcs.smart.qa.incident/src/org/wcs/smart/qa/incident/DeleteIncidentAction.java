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

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.routine.IQaAction;
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
	public void doAction(List<QaError> items) {
		List<QaError> toProcess = new ArrayList<>();
		for (QaError e : items){
			if (e.getDataProviderId().equals(IncidentDataProvider.ID)){
				toProcess.add(e);
			}
		}
		if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected independent incidents?  This action cannot be undone.", toProcess.size()))){
			return;
		}
		
		List<QaError> deleted = new ArrayList<>();
		List<Waypoint> wpDeleted = new ArrayList<>();
		Session s = HibernateManager.openSession();
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
					item.setFixMessage("***Could not delete - Waypoint Not Found*** " + (item.getFixMessage() == null ? "" : " - " + item.getFixMessage()));
				}else{
					s.delete(pw);
					deleted.add(item);
					wpDeleted.add(pw);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			QaPlugIn.displayLog("An error occurred while removing the selected independent incidents.  Refresh QA list and try again, or edit try deleting individual indepdent incidents." + "\n\n", ex);
			return;
		}finally{
			s.close();
		}

		for (QaError item : deleted){
			item.setFixMessage("Incident Deleted");
			item.setStatus(QaError.Status.DELETED);
		}
		
		//delete filestore and fire events
		wpDeleted.forEach((w)->{
			File f = new File(new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation(), IndepedentIncidentSource.FILESTORE_LOC), UuidUtils.getDirectoryPath(w.getUuid()));
			if (f.exists()){
				try{
					FileUtils.forceDelete(f);
				}catch(Exception ex){
					QaPlugIn.log("Could not delete incident filestore path: " + ex.getMessage(), ex);
				}
			}
			
			WaypointEventManager.getInstance().waypointDeleted(w);
			IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_DELETED, w);
		});
		

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
		return "Delete";
	}
	
	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON);
	}
}
