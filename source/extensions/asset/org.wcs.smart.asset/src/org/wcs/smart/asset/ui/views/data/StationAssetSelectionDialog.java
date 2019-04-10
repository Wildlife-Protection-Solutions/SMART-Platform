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
package org.wcs.smart.asset.ui.views.data;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.AssetLabelProvider;
import org.wcs.smart.asset.ui.AssetTypeLabelProvider;
import org.wcs.smart.asset.ui.StationDialog;
import org.wcs.smart.asset.ui.StationLocationDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for selecting a station and associated location
 * 
 * @author Emily
 *
 */
public class StationAssetSelectionDialog extends SmartStyledDialog{

	public static final String CREATE_STATION = Messages.StationAssetSelectionDialog_NewStationAction;
	public static final String CREATE_LOCATION = Messages.StationAssetSelectionDialog_NewLocationAction;
			
	@Inject
	private IEclipseContext context;
	
	public enum Type{
		ASSET,
		LOCATION,
		STATION,
		DATE;
	}
	
	private TableViewer cmbAsset = null;
	private ComboViewer cmbStation = null;
	private ComboViewer cmbLocation = null;
	private DateTime dtDate = null;
	private DateTime dtTime = null;
	
	private Type type;
	
	private AssetStationLocation selectedLocation;
	private Asset selectedAsset;
	private Date selectedDate;
	private AssetStation selectedStation;
	
	private AssetStation definedStation = null;
	
	public StationAssetSelectionDialog(Shell parentShell, Type type) {
		this(parentShell, type, null);
	}
	
	public StationAssetSelectionDialog(Shell parentShell, Type type, AssetStation station) {
		super(parentShell);
		this.type = type;
		this.definedStation = station;
	}
	
	public Asset getSelectedAsset() {
		return this.selectedAsset;
	}
	
	public AssetStationLocation getSelectedLocation() {
		return this.selectedLocation;
	}
	
	public Date getSelectedDate() {
		return this.selectedDate;
	}
	
	public AssetStation getSelectedStation() {
		return this.selectedStation;
	}
	
