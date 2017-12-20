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
package org.wcs.smart.asset.ui.views.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.ui.data.AssetDataPanel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Page for reviewing results
 * 
 * @author Emily
 *
 */
public class DataReviewPage {

	private AssetDataPanel panel;
	private DataImporterView view;
	private boolean requiresRefresh = false;
		
	public DataReviewPage(DataImporterView view, FormToolkit toolkit) {
		this.view = view;
		panel = new AssetDataPanel(toolkit, true, true, view.getContext()) {			
			@Override
			public void loadWaypoints() {
				loadWaypointsJob.schedule();
			}
		};
	}
	
	public void refresh() {
		panel.refresh();
	}
	
	public void createControl(Composite parent) {
		
		
		panel.createControl(parent);
		
		EventHandler refreshHandler = event->{
			//TODO: this doesn't work
			if (view.getContext().get(MPart.class) == event.getProperty(UIEvents.EventTags.ELEMENT)) return;
			requiresRefresh = true;
		};
		view.getContext().get(IEventBroker.class).subscribe(AssetEvents.ASSETDATA, refreshHandler);
		
		
		IPartListener2 refreshListener = new IPartListener2() {
			
			@Override
			public void partVisible(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partOpened(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partHidden(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals(DataImporterView.ID)) {
					if (requiresRefresh) {
						requiresRefresh = false;
						System.out.println("refreshing");
						panel.refresh();
					}	
				}
			}
		};
		view.getSite().getPage().addPartListener(refreshListener);
		
		parent.addListener(SWT.Dispose, e->{
			view.getContext().get(IEventBroker.class).unsubscribe(refreshHandler);
			view.getSite().getPage().removePartListener(refreshListener);
		});

	}
	
	private Job loadWaypointsJob = new Job("load waypoints") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<UUID> waypointUuids  = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				List<?> aw = session.createQuery("SELECT distinct id.waypoint.uuid, id.waypoint.dateTime FROM AssetWaypoint WHERE state = :state AND id.waypoint.conservationArea = :ca ORDER BY id.waypoint.dateTime")
				.setParameter("state", AssetWaypoint.State.DIRTY)
				.setParameter("ca", SmartDB.getCurrentConservationArea())
				.list();
				
				for (Object o : aw) {
					waypointUuids.add((UUID)((Object[])o)[0]);
				}				
			}
			panel.setWaypoints(waypointUuids);
			return Status.OK_STATUS;
		}
		
	};
	
}
