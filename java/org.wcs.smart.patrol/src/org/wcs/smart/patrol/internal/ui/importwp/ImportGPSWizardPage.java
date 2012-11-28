/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.patrol.internal.ui.importwp;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.gpsbabel.GPSBabel;

/**
 * Wizard page to select device type.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportGPSWizardPage extends WizardPage {
	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportGPSWizardPage_PageName;

	private static final String ERROR_COULD_NOT_READ_DEVICES = Messages.ImportGPSWizardPage_Error_CouldNotReadDevices;
	private Button opAll;
	private Button opSelect;
	private Button opDate;
	
	private ComboViewer gpsViewer;
	private ImportWpSelectWizardPage nextPage;
	
	private boolean importAll = false;
	/**
	 * @param pageName
	 */
	protected ImportGPSWizardPage( ImportGpsDataWizard wizard ) {
		super(PAGE_NAME);
		wizard.addPage(this);
		this.importAll = false;
	}

	/**
	 * 
	 * @return <code>true</code> if all waypoints are to be imported 
	 * and assigned to the correct day; <code>false</code> if waypoints
	 * are to be imported for only the current day or if waypoints
	 * are to be selected from a list.
	 */
	public boolean getImportAll(){
		return importAll;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		super.setControl(comp);
		
		Composite center = new Composite(comp, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.ImportGPSWizardPage_DeviceType_Label);
		
		gpsViewer = new ComboViewer(center, SWT.READ_ONLY);
		gpsViewer.setContentProvider(ArrayContentProvider.getInstance());
		gpsViewer.setLabelProvider(new LabelProvider());
		
		
		Composite ops = new Composite(center, SWT.NONE);
		ops.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ops.setLayout(new GridLayout(1, false));
		

		opAll = new Button(ops, SWT.RADIO);
		opAll.setText(Messages.ImportGPSWizardPage_ImportAllOption);
		opAll.setSelection(true);
		opAll.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				importAll = true;
				updateComplete();
			}
		});
		importAll = true;
		
		opDate = new Button(ops, SWT.RADIO);
		opDate.setText(MessageFormat.format(
			Messages.ImportGPSWizardPage_OImportOnlyCurrentDayOp,
			new Object[]{((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase(),
					DateFormat.getDateInstance(DateFormat.MEDIUM).format(((ImportGpsDataWizard)getWizard()).getCurrentDate())}));
		opDate.setSelection(false);
		opDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importAll = false;
				updateComplete();
			}
		});
		
		opSelect = new Button(ops, SWT.RADIO);
		opSelect.setText(MessageFormat.format(
				Messages.ImportGPSWizardPage_SelectPointsToImportOp,
				new Object[]{((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase(),
						DateFormat.getDateInstance(DateFormat.MEDIUM).format(((ImportGpsDataWizard)getWizard()).getCurrentDate())}));

		
		
		
		opSelect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importAll = false;
				updateComplete();
			}
		});
		
		//read gps devices
		HashMap<String, String> supportedDevices;
		
		try {
			supportedDevices = GPSBabel.getDeviceOptions();
			if (supportedDevices == null || supportedDevices.size() == 0){
				
				SmartPatrolPlugIn.displayLog(ERROR_COULD_NOT_READ_DEVICES, null);
				setErrorMessage(ERROR_COULD_NOT_READ_DEVICES);
			}
			
			DeviceSelection[] devices = new DeviceSelection[supportedDevices.size()];
			int i = 0;
			DeviceSelection toSelect = null;
			for (Iterator<Entry<String,String>> iterator = supportedDevices.entrySet().iterator(); iterator.hasNext();) {
				Entry<String, String> type = iterator.next();
				DeviceSelection ds = new DeviceSelection(type.getKey(), type.getValue());;
				devices[i++] = ds;
				if (type.getKey().toLowerCase().contains("garmin")){ //$NON-NLS-1$
					toSelect = ds;
				}
				
			}
			gpsViewer.setInput(devices);
			gpsViewer.setSelection(new StructuredSelection(toSelect));
		} catch (IOException e) {
			SmartPatrolPlugIn.displayLog(ERROR_COULD_NOT_READ_DEVICES, null);
			setErrorMessage(ERROR_COULD_NOT_READ_DEVICES);
		}
		updateComplete();
		super.setMessage(Messages.ImportGPSWizardPage_DialogMessage);
		
	}
	
	private void updateComplete(){
		if (((IStructuredSelection)gpsViewer.getSelection()).getFirstElement() == null){
			setPageComplete(false);
			((ImportGpsDataWizard)getWizard()).setCanFinish(false);
			return;
		}
		setPageComplete(true);
		if (opDate.getSelection() || opAll.getSelection() ){
			((ImportGpsDataWizard)getWizard()).setCanFinish(true);
		}else{
			((ImportGpsDataWizard)getWizard()).setCanFinish(false);
		}
		getWizard().getContainer().updateButtons();		
	}
	
	/**
	 * 
	 * @return the type of device selected
	 */
	public String getDeviceType(){
		DeviceSelection ds = (DeviceSelection) ((StructuredSelection)this.gpsViewer.getSelection()).getFirstElement();
		if (ds != null){
			return ds.name;
		}
		return null;
	}
	
	private class DeviceSelection{
		String description;
		String name;
		public DeviceSelection(String name, String description){
			this.name = name;
			this.description = description;
		}
		
		@Override
		public String toString(){
			return this.description;
		}
	}
	
	@Override
    public IWizardPage getNextPage() {
		if (opDate.getSelection() || opAll.getSelection() ){
			//not more pages
			return null;
		}else if (opSelect.getSelection()){
			if (nextPage == null){
				nextPage = new ImportWpSelectWizardPage((ImportGpsDataWizard) getWizard());
			}
			return nextPage;
		}
		return null;
    }
}
