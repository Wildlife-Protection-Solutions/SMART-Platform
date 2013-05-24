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

import java.text.MessageFormat;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.GPSDataImport.ImportType;

/**
 * Wizard page to select where to import waypoint data from
 * @author Emily
 * @since 1.0.0
 */
public class ImportWpTypeWizardPage extends WizardPage {

	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportWpTypeWizardPage_PageName;
	private Button opGPS;
	private Button opGPX;
	private Button opWaypoint;
	
	public static final int IMPORT_GPS = 2;
	public static final int IMPORT_GPX = 4;
	public static final int IMPORT_WAYPOINT = 6;
	
	//next pages
	private ImportGPSWizardPage devicePage;
	private ImportGpxWizardPage gpxPage;
	private ImportFromWaypointWizardPage waypointPage;
	
	/**
	 * @param pageName
	 */
	protected ImportWpTypeWizardPage( ) {
		super(PAGE_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Composite center = new Composite(comp, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(MessageFormat.format(
				Messages.ImportWpTypeWizardPage_ImportFromLabel, new Object[]{((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase() }));
		
		Composite ops = new Composite(center, SWT.NONE);
		ops.setLayout(new GridLayout(1, false));
		((GridLayout)ops.getLayout()).marginLeft = 20;
		opGPS = new Button(ops, SWT.RADIO);
		opGPS.setText(Messages.ImportWpTypeWizardPage_GPSOp);
		opGPX = new Button(ops, SWT.RADIO);
		opGPX.setText(Messages.ImportWpTypeWizardPage_GPXOp);
		opGPS.setSelection(true);
		
		if (  ((ImportGpsDataWizard)getWizard()).getType() == ImportType.TRACK ){
			opWaypoint = new Button(ops, SWT.RADIO);
			opWaypoint.setText(Messages.ImportWpTypeWizardPage_GenerateWaypointsOp);
		}
		
		super.setTitle(Messages.ImportWpTypeWizardPage_PageTitle + ((ImportGpsDataWizard)getWizard()).getType().guiName);
		super.setMessage(MessageFormat.format(Messages.ImportWpTypeWizardPage_PageMessage, new Object[]{((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase()}));
		super.setControl(comp);
	}
	
	@Override
    public IWizardPage getNextPage() {
		if (opGPS.getSelection()){
			if (devicePage == null){
				devicePage = new ImportGPSWizardPage( (ImportGpsDataWizard)getWizard());
			}
			return devicePage;
		}else if (opGPX.getSelection()){
			if (gpxPage == null){
				gpxPage = new ImportGpxWizardPage((ImportGpsDataWizard)getWizard());
			}
			return gpxPage;
		}else if (opWaypoint.getSelection()){
			if (waypointPage == null){
				waypointPage = new ImportFromWaypointWizardPage((ImportGpsDataWizard)getWizard());
			}
			return waypointPage;
		}
		return null;
    }
}
