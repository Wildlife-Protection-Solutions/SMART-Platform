package org.wcs.smart.incident.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;

public class DeleteIncidentHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		
		if (!(selection instanceof IStructuredSelection)){
			return null;
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
			return null;
		}
		if (!MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Confirm", MessageFormat.format("Are you sure you want to delete the {0} selected incidents? This action cannot be undone", new Object[]{toDelete.size()}))){
			return null;
		}
				
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			Query q=s.createQuery("delete Waypoint w where w.uuid = :wp");
			for (IncidentEditorInput w : toDelete){
				q.setParameter("wp", w.getUuid());
				q.executeUpdate();
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			IncidentPlugIn.displayLog("Error deleteing incidents. " + ex.getMessage(), ex);
		}
		
		//fire events
		for (IncidentEditorInput w : toDelete){
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_DELETED, w);
				
				Waypoint tmp = new Waypoint();
				tmp.setUuid(w.getUuid());
				WaypointEventManager.getInstance().waypointDeleted(tmp);
			}catch (Exception ex){
				IncidentPlugIn.displayLog("Error occurred. " +ex.getMessage(), ex);
			}
		}
	
		return null;
	}
}
