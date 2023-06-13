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
package org.wcs.smart.asset.ui.views.asset;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.asset.ui.AttributeFieldEditor;
import org.wcs.smart.asset.ui.StationDialog;
import org.wcs.smart.asset.ui.StationLocationDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for asset deployments.  For creating new deployments.
 * 
 * @author Emily
 *
 */
public class AssetDeploymentDialog extends SmartStyledTitleDialog{

	private static final String NEW_STATION = Messages.AssetDeploymentDialog_CreateNewStation;
	private static final String NEW_LOCATION = Messages.AssetDeploymentDialog_CreateNewLocation;
	private static final String NO_OP = ""; //$NON-NLS-1$
	
	private AssetDeployment toUpdate;
	
	private ComboViewer cmbStation;
	private ComboViewer cmbLocation;
	private DateTime dtStartDate;
	private DateTime dtEndDate;
	private DateTime dtStartTime;
	private DateTime dtEndTime;
	
	private Button chEndDate;
	
	@Inject
	private IEclipseContext context;
	private List<AssetDeploymentWrapper> allDeployments;
	private List<AttributeFieldEditor> attributeFields;
	
	private LocalDateTime minWaypointDate = null;
	private LocalDateTime maxWaypointDate = null;
	
	public AssetDeploymentDialog(Shell parentShell, AssetDeployment toUpdate, List<AssetDeploymentWrapper> allDeployments) {
		super(parentShell);
		this.toUpdate = toUpdate;
		this.allDeployments = allDeployments;
		
		if (toUpdate.getUuid() != null) {
			try(Session s = HibernateManager.openSession()){
				
				toUpdate.getAssetWaypoints().forEach(w->{
					LocalDateTime dt = w.getWaypoint().getDateTime();
					if (minWaypointDate == null || dt.isBefore(minWaypointDate)) minWaypointDate = dt;
					if (maxWaypointDate == null || dt.isAfter(maxWaypointDate)) maxWaypointDate = dt;
				});
				HibernateManager.saveOrMerge(s, toUpdate);
			}
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button okBtn = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		okBtn.setEnabled(false);
	}
	
	@Override
	public void okPressed() {
		Object location = ((IStructuredSelection)cmbLocation.getSelection()).getFirstElement();
		toUpdate.setStationLocation((AssetStationLocation)location);
		toUpdate.setStartDate(SmartUtils.toDateTime(dtStartDate, dtStartTime));
		if (chEndDate.getSelection()) {
			toUpdate.setEndDate(SmartUtils.toDateTime(dtEndDate, dtEndTime));
		}else {
			toUpdate.setEndDate(null);
		}
		
		if (toUpdate.getAttributeValues() == null) toUpdate.setAttributeValues(new ArrayList<>());
		for (AttributeFieldEditor editor : attributeFields) {
			AssetDeploymentAttributeValue attributeToUpdate = null;
			for (AssetDeploymentAttributeValue value : toUpdate.getAttributeValues()) {
				if (value.getAttribute().equals(editor.getAttribute())) {
					attributeToUpdate = value;
					break;
				}
			}
			if (attributeToUpdate == null) {
				attributeToUpdate = new AssetDeploymentAttributeValue();
				attributeToUpdate.setAttribute(editor.getAttribute());
				attributeToUpdate.setAssetDeployment(toUpdate);
				toUpdate.getAttributeValues().add(attributeToUpdate);
			}
			boolean add = editor.updateValue(attributeToUpdate);
			if (!add) {
				toUpdate.getAttributeValues().remove(attributeToUpdate);
			}
			
		}
		super.okPressed();
	}
	
	
	private void validate() {
		Button btnOk = getButton(IDialogConstants.OK_ID);
		if (btnOk == null) return;
		btnOk.setEnabled(false);
		setErrorMessage(null);
		
		for (AttributeFieldEditor editor : attributeFields) {
			if (!editor.isValid()) return;
		}
		
		Object station = ((IStructuredSelection)cmbStation.getSelection()).getFirstElement();
		if (station == null || !(station instanceof AssetStation)) {
			setErrorMessage(Messages.AssetDeploymentDialog_StationRequired);
			return;
		}
		
		Object location = ((IStructuredSelection)cmbLocation.getSelection()).getFirstElement();
		if (!cmbLocation.getControl().isEnabled() || location == null || !(location instanceof AssetStationLocation)) {
			setErrorMessage(Messages.AssetDeploymentDialog_LocaitonRequired);
			return;
		}
		
		boolean found = false;
		for (AssetStationLocation l : ((AssetStation)station).getLocations()) {
			if (l.equals(location)) {
				found = true;
				break;
			}
		}
		if (!found) {
			setErrorMessage(Messages.AssetDeploymentDialog_InvalidLocation);
			return;
		}
		
		boolean overlaps = false;
		
		LocalDateTime start = SmartUtils.toDate(dtStartDate).atTime(SmartUtils.toTime(dtStartTime));
		LocalDateTime end = LocalDateTime.now();

		if (chEndDate.getSelection()) {
			end = SmartUtils.toDate(dtEndDate).atTime(SmartUtils.toTime(dtEndTime));
			if (start.isAfter(end)) {
				setErrorMessage(Messages.AssetDeploymentDialog_StartBeforeEnd);
				return;
			}
		}
		
		LocalDateTime now = LocalDateTime.now();
		
		for (AssetDeploymentWrapper deployment : allDeployments) {
			AssetDeployment deploy = deployment.getDeployment();
			if (deploy.equals(toUpdate)) continue;
				
			LocalDateTime startTest = deploy.getStartDate();
			LocalDateTime endTest = now;
			
			if (deploy.getEndDate() != null) endTest = deploy.getEndDate();
			
			if (!(endTest.isBefore(start) || startTest.isAfter(end))) { 
				overlaps = true;
			}
			if (!chEndDate.getSelection() && deploy.getEndDate() == null) {
				overlaps = true;
			}
			if (overlaps) break;
		
		}
		if (overlaps) {
			setErrorMessage(Messages.AssetDeploymentDialog_OverlappingDates);
			return;
		}
		
		//check waypoint are all within deployment date/time
		if ((minWaypointDate != null && minWaypointDate.isBefore(start)) || (maxWaypointDate != null && chEndDate.getSelection() && maxWaypointDate.isAfter(end))) {
			setErrorMessage(Messages.AssetDeploymentDialog_invalidDateRange);
			return;
		}
		btnOk.setEnabled(true);
	}
	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite form = new Composite(parent, SWT.NONE);
		form.setLayout(new GridLayout(2, false));
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(form, SWT.NONE);
		l.setText(AssetDeploymentTableColumn.FixedColumn.STATION.guiName + ":"); //$NON-NLS-1$
		
		cmbStation = new ComboViewer(form, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbStation.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbStation.setContentProvider(ArrayContentProvider.getInstance());
		cmbStation.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetStation) return ((AssetStation)element).getId();
				return super.getText(element);
			}
		});
		cmbStation.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbStation.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)cmbStation.getSelection()).getFirstElement();
				if (x instanceof AssetStation) {
					updateLocationOptions((AssetStation)x, null);
				}else if ( x == NEW_STATION) {
					createNewStation();
				}
				validate();
			}
		});
		l = new Label(form, SWT.NONE);
		l.setText(AssetDeploymentTableColumn.FixedColumn.LOCATION.guiName + ":"); //$NON-NLS-1$
		
		cmbLocation = new ComboViewer(form, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbLocation.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbLocation.setContentProvider(ArrayContentProvider.getInstance());
		cmbLocation.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetStationLocation) return ((AssetStationLocation)element).getId();
				return super.getText(element);
			}
		});
		cmbLocation.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbLocation.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)cmbLocation.getSelection()).getFirstElement();
				if ( x == NEW_LOCATION ) {
					createNewStationLocation();
				}
				validate();
			}
		});
		
		l = new Label(form, SWT.NONE);
		l.setText(AssetDeploymentTableColumn.FixedColumn.START_DATE.guiName + ":"); //$NON-NLS-1$
		
		Composite compstart = new Composite(form, SWT.NONE);
		compstart.setLayout(new GridLayout(2, false));
		((GridLayout)compstart.getLayout()).marginWidth = 0;
		((GridLayout)compstart.getLayout()).marginHeight = 0;
		
		dtStartDate = new DateTime(compstart,  SWT.DATE | SWT.DROP_DOWN | SWT.MEDIUM);
		dtStartTime = new DateTime(compstart, SWT.TIME | SWT.MEDIUM);
		dtStartDate.addListener(SWT.Selection, e->validate());
		dtStartTime.addListener(SWT.Selection, e->validate());
		if (toUpdate.getStartDate() != null) {
			SmartUtils.initDateTimeWidget(dtStartDate, toUpdate.getStartDate().toLocalDate());
			SmartUtils.initDateTimeWidget(dtStartTime, toUpdate.getStartDate().toLocalTime());
		}
		
		l = new Label(form, SWT.NONE);
		l.setText(AssetDeploymentTableColumn.FixedColumn.END_DATE.guiName + ":"); //$NON-NLS-1$
		
		Composite compEndDate = new Composite(form, SWT.NONE);
		compEndDate.setLayout(new GridLayout(3, false));
		((GridLayout)compEndDate.getLayout()).marginWidth = 0;
		((GridLayout)compEndDate.getLayout()).marginHeight = 0;
		
		chEndDate = new Button(compEndDate, SWT.CHECK);
		chEndDate.setSelection(false);
		
		dtEndDate = new DateTime(compEndDate,  SWT.DATE | SWT.DROP_DOWN | SWT.MEDIUM);
		dtEndDate.setEnabled(false);
		dtEndTime = new DateTime(compEndDate, SWT.TIME | SWT.MEDIUM);
		dtEndTime.setEnabled(false);
		
		dtEndDate.addListener(SWT.Selection, e->validate());
		dtEndTime.addListener(SWT.Selection, e->validate());
		
		chEndDate.addListener(SWT.Selection, e->{
			dtEndDate.setEnabled(chEndDate.getSelection());
			dtEndTime.setEnabled(chEndDate.getSelection());
			validate();
		});
		
		
		if (toUpdate.getEndDate() != null) {
			SmartUtils.initDateTimeWidget(dtEndDate, toUpdate.getEndDate().toLocalDate());
			SmartUtils.initDateTimeWidget(dtEndTime, toUpdate.getEndDate().toLocalTime());

			dtEndDate.setEnabled(true);
			dtEndTime.setEnabled(true);
			chEndDate.setSelection(true);
		}
		

		attributeFields = new ArrayList<>();
		List<AssetTypeDeploymentAttribute> attributes;
		try(Session session = HibernateManager.openSession()){
			Asset asset = toUpdate.getAsset();
			if (asset.getUuid() != null) {
				asset = session.get(Asset.class,toUpdate.getAsset().getUuid());
			}
			attributes = QueryFactory.buildQuery(session, AssetTypeDeploymentAttribute.class, "id.assetType", asset.getAssetType()).list(); //$NON-NLS-1$
			attributes.forEach(a->{
				a.getAttribute().getName();
				if (a.getAttribute().getAttributeList() != null) a.getAttribute().getAttributeList().forEach(li -> li.getName());
			});
		}
		if (!attributes.isEmpty()) {
			l = new Label(form, SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			ScrolledComposite scroll = new ScrolledComposite(form, SWT.V_SCROLL);
			scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			
			Composite attributeComp = new Composite(scroll, SWT.NONE);
			attributeComp.setLayout(new GridLayout(2, false));
			scroll.setContent(attributeComp);
			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);
			
			for (AssetTypeDeploymentAttribute attribute : attributes) {
				AttributeFieldEditor editor = new AttributeFieldEditor(attributeComp, attribute.getAttribute());
				if (editor.getTextAttributeControl() != null) editor.getTextAttributeControl().addListener(SWT.Resize, e->scroll.setMinSize(attributeComp.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
				attributeFields.add(editor);
				editor.addSelectionListener(new SelectionListener() {
					
					@Override
					public void widgetSelected(SelectionEvent e) {
						validate();
					}
					
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {}
				});
				if (toUpdate.getAttributeValues() != null) {
					for (AssetDeploymentAttributeValue value : toUpdate.getAttributeValues()) {
						if (value.getAttribute().equals(attribute.getAttribute())) {
							editor.initControl(value);
							break;
						}
					}
				}
			}
			
			scroll.setMinSize(attributeComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//			attributeComp.setSize(scroll.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
		
		
		
		loadStations(toUpdate.getStationLocation() != null ? toUpdate.getStationLocation().getStation() : null, toUpdate.getStationLocation());
		
		setTitle(Messages.AssetDeploymentDialog_Title);
		setMessage(Messages.AssetDeploymentDialog_Message);
		getShell().setText(Messages.AssetDeploymentDialog_Title);
		
		return parent;
	}
	
	private void createNewStation() {
		AssetStation station = new AssetStation();
		station.setConservationArea(SmartDB.getCurrentConservationArea());
		station.setLocations(new ArrayList<>());
		
		StationDialog dialog = new StationDialog(getShell(), station);
		ContextInjectionFactory.inject(dialog, context);
		dialog.open();
		//if station has been saved, then lets reload the stations
		if (station.getUuid() != null) {
			loadStations(station, null);
		}
	}
	
	private void createNewStationLocation() {
		Object x = ((IStructuredSelection)cmbStation.getSelection()).getFirstElement();
		if (!(x instanceof AssetStation)) return;
		
		AssetStation station = (AssetStation)x;
		
		AssetStationLocation location = new AssetStationLocation();
		location.setStation(station);
		
		StationLocationDialog dialog = new StationLocationDialog(getShell(), location);
		ContextInjectionFactory.inject(dialog, context);
		dialog.open();
		//if station has been saved, then lets reload the stations
		if (location.getUuid() != null) {
			loadStations(station, location);
		}
	}
	
	private void updateLocationOptions(AssetStation station, AssetStationLocation toSelect) {
		List<Object> locationOptions = new ArrayList<>();
		locationOptions.add(NO_OP);
		locationOptions.add(NEW_LOCATION);
		
		if (station != null) {
			locationOptions.addAll(station.getLocations());
		}
		cmbLocation.setInput(locationOptions);
		
		if (toSelect != null && locationOptions.contains(toSelect)) {
			cmbLocation.setSelection(new StructuredSelection(toSelect));
		}else {
			if (station!= null && !station.getLocations().isEmpty()) {
				cmbLocation.setSelection(new StructuredSelection(station.getLocations().get(0)));
			}else {
				cmbLocation.setSelection(new StructuredSelection(NO_OP));
			}
		}
		cmbLocation.getControl().setEnabled(station != null);
	}
	
	private void loadStations(AssetStation toSelect, AssetStationLocation locationToSelection) {
		Job j = new Job(Messages.AssetDeploymentDialog_loadingJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Object> stations = new ArrayList<>();
				stations.add(NO_OP);
				stations.add(NEW_STATION);
				try(Session session = HibernateManager.openSession()){
					List<AssetStation> assetStations = (QueryFactory.buildQuery(session, AssetStation.class,
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				
					for (AssetStation s : assetStations) {
						if (s.getLocations() != null) {
							s.getLocations().forEach( loc -> loc.getId() );
						}
					}
					assetStations.sort((a,b)->Collator.getInstance().compare(a.getId(), b.getId()));
					stations.addAll(assetStations);
				}
				Display.getDefault().syncExec(()->{
					cmbStation.setInput(stations);
					AssetStation item = null;
					if (toSelect != null) {
						for (Object x : stations) {
							if (x.equals(toSelect)) {
								item = (AssetStation) x;
								break;
							}
						}
						cmbStation.setSelection(new StructuredSelection(item));
					}
					updateLocationOptions(item, locationToSelection);
				});
				
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
	}

	
	@Override
	public boolean isResizable() {
		return true;
	}
}
