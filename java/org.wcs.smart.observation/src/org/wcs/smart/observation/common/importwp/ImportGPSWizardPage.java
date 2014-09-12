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
package org.wcs.smart.observation.common.importwp;

import java.io.IOException;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.common.importwp.gpsbabel.GPSBabel;
import org.wcs.smart.observation.internal.Messages;

/**
 * Wizard page to select device type.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportGPSWizardPage extends ImportOptionsWizardPage {
	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportGPSWizardPage_PageName;

	private static final String ERROR_COULD_NOT_READ_DEVICES = Messages.ImportGPSWizardPage_Error_CouldNotReadDevices;

	private ImportOptionsComposite ops;
	private ComboViewer gpsViewer;
	private ImportWpSelectWizardPage nextPage;
	

	/**
	 * @param pageName
	 */
	public ImportGPSWizardPage( ImportGpsDataWizard wizard ) {
		super(PAGE_NAME, wizard);
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
		
		ops = new ImportOptionsComposite(center, getImportType(), getValidOptions(), getOptionLabels(), getWarningMessage());
		ops.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ops.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				updateComplete();
			}});
		
		//read gps devices
		HashMap<String, String> supportedDevices;
		
		try {
			supportedDevices = GPSBabel.getDeviceOptions();
			if (supportedDevices == null || supportedDevices.size() == 0){
				
				ObservationPlugIn.displayLog(ERROR_COULD_NOT_READ_DEVICES, null);
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
			ObservationPlugIn.displayLog(ERROR_COULD_NOT_READ_DEVICES, null);
			setErrorMessage(ERROR_COULD_NOT_READ_DEVICES);
		}
		updateComplete();
		super.setTitle(Messages.ImportGPSWizardPage_PageTitle + ((ImportGpsDataWizard)getWizard()).getType().guiName);
		super.setMessage(Messages.ImportGPSWizardPage_DialogMessage);
		
	}
	
	private void updateComplete(){
		if (((IStructuredSelection)gpsViewer.getSelection()).getFirstElement() == null){
			setPageComplete(false);
			((ImportGpsDataWizard)getWizard()).setCanFinish(false);
			return;
		}
		setPageComplete(true);
		ImportOption op = ops.getImportOption();
		if (op == ImportOption.DATE || op == ImportOption.ALL ){
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
	private String getDeviceType(){
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
		ImportOption op = ops.getImportOption();
		if (op == ImportOption.DATE || op == ImportOption.ALL ){
			//not more pages
			return null;
		}else if (op == ImportOption.SELECT){
			if (nextPage == null){
				nextPage = new ImportWpSelectWizardPage((ImportGpsDataWizard) getWizard());
			}
			return nextPage;
		}
		return null;
    }

	@Override
	public boolean beforeMoveNext(WizardPage nextPage) {
		((ImportGpsDataWizard)getWizard()).setImportOption(ops.getImportOption());
		((GpsImportEngine)((ImportGpsDataWizard)getWizard()).getImportEngine()).setDeviceType(getDeviceType());
		return true;
	}

	@Override
	public boolean init() {
		return true;
	}
}
