/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.station;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.AssetSecurityManager;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.ui.data.AssetDataPanel;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SharedUtils;

/**
 * Station editor data page
 * @author Emily
 *
 */
public class StationDataPage {

	@Inject
	private IEclipseContext context;
	
	private StationEditor parentEditor;
	
	private AssetDataPanel dataPanel;
	private Composite mainControl;
	private DateFilterDropDownComposite dateFilter;
	
	public StationDataPage(StationEditor parent) {
		this.parentEditor = parent;

	}
	
	public Composite createDataSection(Composite parent, FormToolkit toolkit) {
		boolean canEdit = AssetSecurityManager.INSTANCE.canEditStationLocationData();
		dataPanel = new AssetDataPanel(toolkit, canEdit, false, false, false, context) {
			@Override
			public void loadWaypoints() {
				reloadData();
			}
		};
		
		mainControl = toolkit.createComposite(parent, SWT.NONE);
		mainControl.setLayout(new GridLayout());
		mainControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)mainControl.getLayout()).marginWidth = 0;
		((GridLayout)mainControl.getLayout()).marginHeight = 0;
		
		
		Composite filterSection = toolkit.createComposite(mainControl, SWT.BORDER);
		filterSection.setLayout(new GridLayout(3, false));
		((GridLayout)filterSection.getLayout()).marginWidth = 0;
		filterSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.createLabel(filterSection, Messages.StationDataPage_DateFilter);
		
		 DateFilterComposite.DateFilter[] filters = new  DateFilterComposite.DateFilter[] {
				 DateFilterComposite.DateFilter.LAST_30_DAYS,
				 DateFilterComposite.DateFilter.LAST_60_DAYS,
				 DateFilterComposite.DateFilter.LAST_1_YEARS,
				 DateFilterComposite.DateFilter.LAST_5_YEARS,
				 DateFilterComposite.DateFilter.ALL
				 
		 };
		dateFilter = new DateFilterDropDownComposite(filterSection, filters, DateFilterComposite.DateFilter.LAST_1_YEARS);
		dateFilter.addChangeListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				reloadData();
			}
		});
		
		dataPanel.createControl(mainControl);
		
		return mainControl;
	}
	
	private UUID scrollToWp = null;
	public void scrollTo(UUID waypointUuid) {
		if (dataPanel.scrollToWaypoint(waypointUuid)) return;
		scrollToWp = waypointUuid;
	}
	
	
	public void initializePanel() {
		reloadData();
	}
	
	
	public void reloadData() {
		
		final LocalDateTime startDate = dateFilter.getDateFilter().getStartDate() == null ? null : dateFilter.getDateFilter().getStartDate().atStartOfDay();
		final LocalDateTime endDate = dateFilter.getDateFilter().getEndDate() == null ? null : dateFilter.getDateFilter().getEndDate().atTime(LocalTime.MAX);

		Job loadData = new Job(Messages.StationDataPage_loadJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<UUID> waypointUuids = new ArrayList<>();
				
				try(Session session = HibernateManager.openSession()){
					String query = "SELECT distinct id.waypoint.uuid, id.waypoint.dateTime FROM AssetWaypoint WHERE id.assetDeployment.stationLocation.station = :station "; //$NON-NLS-1$
					if (startDate != null) {
						query += " and id.waypoint.dateTime >= :startDate "; //$NON-NLS-1$
					}
					if (endDate != null) {
						query += " and id.waypoint.dateTime <= :endDate "; //$NON-NLS-1$
					}
					query += " ORDER BY id.waypoint.dateTime desc "; //$NON-NLS-1$
					Query<?> q = session.createQuery(query);
					if (startDate != null) q.setParameter("startDate",  startDate); //$NON-NLS-1$
					if (endDate != null) q.setParameter("endDate", endDate); //$NON-NLS-1$
					q.setParameter("station", parentEditor.getAssetStation()); //$NON-NLS-1$
					for (Object x : q.list()) {
						waypointUuids.add( (UUID)((Object[])x)[0]);  
					}
				}
				dataPanel.setWaypoints(waypointUuids);
				if (scrollToWp != null) {
					dataPanel.scrollToWaypoint(scrollToWp);
					scrollToWp = null;
				}
				return Status.OK_STATUS;
			}
			
		};
		loadData.schedule();
	}

	
}