	@Override
	protected void okPressed() {
		if (cmbAsset != null) {
			Object x = ((IStructuredSelection)cmbAsset.getSelection()).getFirstElement();
			if (x instanceof Asset) {
				selectedAsset = (Asset) x;
			}else {
				return;
			}
		}
		
		if (cmbLocation != null) {
			Object x = ((IStructuredSelection)cmbLocation.getSelection()).getFirstElement();
			if (x instanceof AssetStationLocation) {
				selectedLocation = (AssetStationLocation) x;
			}else {
				return;
			}
		}
		if (cmbStation != null) {
			Object x = ((IStructuredSelection)cmbStation.getSelection()).getFirstElement();
			if (x instanceof AssetStation) {
				selectedStation = (AssetStation) x;
			}else {
				return;
			}
		}
		if (dtDate != null) {
			selectedDate = SmartUtils.combineDateTime(SmartUtils.getDate(dtDate), SmartUtils.getTime(dtTime));
		}
		super.okPressed();
	}

	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (type == Type.ASSET ) {
			cmbAsset = new TableViewer(main, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
			cmbAsset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			((GridData)cmbAsset.getControl().getLayoutData()).heightHint = 100;
			((GridData)cmbAsset.getControl().getLayoutData()).widthHint = 300;
			
			cmbAsset.setContentProvider(ArrayContentProvider.getInstance());
			cmbAsset.setLabelProvider(new LabelProvider() {
				AssetTypeLabelProvider p2 = new AssetTypeLabelProvider();
				@Override
				public void dispose() {
					super.dispose();
					p2.dispose();
				}
				
				@Override
				public String getText(Object element) {
					if (element instanceof Asset) return ((Asset)element).getId();
					return super.getText(element);
				}
				
				@Override
				public Image getImage(Object element) {
					if (element instanceof Asset) {
						return p2.getImage( ((Asset)element).getAssetType() );
					}
					return super.getImage(element);
				}
			});
			cmbAsset.addDoubleClickListener(e->okPressed());
			
			cmbAsset.setInput(new String[] {DialogConstants.LOADING_TEXT});
			
			loadAssets.setSystem(true);
			loadAssets.schedule();
			
			getShell().setText(Messages.StationAssetSelectionDialog_AssetTitle);
		}
		if (type == Type.LOCATION || type == Type.STATION) {
			Label l = new Label(main, SWT.NONE);
			l.setText(Messages.StationAssetSelectionDialog_StationLabel);
			
			cmbStation = new ComboViewer(main, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
			cmbStation.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			((GridData)cmbStation.getControl().getLayoutData()).widthHint = 300;
			cmbStation.setContentProvider(ArrayContentProvider.getInstance());
			cmbStation.setLabelProvider(new AssetLabelProvider());
			cmbStation.setInput(new String[] {DialogConstants.LOADING_TEXT});
			if (definedStation != null) cmbStation.getControl().setEnabled(false);
			
			cmbStation.addSelectionChangedListener(e->{
				Object x = ((IStructuredSelection)cmbStation.getSelection()).getFirstElement();
				if (x instanceof AssetStation) {
					if (cmbLocation != null) {
						List<Object> kids = new ArrayList<>();
						kids.addAll(((AssetStation)x).getLocations());
						kids.add(CREATE_LOCATION);
						cmbLocation.setInput(kids);
					}
				}else if (x == CREATE_STATION) {
					AssetStation newStation = new AssetStation();
					newStation.setConservationArea(SmartDB.getCurrentConservationArea());
					
					StationDialog dialog = new StationDialog(getShell(), newStation);
					ContextInjectionFactory.inject(dialog, context);
					dialog.open();
					definedStation = newStation;
					cmbStation.setInput(new String[] {DialogConstants.LOADING_TEXT});
					if (cmbLocation != null) cmbLocation.setInput(new String[] {DialogConstants.LOADING_TEXT});
					loadStations.schedule();
				}
			});
			
			if (type == Type.LOCATION) {
				l = new Label(main, SWT.NONE);
				l.setText(Messages.StationAssetSelectionDialog_StationLocationLabel);
				
				cmbLocation = new ComboViewer(main, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
				cmbLocation.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				cmbLocation.setContentProvider(ArrayContentProvider.getInstance());
				cmbLocation.setLabelProvider(new AssetLabelProvider());
				cmbLocation.setInput(new String[] {DialogConstants.LOADING_TEXT});
				cmbLocation.addSelectionChangedListener(e->{
					Object y = ((IStructuredSelection)cmbLocation.getSelection()).getFirstElement();
					if (y == CREATE_LOCATION) {
					
						Object x = ((IStructuredSelection)cmbStation.getSelection()).getFirstElement();
						if (x instanceof AssetStation) {
							AssetStation station = (AssetStation)x;
							
							AssetStationLocation newLocation = new AssetStationLocation();
							newLocation.setStation(station);
									
							StationLocationDialog dialog = new StationLocationDialog(getShell(), newLocation);
							ContextInjectionFactory.inject(dialog, context);
							dialog.open();
							
							cmbStation.setInput(new String[] {DialogConstants.LOADING_TEXT});
							cmbLocation.setInput(new String[] {DialogConstants.LOADING_TEXT});
							loadStations.schedule();
						}
					}
				});
			}
			
			loadStations.setSystem(true);
			loadStations.schedule();
			
			getShell().setText(Messages.StationAssetSelectionDialog_LocationTitle);
		}
		
		if (type == Type.DATE) {
			Label l = new Label(main, SWT.NONE);
			l.setText(Messages.StationAssetSelectionDialog_DateTimeLabel);
			
			Composite c = new Composite(main,  SWT.NONE);
			c.setLayout(new GridLayout(2, false));
			((GridLayout)c.getLayout()).marginWidth = 0;
			((GridLayout)c.getLayout()).marginHeight = 0;
			
			dtDate = new DateTime(c, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR | SWT.LONG);
			dtTime = new DateTime(c, SWT.TIME);
			
			getShell().setText(Messages.StationAssetSelectionDialog_DateTitle);
		}
		return parent;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private Job loadAssets = new Job(Messages.StationAssetSelectionDialog_assetsJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Asset> assets = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				assets.addAll(QueryFactory.buildQuery(session, Asset.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"isRetired", false} //$NON-NLS-1$
				).list());
				assets.forEach(a->a.getAssetType().getName());
			}
			
			assets.sort((a,b)->Collator.getInstance().compare(a.getId(), b.getId()));
			 
			Display.getDefault().syncExec(()->{
				cmbAsset.setInput(assets);
				if (!assets.isEmpty()) cmbAsset.setSelection(new StructuredSelection(assets.get(0)));
			});
			return Status.OK_STATUS;
		}
		
	};
	
	Job loadStations = new Job(Messages.StationAssetSelectionDialog_stationsJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<AssetStation> stations = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				stations.addAll(QueryFactory.buildQuery(session, AssetStation.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}				 //$NON-NLS-1$
				).list());
				stations.forEach(s->s.getId());
				stations.forEach(a->a.getLocations().forEach(l->l.getId()));
			}
			List<Object> input = new ArrayList<>();
			input.addAll(stations);
			input.add(CREATE_STATION);
			Display.getDefault().syncExec(()->{
				cmbStation.setInput(input);
				if (definedStation != null) {
					if (input.contains(definedStation)) {
						cmbStation.setSelection(new StructuredSelection(definedStation));
					}else {
						cmbStation.setSelection(null);
					}
				}else {
					if (!stations.isEmpty()) cmbStation.setSelection(new StructuredSelection(stations.get(0)));
				}
			});
			return Status.OK_STATUS;
		}
		
	};
}
