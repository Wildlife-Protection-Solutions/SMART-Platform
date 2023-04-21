package org.wcs.smart.incident.ui;

import java.util.UUID;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.json.IncidentJsonFeatureProcessor;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.model.IWaypointSourceProvider;
import org.wcs.smart.observation.model.Waypoint;

public abstract class AbstractIncidentSourceUiProvider implements IWaypointSourceProvider {

	protected abstract String getSourceKey();
	
	@Override
	public void findAndShow(UUID waypointUuid) {
		Waypoint pw = null;
		try (Session s = HibernateManager.openSession()) {
			pw = s.get(Waypoint.class, waypointUuid);
			if (pw == null) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), ERROR_STR,
						Messages.IndIncidentSourceUiProvider_WaypointNotFound);
				return;
			}
		}

		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		ctx = ctx.getActiveChild().createChild();
		ctx.set(OpenIncidentHandler.UUID_PARAM, waypointUuid);
		ctx.set(OpenIncidentHandler.SOURCE_PARAM, getSourceKey());
		ContextInjectionFactory.invoke(new OpenIncidentHandler(), Execute.class, ctx);

	}

	@Override
	public void postProcessJsonData(IJsonFeatureProcessor processor) {
		if (!(processor instanceof IncidentJsonFeatureProcessor))
			return;

		//fire event for new features
		IncidentJsonFeatureProcessor pp = (IncidentJsonFeatureProcessor) processor;
		IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_ADDED, 
				pp.getCreatedFeatures(WaypointSourceEngine.INSTANCE.getSource(getSourceKey())));
	}
	
}
