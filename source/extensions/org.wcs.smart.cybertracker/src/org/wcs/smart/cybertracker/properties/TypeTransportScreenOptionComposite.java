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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrolOption;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;

/**
 * Patrol Type/Transport screens configuration.
 *  
 * @author elitvin
 * @since 2.0.0
 */
public class TypeTransportScreenOptionComposite extends Composite {

	private CyberTrackerPatrolOption typeOption;
	private CyberTrackerPatrolOption transportOption;
	
	private List<PatrolType> patrolTypes;

	private Button btnTypeDisplayPage;
	private Button btnTransportDisplayPage;
	private ComboViewer typeViewer;
	private ComboViewer transportViewer;
	
	public TypeTransportScreenOptionComposite(Composite parent, CyberTrackerPatrolOption typeOption, CyberTrackerPatrolOption transportOption, List<PatrolType> patrolTypes) {
		super(parent, SWT.NONE);
		this.typeOption = typeOption;
		this.transportOption = transportOption;
		this.patrolTypes = patrolTypes;
		
		createControls();
	}

	protected void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group typeGroup = new Group(this,  SWT.NONE);
		typeGroup.setText(typeOption.getType().getGuiLabel());
		typeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		typeGroup.setLayout(new GridLayout(2, false));

		Label label = new Label(typeGroup, SWT.NONE);
		label.setText(Messages.ScreenOptionComposite_DisplayPage);
		btnTypeDisplayPage = new Button(typeGroup, SWT.CHECK);
		btnTypeDisplayPage.setSelection(typeOption.isVisible());
		btnTypeDisplayPage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				typeViewer.getControl().setEnabled(!btnTypeDisplayPage.getSelection());
			}
		});
		
		label = new Label(typeGroup, SWT.NONE);
		label.setText(Messages.ScreenOptionComposite_DefaultValue);
		
		typeViewer = new ComboViewer(typeGroup, SWT.READ_ONLY);
		typeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		typeViewer.getControl().setEnabled(!typeOption.isVisible());
		typeViewer.setContentProvider(ArrayContentProvider.getInstance());
		typeViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolType) {
					PatrolType i = (PatrolType) element;
					return i.getType().getGuiName();
				}
				return super.getText(element);
			}
		});
 		typeViewer.setInput(patrolTypes);
 		PatrolType.Type type = getPatrolType(typeOption);
 		if (type == null && !patrolTypes.isEmpty()) {
 			type = patrolTypes.get(0).getType();
 			typeOption.setStringValue(type.name());
 		}
 		if (type != null) {
 			for (PatrolType item : patrolTypes) {
 				if (item.getType() == type) {
 					typeViewer.setSelection(new StructuredSelection(item));
 					break;
 				}
 			}
 		}
 		typeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
 			@Override
 			public void selectionChanged(SelectionChangedEvent event) {
 				IStructuredSelection selection = (IStructuredSelection) typeViewer.getSelection();
 				Object obj = selection.getFirstElement();
 				if (obj instanceof PatrolType) {
 					PatrolType pt = (PatrolType) obj;
 	 				typeOption.setStringValue(pt.getType().name());

 	 				List<PatrolTransportType> tTypes = getTransportTypes();
 	 		 		transportViewer.setInput(tTypes);
 	 		 		byte[] uuid = tTypes.isEmpty() ? null : tTypes.get(0).getUuid();
 		 			transportOption.setUuidValue(uuid);
 	 		 		if (uuid != null) {
 	 		 			for (NamedItem item : tTypes) {
 	 		 				if (Arrays.equals(item.getUuid(), uuid)) {
 	 		 					transportViewer.setSelection(new StructuredSelection(item));
 	 		 					break;
 	 		 				}
 	 		 			}
 	 		 		}
 	 				//setChangesMade(true);
 				}
 			}
 		});
		
 		//============== transport ==============
 		
		Group transportGroup = new Group(this,  SWT.NONE);
		transportGroup.setText(transportOption.getType().getGuiLabel());
		transportGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		transportGroup.setLayout(new GridLayout(2, false));

		label = new Label(transportGroup, SWT.NONE);
		label.setText(Messages.ScreenOptionComposite_DisplayPage);
		btnTransportDisplayPage = new Button(transportGroup, SWT.CHECK);
		btnTransportDisplayPage.setSelection(transportOption.isVisible());
		btnTransportDisplayPage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				transportViewer.getControl().setEnabled(!btnTransportDisplayPage.getSelection());
			}
		});
		
		label = new Label(transportGroup, SWT.NONE);
		label.setText(Messages.ScreenOptionComposite_DefaultValue);
		
		transportViewer = new ComboViewer(transportGroup, SWT.READ_ONLY);
		transportViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		transportViewer.getControl().setEnabled(!transportOption.isVisible());
		transportViewer.setContentProvider(ArrayContentProvider.getInstance());
		transportViewer.setLabelProvider(new NamedItemLabelProvider());
		List<PatrolTransportType> tTypes = getTransportTypes();
 		transportViewer.setInput(tTypes);
 		byte[] uuid = transportOption.getUuidValue();
 		if (uuid == null && !tTypes.isEmpty()) {
 			uuid = tTypes.get(0).getUuid();
 			transportOption.setUuidValue(uuid);
 		}
 		if (uuid != null) {
 			for (NamedItem item : tTypes) {
 				if (Arrays.equals(item.getUuid(), uuid)) {
 					transportViewer.setSelection(new StructuredSelection(item));
 					break;
 				}
 			}
 		}
		transportViewer.addSelectionChangedListener(new ISelectionChangedListener() {
 			@Override
 			public void selectionChanged(SelectionChangedEvent event) {
 				IStructuredSelection selection = (IStructuredSelection) transportViewer.getSelection();
 				Object obj = selection.getFirstElement();
 				if (obj instanceof UuidItem) {
 					UuidItem i = (UuidItem) obj;
 					transportOption.setUuidValue(i.getUuid());
 	 				//setChangesMade(true);
 				}
 			}
 		});
 		
	}

	private PatrolType.Type getPatrolType(CyberTrackerPatrolOption option) {
 		String val = option.getStringValue();
 		return val == null ? null : PatrolType.Type.valueOf(val);
	}
	
	private List<PatrolTransportType> getTransportTypes() {
 		PatrolType.Type type = getPatrolType(typeOption);
 		if (type == null || patrolTypes.isEmpty())
 			return new ArrayList<PatrolTransportType>();

 		for (PatrolType item : patrolTypes) {
 			if (item.getType() == type) {
 				return item.getTransportTypes();
 			}
 		}

		return new ArrayList<PatrolTransportType>();
	}
	
}
