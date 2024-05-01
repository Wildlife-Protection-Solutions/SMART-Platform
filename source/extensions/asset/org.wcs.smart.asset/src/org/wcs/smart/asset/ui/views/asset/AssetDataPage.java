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
package org.wcs.smart.asset.ui.views.asset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
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

import jakarta.persistence.Tuple;

/**
 * Data page for asset editor.
 * 
 * @author Emily
 *
 */
public class AssetDataPage {

	@Inject
	private IEclipseContext context;
	
	private AssetEditor parentEditor;
	
	private AssetDataPanel dataPanel;
	private Composite mainControl;
	private DateFilterDropDownComposite dateFilter;
	
	public AssetDataPage(AssetEditor parent) {
		this.parentEditor = parent;

	}
	
	public Composite createDataSection(Composite parent, FormToolkit toolkit) {
		
		dataPanel = new AssetDataPanel(toolkit, AssetSecurityManager.INSTANCE.canEditAssetData(), false, false, false, context) {
			@Override
			public void loadWaypoints() {
				reloadData();
			}
		};
		dataPanel.setAsset(parentEditor.getAsset());
		
		mainControl = toolkit.createComposite(parent, SWT.NONE);
		mainControl.setLayout(new GridLayout());
		mainControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)mainControl.getLayout()).marginWidth = 0;
		((GridLayout)mainControl.getLayout()).marginHeight = 0;
		
		
		Composite filterSection = toolkit.createComposite(mainControl, SWT.BORDER);
		filterSection.setLayout(new GridLayout(3, false));
		//((GridLayout)filterSection.getLayout()).marginWidth = 0;
		filterSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.createLabel(filterSection, Messages.AssetDataPage_DateFilter);
		
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
	
	
	public void initializePanel() {
		reloadData();
	}
	
	
	public void reloadData() {
		final LocalDate startDate = dateFilter.getDateFilter().getStartDate();
		final LocalDate endDate = dateFilter.getDateFilter().getEndDate();

		Job loadData = new Job(Messages.AssetDataPage_loadDataJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (parentEditor.getAsset() == null) {
					dataPanel.setWaypoints(null);
					return Status.OK_STATUS;
				}
				
				if (parentEditor.getAsset().getUuid() == null) {
					dataPanel.setWaypoints(Collections.emptyList());
					return Status.OK_STATUS;
				}
				
				List<UUID> waypointUuids = new ArrayList<>();
				try(Session session = HibernateManager.openSession()){
					String query = "SELECT distinct wp.uuid, wp.dateTime FROM AssetWaypoint join waypoint wp WHERE assetDeployment.asset = :asset "; //$NON-NLS-1$
					if (startDate != null) {
						query += " and wp.dateTime >= :startDate "; //$NON-NLS-1$
					}
					if (endDate != null) {
						query += " and wp.dateTime <= :endDate "; //$NON-NLS-1$
					}
					query += " ORDER BY wp.dateTime desc "; //$NON-NLS-1$
					Query<Tuple> q = session.createQuery(query, Tuple.class);
					if (startDate != null) q.setParameter("startDate",  startDate.atStartOfDay()); //$NON-NLS-1$
					if (endDate != null) q.setParameter("endDate", endDate.atTime(LocalTime.MAX)); //$NON-NLS-1$
					q.setParameter("asset", parentEditor.getAsset()); //$NON-NLS-1$
					for (Tuple x : q.list()) {
						waypointUuids.add( (UUID) x.get(0));  
					}
				}
				dataPanel.setWaypoints(waypointUuids);
				return Status.OK_STATUS;
			}
			
		};
		loadData.schedule();
	}

	
}

