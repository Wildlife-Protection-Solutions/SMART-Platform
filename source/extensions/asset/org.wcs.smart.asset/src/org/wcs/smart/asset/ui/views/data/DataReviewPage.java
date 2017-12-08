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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypoint.State;
import org.wcs.smart.asset.model.AssetWaypointAttachment;
import org.wcs.smart.asset.ui.AttachmentTable;
import org.wcs.smart.asset.ui.DataDisplaySettings;
import org.wcs.smart.asset.ui.data.AssetDataPanel;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.ui.AttachmentPropertiesDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

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
		
		panel = new AssetDataPanel(toolkit, true, view.getContext()) {			
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
				List<Object> aw = session.createQuery("SELECT distinct id.waypoint.uuid, id.waypoint.dateTime FROM AssetWaypoint WHERE state = :state AND id.waypoint.conservationArea = :ca ORDER BY id.waypoint.dateTime")
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
