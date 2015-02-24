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
package org.wcs.smart.incident.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Delete incident handler.
 * @author Emily
 *
 */
public class DeleteIncidentHandler{

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object selection, Shell activeShell){
		
		if (selection == null || !(selection instanceof IStructuredSelection)){
			return ;
		}
		IStructuredSelection sel = (IStructuredSelection) selection;
		List<IncidentEditorInput> toDelete = new ArrayList<IncidentEditorInput>();
		
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object waypoint = (Object) iterator.next();
			if (waypoint instanceof Waypoint){
				Waypoint wp = (Waypoint)waypoint;
				toDelete.add(new IncidentEditorInput(wp.getUuid(), wp.getId(),wp.getDateTime()));
			}else if (waypoint instanceof IncidentEditorInput){
				toDelete.add(((IncidentEditorInput)waypoint));
			}
		}
		if (toDelete.size() == 0){
			//nothing to delete
			return;
		}
		if (!MessageDialog.openConfirm(activeShell, Messages.DeleteIncidentHandler_ConfirmLabel, MessageFormat.format(Messages.DeleteIncidentHandler_ConfirmMessage, new Object[]{toDelete.size()}))){
			return;
		}
				
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			Query q=s.createQuery("delete Waypoint w where w.uuid = :wp"); //$NON-NLS-1$
			for (IncidentEditorInput w : toDelete){
				q.setParameter("wp", w.getUuid()); //$NON-NLS-1$
				q.executeUpdate();
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			IncidentPlugIn.displayLog(Messages.DeleteIncidentHandler_Error1 + ex.getMessage(), ex);
		}
		
		//fire events
		for (IncidentEditorInput w : toDelete){
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_DELETED, w);
				
				Waypoint tmp = new Waypoint();
				tmp.setUuid(w.getUuid());
				WaypointEventManager.getInstance().waypointDeleted(tmp);
			}catch (Exception ex){
				IncidentPlugIn.displayLog(Messages.DeleteIncidentHandler_Error2 +ex.getMessage(), ex);
			}
		}
	
		return;
	}
	
	public static class DeleteIncidentHandlerWrapper extends DIHandler<DeleteIncidentHandler>{
		public DeleteIncidentHandlerWrapper(){
			super(DeleteIncidentHandler.class);
		}
	}
}
