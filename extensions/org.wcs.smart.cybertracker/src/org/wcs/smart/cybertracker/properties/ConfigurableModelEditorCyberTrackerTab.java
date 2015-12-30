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
package org.wcs.smart.cybertracker.properties;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.properties.CyberTrackerPropertiesComposite.IPropsChangeListener;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.IConfigurableModelEditorTabContent;

/**
 * Tab with CyberTracker properties content for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConfigurableModelEditorCyberTrackerTab implements IConfigurableModelEditorTabContent {

	private ConfigurableModelEditDialog dialog;
	
	private ComboViewer cbProfile;

	@Override
	public void setDialog(ConfigurableModelEditDialog dialog) {
		this.dialog = dialog;
	}
	
	@Override
	public String getTabName() {
		return "CyberTracker";
	}

	@Override
	public Composite createTabContent(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout mainLayout = new GridLayout(1, false);
		mainLayout.marginBottom=0;
		mainLayout.marginHeight = 0;
		mainLayout.marginLeft = 0;
		mainLayout.marginRight = 0;
		mainLayout.marginTop = 0;
		mainLayout.marginWidth = 0;
		main.setLayout(mainLayout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite controlCmp = new Composite(main, SWT.NONE);
		controlCmp.setLayout(new GridLayout(2, false));
		controlCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblProfile = new Label(controlCmp, SWT.NONE);
		lblProfile.setText("Properties profile:");
		lblProfile.setToolTipText("Properties profile that will be used when this Configurable model is exported to CyberTracker.");

		cbProfile = new ComboViewer(controlCmp, SWT.READ_ONLY);
		cbProfile.getControl().setToolTipText("Properties profile that will be used when this Configurable model is exported to CyberTracker.");
		cbProfile.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cbProfile.setContentProvider(ArrayContentProvider.getInstance());
		cbProfile.setLabelProvider(new CyberTrackerProfileLabelProvider());
 		cbProfile.setInput(getAvailableProfiles());
		cbProfile.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				profileChanged();
			}
		});

		Composite buttonsCmp = new Composite(main, SWT.NONE);
		buttonsCmp.setLayout(new GridLayout(2, false));
		buttonsCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnManage = new Button(buttonsCmp, SWT.PUSH);
		btnManage.setText("Manage profiles...");
		btnManage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				manageProfiles();
			}
		});
		
		Button btnEdit = new Button(buttonsCmp, SWT.PUSH);
		btnEdit.setText("Edit profile");
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editProfile();
			}
		});
		
		CyberTrackerPropertiesComposite ctPropCmp = new CyberTrackerPropertiesComposite(main);
		ctPropCmp.populateValuesFromObj(new CyberTrackerProperties()); //TODO: need real data
		ctPropCmp.addPropsChangeListener(new IPropsChangeListener(){
			@Override
			public void changesMade() {
				//TODO: it should be readonly!!!!
			}
		});
		
		return main;
	}

	private List<String> getAvailableProfiles() {
		// TODO: need real data
		return Arrays.asList("Profile 1", "Profile 2", "Profile 3");
	}

	protected void profileChanged() {
		// TODO Auto-generated method stub
		
	}

	protected void manageProfiles() {
		// TODO Auto-generated method stub
		
	}

	protected void editProfile() {
		// TODO Auto-generated method stub
		
	}
	
	private class CyberTrackerProfileLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			//TODO: display profile names
//			if (element instanceof Integer) {
//				String name = id2Name.get((Integer) element);
//				if (name != null)
//					return name;
//			}
			return super.getText(element);
		}
	}

}
