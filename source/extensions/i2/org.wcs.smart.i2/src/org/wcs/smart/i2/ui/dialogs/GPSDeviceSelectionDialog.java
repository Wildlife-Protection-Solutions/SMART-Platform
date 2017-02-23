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
package org.wcs.smart.i2.ui.dialogs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.gpx.GPSBabel;
import org.wcs.smart.i2.Intelligence2PlugIn;

/**
 * Simple dialog for selecting the device type from the GPS Babel device options.
 * 
 * @author Emily
 *
 */
public class GPSDeviceSelectionDialog extends TitleAreaDialog{

	private ComboViewer cmbDevice;
	private String type;
	
	public GPSDeviceSelectionDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public String getDeviceType(){
		return this.type;
	}
	
	@Override
	public void cancelPressed(){
		type = null;
		super.cancelPressed();
	}
	
	@Override
	public void okPressed(){
		type = null;
		String[] selection = (String[]) ((IStructuredSelection)cmbDevice.getSelection()).getFirstElement();
		type = selection[0];
		super.okPressed();
	}

	
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Device Type:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,false, false));
		HashMap<String, String> options = new HashMap<String, String>();
		try {
			options = GPSBabel.getDeviceOptions();
		} catch (IOException e) {
			Intelligence2PlugIn.displayLog("Unable to load gps device options: " +e.getMessage(), e);
		}
		
		String[][] stroptions = new String[options.size()][2];
		int index = 0;
		String[] toSelect = null;
		for (Entry<String,String> op : options.entrySet()){
			stroptions[index][0] = op.getKey();
			stroptions[index++][1] = op.getValue();
			if (op.getKey().toLowerCase().contains(GPSBabel.DEFAULT_DEVICE_TYPE)){ //$NON-NLS-1$
				toSelect = stroptions[index-1];
			}
		}
		
		cmbDevice = new ComboViewer(main, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbDevice.setContentProvider(ArrayContentProvider.getInstance());
		cmbDevice.setInput(stroptions);
		cmbDevice.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		cmbDevice.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof Object[]) {
					String[] data = (String[]) element;
					return data[1];
				}
				return super.getText(element);
			}
		});
		if (toSelect != null) cmbDevice.setSelection(new StructuredSelection((Object)toSelect));
		cmbDevice.addSelectionChangedListener(e->getButton(IDialogConstants.OK_ID).setEnabled(validate()));
		
		super.setMessage("Select the type of device to import from");
		super.setTitle("Import Waypoints");
		getShell().setText("Import Waypoints");
		
		return parent;
	}
	
	private boolean validate(){
		return !cmbDevice.getSelection().isEmpty() && ((IStructuredSelection)cmbDevice.getSelection()).getFirstElement() instanceof String[];
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(validate());
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

}
