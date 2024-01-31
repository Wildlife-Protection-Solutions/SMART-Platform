package org.wcs.smart.connect.event;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.event.EventProcessor;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.WaypointObservation;

public class ConnectEventProcessor implements Runnable {

	private WaypointObservation wo;
	SessionFactory sessionFactory;
	
	public ConnectEventProcessor(WaypointObservation wo, SessionFactory sessionFactory) {
		this.wo = wo;
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public void run() {
		try(Session session = sessionFactory.openSession()){
			wo = session.get(WaypointObservation.class, wo.getUuid());
			if (wo == null) return;
		
			for(EActionEvent event : getEventActions(session, wo.getWaypoint().getConservationArea())) {
				if (!event.isEnabled()) continue;
				try {
					EventProcessor.INSTANCE.processEvent(event, wo);
				}catch (Exception ex) {
					Logger.getLogger(ConnectEventProcessor.class.getName()).log(Level.WARNING, "Unable to process events associated with new waypoint observation: " + ex.getMessage(), ex); //$NON-NLS-1$
				}
			}
		}
	}
	
	private List<EActionEvent> getEventActions(Session session, ConservationArea ca){
		List<EActionEvent> events = QueryFactory.buildQuery(session, EActionEvent.class, 
					new Object[] {"action.conservationArea", ca}).list(); //$NON-NLS-1$
		events.forEach(evt->{
			evt.getAction().getParameters().forEach(p->p.getId().getParameterKey());
			evt.getFilter().getFilterString();
		});
		return events;
	}

}
