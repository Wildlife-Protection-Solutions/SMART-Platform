/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.connect.cybertracker.ctpackage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeContentProvider;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeElement;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeLabelProvider;
import org.wcs.smart.connect.cybertracker.model.CtConnectPackageMetadata;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICmProvider;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Ui Controller for adding connect alerts/upload details to package
 * @author Emily
 *
 */
public class ConnectDataUiController implements IPackageUiContribution{

	private ICtPackage ctpackage;

	private Listener onModified;
	
	private Button btnUploadData;
	private Label lblUp1, lblUp2;
	private Text txtDataPeriod;
	
	private Button btnPositionUpdates;
	private Label lblPos1, lblPos2;
	private Text txtPositionPeriod;
	private ComboViewer cmbPositionType;
	
	private List<AlertType> types ;
	private LoadAlertTypesJob loadTypesJob;
	
	@Inject
	private IEclipseContext context;

	private boolean fireEvents = true;
	
	public boolean isTab() { 
		return true; 
	}
	
	public String getTabName() { 
		return "Connect"; 
	}
	
	private void validate() {
		if (!fireEvents) return;
		if (onModified == null) return;
		onModified.handleEvent(new Event());
	}
	
	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified) {
		this.ctpackage = ctpackage;
		this.onModified = onModified;
		
		loadTypesJob = new LoadAlertTypesJob(context) {
			@Override
			public void typesLoaded(List<AlertType> atypes) {
				types = atypes;
				parent.getDisplay().asyncExec(()->{
					cmbPositionType.setInput(types);
					
					if (ctpackage instanceof AbstractCtPackage) {
						MetadataFieldValue data = findCreateMetadataField(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name(), (AbstractCtPackage)ctpackage);
						if (data.getUuidValue() != null) {
							AlertType temp = new AlertType();
							temp.setUuid(data.getUuidValue());
							cmbPositionType.setSelection(new StructuredSelection(temp));
						}
					}

				});
			}
		};
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		Composite upDataComp = new Composite(main, SWT.FLAT);
		upDataComp.setLayout(new GridLayout());
		upDataComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)upDataComp.getLayout()).marginWidth = 0;
		((GridLayout)upDataComp.getLayout()).marginHeight = 0;
		
		Composite header = new Composite(upDataComp, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		WidgetElement.setCSSClass(header, "SMARTSection");
		
		Label l = new Label(header, SWT.NONE);
		l.setText("Data Uploads");
//		l.setToolTipText("If using this option, users cannot plug\nthe mobile device into the SMART Desktop\nto download data.  All data\nwill be sent to a SMART Connect\nserver via an internet connection,\nthen processed on the SMART Desktop\nthrough the data queue.\nObservation data will stay on\nthe device until an internet connection\nis acquired at which time it\nwill be sent to SMART Connect.");
		Composite core = new Composite(upDataComp, SWT.FLAT);
		core.setLayout(new GridLayout(4, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label msg = new Label(core, SWT.WRAP);
		msg.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		msg.setText("If using this option all data will be sent to Connect, then imported into SMART Desktop through the data queue features.  Users cannot plug the mobile device into the SMART Desktop to download data. An internet connection is required, and all data will remain on the device until an internet connection is acquired.");
		((GridData)msg.getLayoutData()).widthHint = 140;
		
		btnUploadData = new Button(core, SWT.CHECK);
		btnUploadData.setSelection(true);
		btnUploadData.addListener(SWT.Selection, e->{
			txtDataPeriod.setEnabled(btnUploadData.getSelection());
			lblUp1.setEnabled(btnUploadData.getSelection());
			lblUp2.setEnabled(btnUploadData.getSelection());
			validate();
		});
		
		lblUp1 = new Label(core, SWT.NONE);
		lblUp1.setText("Upload patrol data every");
		lblUp1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				btnUploadData.setSelection(!btnUploadData.getSelection());
				btnUploadData.notifyListeners(SWT.Selection, new Event());
			}
		});
		txtDataPeriod = new Text(core, SWT.BORDER);
		txtDataPeriod.setText("20");
		txtDataPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtDataPeriod.getLayoutData()).widthHint = 30;
		txtDataPeriod.addListener(SWT.Modify, e->validate());
		
		lblUp2 = new Label(core, SWT.NONE);
		lblUp2.setText("minutes");
		lblUp2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite posComp = new Composite(main, SWT.FLAT);
		posComp.setLayout(new GridLayout());
		posComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)posComp.getLayout()).marginWidth = 0;
		((GridLayout)posComp.getLayout()).marginHeight = 0;
		
		header = new Composite(posComp, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		WidgetElement.setCSSClass(header, "SMARTSection");
		
		l = new Label(header, SWT.NONE);
		l.setText("Position Updates");
		
		core = new Composite(posComp, SWT.FLAT);
		core.setLayout(new GridLayout(5, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		msg = new Label(core, SWT.WRAP);
		msg.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		msg.setText("Position updates will appear on the SMART Connect web application Alerts Map.  Position updates require the mobile device has an internet connection and remain on the device until an internet connection is acquired.");
		((GridData)msg.getLayoutData()).widthHint = 140;
		
		btnPositionUpdates = new Button(core, SWT.CHECK);
		btnPositionUpdates.setSelection(false);
		btnPositionUpdates.addListener(SWT.Selection, e->{
			lblPos1.setEnabled(btnPositionUpdates.getSelection());
			lblPos2.setEnabled(btnPositionUpdates.getSelection());
			txtPositionPeriod.setEnabled(btnPositionUpdates.getSelection());
			cmbPositionType.getControl().setEnabled(btnPositionUpdates.getSelection());
			validate();
		});
		lblPos1 = new Label(core, SWT.NONE);
		lblPos1.setText("Send position updates every ");
		lblPos1.setEnabled(false);
		lblPos1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				btnPositionUpdates.setSelection(!btnPositionUpdates.getSelection());
				btnPositionUpdates.notifyListeners(SWT.Selection, new Event());
			}
		});
		
		txtPositionPeriod = new Text(core, SWT.BORDER);
		txtPositionPeriod.setText("10");
		txtPositionPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtDataPeriod.getLayoutData()).widthHint = 30;
		txtPositionPeriod.addListener(SWT.Modify, e->validate());
		txtPositionPeriod.setEnabled(false);
		
		lblPos2 = new Label(core, SWT.NONE);
		lblPos2.setText("minutes as type ");
		lblPos2.setEnabled(false);
		
		cmbPositionType = new ComboViewer(core, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbPositionType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbPositionType.getControl().setEnabled(false);
		cmbPositionType.setContentProvider(ArrayContentProvider.getInstance());
		cmbPositionType.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AlertType) return ((AlertType) element).getLabel();
				return super.getText(element);
			}
		});
		cmbPositionType.setInput("Not Loaded");
		cmbPositionType.addSelectionChangedListener(e->validate());

		Link link = new Link(main, SWT.NONE);
		link.setText("<a>" + "refresh alert types" + "</a>");
		link.addListener(SWT.Selection, e->{
			loadTypesJob.schedule();
		});
		
		loadTypesJob.schedule();
		
		
		if (ctpackage instanceof AbstractCtPackage) {
			try {
				fireEvents = false;
				MetadataFieldValue data = findCreateMetadataField(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name(), (AbstractCtPackage)ctpackage);
				if (data.getBooleanValue()) {
					btnUploadData.setSelection(true);
					txtDataPeriod.setText(data.getStringValue());
				}else {
					btnUploadData.setSelection(false);
				}
				data = findCreateMetadataField(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name(), (AbstractCtPackage)ctpackage);
				if (data.getBooleanValue()) {
					btnPositionUpdates.setSelection(true);
					txtPositionPeriod.setText(data.getStringValue());
				}else {
					btnPositionUpdates.setSelection(false);
				}
				btnUploadData.notifyListeners(SWT.Selection, new Event());
				btnPositionUpdates.notifyListeners(SWT.Selection, new Event());
			}finally {
				fireEvents = true;
			}
			
		}

		
		return main;
	}

	
	@Override
	public String isValid() {
		if (btnUploadData.getSelection()) {
			String min = txtDataPeriod.getText();
			
			try {
				int imin = Integer.parseInt(min);
				if (imin <= 0) {
					throw new Exception();
				}
			}catch (Exception ex) {
				return "Invalid data upload time period. Must be an integer between 1 and 9,9999.";
			}
		}
		if (btnPositionUpdates.getSelection()) {
			String time = txtPositionPeriod.getText();
			try {
				int imin = Integer.parseInt(time);
				if (imin <= 0) {
					throw new Exception();
				}
			}catch (Exception ex) {
				return "Invalid position update time period. Must be an integer between 1 and 9,9999.";
			}
			if (cmbPositionType.getStructuredSelection().isEmpty()) {
				return "An alert type must be selected for position alerts";
			}
			if (!(cmbPositionType.getStructuredSelection().getFirstElement() instanceof AlertType )){
				return "An alert type must be selected for position alerts";
			}
		}
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage) {
		if (!(ctpackage instanceof AbstractCtPackage)) return;
		
		MetadataFieldValue data = findCreateMetadataField(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name(), (AbstractCtPackage)ctpackage);
		if (btnUploadData.getSelection()) {
			data.setBooleanValue(true);
			data.setStringValue(txtDataPeriod.getText());
		}else {
			data.setBooleanValue(false);
			data.setStringValue(null);
		}
		
		data = findCreateMetadataField(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name(), (AbstractCtPackage)ctpackage);
		if (btnPositionUpdates.getSelection()) {
			data.setBooleanValue(true);
			data.setStringValue(txtPositionPeriod.getText());
			data.setUuidValue(  ((AlertType)cmbPositionType.getStructuredSelection().getFirstElement()).getUuid() );
		}else {
			data.setBooleanValue(false);
			data.setStringValue(null);
			data.setUuidValue(null);
		}
		
	}
	
	private MetadataFieldValue findMetadataField(String key, AbstractCtPackage ctpackage) {
		for (MetadataFieldValue v : ctpackage.getMetadataValues()) {
			if (v.getMetadataKey().equals(key)) return v;
		}
		return null;
	}
	
	private MetadataFieldValue findCreateMetadataField(String key, AbstractCtPackage ctpackage) {
		for (MetadataFieldValue v : ctpackage.getMetadataValues()) {
			if (v.getMetadataKey().equals(key)) return v;
		}
		MetadataFieldValue v = new MetadataFieldValue();
		v.setMetadataKey(key);
		v.setConservationArea(ctpackage.getConservationArea());
		v.setCtPackage(ctpackage);
		ctpackage.getMetadataValues().add(v);
		return v;
	}

}
