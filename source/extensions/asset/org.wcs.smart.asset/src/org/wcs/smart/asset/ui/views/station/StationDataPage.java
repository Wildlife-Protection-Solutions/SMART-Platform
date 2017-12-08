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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointMapping;
import org.wcs.smart.asset.ui.AttachmentTable;
import org.wcs.smart.asset.ui.DataDisplaySettings;
import org.wcs.smart.asset.ui.SettingsShell;
import org.wcs.smart.asset.ui.data.AssetDataPanel;
import org.wcs.smart.asset.ui.views.asset.AssetDataPage;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.ui.input.ObservationWizard;
import org.wcs.smart.ui.AttachmentPropertiesDialog;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.ui.properties.DialogConstants;

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
		
		dataPanel = new AssetDataPanel(toolkit, false, context) {
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
		
		
		Composite filterSection = toolkit.createComposite(mainControl, SWT.NONE);
		filterSection.setLayout(new GridLayout(3, false));
		((GridLayout)filterSection.getLayout()).marginWidth = 0;
		filterSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.createLabel(filterSection, "Date Filter:");
		
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
		
		ToolBar tb = new ToolBar(filterSection, SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		//enable/disable editing button based on user permission
		ToolItem itemEdit = new ToolItem(tb, SWT.CHECK);
		itemEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		itemEdit.setToolTipText("enable edit mode");
		itemEdit.addListener(SWT.Selection, e->dataPanel.setEditable(itemEdit.getSelection()));
		
//		ToolItem itemSettings = new ToolItem(tb, SWT.PUSH);
//		itemSettings.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_SETTINGS));
//		itemSettings.setToolTipText("Configure table settings");
//		itemSettings.addListener(SWT.Selection, e->{
//			SettingsShell settingDialog = new SettingsShell(tb.getDisplay());
//			settingDialog.show(tb);
//			settingDialog.getShell().addListener(SWT.Dispose, evt->{
//				DataDisplaySettings newSettings = settingDialog.getSettings();
//				if (newSettings.getDisplayType() != this.settings.getDisplayType() || 
//						newSettings.getIconSize() != this.settings.getIconSize()) {
//					this.settings = newSettings;
//					createDataPanel(null);
//				}
//				
//			});
//		});
		
		
		dataPanel.createControl(mainControl);
		
		return mainControl;
	}
	
	
	public void initializePanel() {
		reloadData();
	}
	
	
	public void reloadData() {
		final Date startDate = dateFilter.getDateFilter().getStartDate();
		final Date endDate = dateFilter.getDateFilter().getEndDate();

		Job loadData = new Job("load data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<UUID> waypointUuids = new ArrayList<>();
				
				try(Session session = HibernateManager.openSession()){
					String query = "SELECT distinct id.waypoint.uuid, id.waypoint.dateTime FROM AssetWaypoint WHERE id.assetDeployment.stationLocation.station = :station ";
					if (startDate != null) {
						query += " and id.waypoint.dateTime >= :startDate ";
					}
					if (endDate != null) {
						query += " and id.waypoint.dateTime <= :endDate ";
					}
					query += " ORDER BY id.waypoint.dateTime desc ";
					Query q = session.createQuery(query);
					if (startDate != null) q.setParameter("startDate",  startDate);
					if (endDate != null) q.setParameter("endDate", endDate);
					q.setParameter("station", parentEditor.getAssetStation());
					for (Object x : q.list()) {
						waypointUuids.add( (UUID)((Object[])x)[0]);  
					}
				}
				dataPanel.setWaypoints(waypointUuids);
				return Status.OK_STATUS;
			}
			
		};
		loadData.schedule();
	}

	
}
