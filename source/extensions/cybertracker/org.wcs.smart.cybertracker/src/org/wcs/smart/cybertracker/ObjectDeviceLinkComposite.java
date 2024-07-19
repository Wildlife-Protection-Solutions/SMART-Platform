/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.SmartMobileDevice;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Displays the smart mobile device ids associated with an object if applicable
 * @since 8.1.0
 */
public class ObjectDeviceLinkComposite extends Composite {

	private TableViewer tblDevices;
	
	private ConservationArea ca;
	
	private HashMap<String, SmartMobileDevice> devices = new HashMap<>();
	private List<String[]> objId2DeviceId;
	
	private Composite compTable;
	private Composite compNone;
	private Composite compSingle;
	private Label lblSingleIcon, lblSingleText;
	private Composite main; 
	private IconCache cache = new IconCache(null, IconManager.Size.SMALL);

	public ObjectDeviceLinkComposite(Composite parent, FormToolkit toolkit, String objectType, String objectIdLabel) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		addListener(SWT.Dispose,e->cache.dispose());
		
		main = toolkit.createComposite(this);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new StackLayout());
		
		compNone = toolkit.createComposite(main);
		compNone.setLayout(new GridLayout());
		toolkit.createLabel(compNone, MessageFormat.format(Messages.ObjectDeviceLinkComposite_NoDevicesFound, objectType));
		
		((StackLayout)main.getLayout()).topControl = compNone;
		
		compSingle = toolkit.createComposite(main);
		compSingle.setLayout(new GridLayout(2, false));
		lblSingleIcon = toolkit.createLabel(compSingle, ""); //$NON-NLS-1$
		lblSingleText = toolkit.createLabel(compSingle, ""); //$NON-NLS-1$
		
		compTable = toolkit.createComposite(main);
		compTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tableLayout = new TableColumnLayout();
		compTable.setLayout(tableLayout);
		
		tblDevices = new TableViewer(compTable, SWT.FULL_SELECTION | SWT.BORDER);
		tblDevices.setContentProvider(ArrayContentProvider.getInstance());
		tblDevices.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblDevices.getTable().setHeaderVisible(true);
		tblDevices.getTable().setLinesVisible(true);
				
		
		createColumn(Messages.ObjectDeviceLinkComposite_DeviceColumnName, 1, tblDevices, new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof String[] link) {
					String deviceId = link[1];
					
					if (devices.containsKey(deviceId)) return devices.get(deviceId).getName();
					return deviceId;
				}
				return super.getText(element);
			}
			public Image getImage(Object element) {
				if (element instanceof String[] link) {
					String deviceId = link[1];
					if (devices.containsKey(deviceId)) {
						SmartMobileDevice device = devices.get(deviceId);
						return cache.getImage(device);
					}
				}
				return null;
			}
		});
		
		createColumn(objectIdLabel, 3, tblDevices, new ColumnLabelProvider() {			
			public String getText(Object element) {
				if (element instanceof String[] link) return link[0];
				return super.getText(element);
			}
		});
	}
	
	private TableViewerColumn createColumn(String name, int weight, TableViewer tbl, ColumnLabelProvider p) {
		TableViewerColumn col = new TableViewerColumn(tbl, SWT.DEFAULT);
		col.getColumn().setText(name);
		col.setLabelProvider(p);
		col.getColumn().setResizable(true);
		col.getColumn().setMoveable(false);
		
		TableColumnLayout collayout = (TableColumnLayout) tbl.getTable().getParent().getLayout();
		collayout.setColumnData(col.getColumn(), new ColumnWeightData(weight, ColumnWeightData.MINIMUM_WIDTH, true));
		
		
		tblDevices.setInput(DialogConstants.LOADING_TEXT);
		return col;
	}

	public static String getTitle() {
		return Messages.ObjectDeviceLinkComposite_PageHeader;
	}

	/**
	 * 
	 * @param ca
	 * @param objId2DeviceId list of 2-element string array where the first element is
	 * the object identifier and the second is the deviceId
	 */
	public void setData(ConservationArea ca, List<String[]> objId2DeviceId) {
		this.ca = ca;
		this.objId2DeviceId = objId2DeviceId;
		
		loadDevices.schedule();

	}
	
	
	private void configureUi() {
		if (main.isDisposed()) return;
		
		if (objId2DeviceId.isEmpty()) {
			((StackLayout)main.getLayout()).topControl = compNone;
		}else if (objId2DeviceId.size() == 1) {
			((StackLayout)main.getLayout()).topControl = compSingle;
			
			String deviceId = objId2DeviceId.get(0)[1];
			SmartMobileDevice device = devices.containsKey(deviceId) ? devices.get(deviceId) : null;
			
			lblSingleText.setText( device != null ? device.getName() : deviceId );
			if (device == null || device.getIcon() == null) {
				lblSingleIcon.setImage(null);
			}else {
				lblSingleIcon.setImage(cache.getImage(device));
			}					
		}else {
			((StackLayout)main.getLayout()).topControl = compTable;
			
			tblDevices.setInput(objId2DeviceId);
			tblDevices.refresh();
		}
		main.layout(true);
	}
	
	private Job loadDevices = new Job(Messages.ObjectDeviceLinkComposite_loadingdevicesjobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			devices.clear();
			try(Session session = HibernateManager.openSession()){
				SmartMobileDeviceManager.INSTANCE.getDevices(session, ca)
				.forEach(device->devices.put(device.getDeviceId(), device));
			}
			Display.getDefault().asyncExec(()->configureUi());
			return Status.OK_STATUS;
		}
		
	};

}
