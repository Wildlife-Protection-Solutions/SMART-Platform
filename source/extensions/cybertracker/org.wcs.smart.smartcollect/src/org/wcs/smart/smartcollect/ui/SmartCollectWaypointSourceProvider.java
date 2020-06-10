package org.wcs.smart.smartcollect.ui;

import java.util.UUID;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.ui.OpenIncidentHandler;
import org.wcs.smart.observation.model.IWaypointSourceUiProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;

public class SmartCollectWaypointSourceProvider implements IWaypointSourceUiProvider {

	public SmartCollectWaypointSourceProvider() {
	}

	@Override
	public void findAndShow(UUID waypointUuid) {
		Waypoint pw = null;
		try(Session s = HibernateManager.openSession()){
			pw = s.get(Waypoint.class, waypointUuid);
			if (pw == null){
				MessageDialog.openError(Display.getDefault().getActiveShell(), ERROR_STR, "SMART Collect waypoint not found.");
				return;
			}
		}
			
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		ctx.set(OpenIncidentHandler.UUID_PARAM, waypointUuid);
		ctx.set(OpenIncidentHandler.SOURCE_PARAM, SmartCollectWaypointSource.KEY);
		ContextInjectionFactory.invoke(new OpenIncidentHandler(), Execute.class, ctx.getActiveLeaf());
	}

}
