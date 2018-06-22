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
package org.wcs.smart.event.i2.entity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.dialogs.AttributeFieldEditor;

/**
 * Dialog for creating entity mappings for the create entity action
 * 
 * @author Emily
 *
 */
public class NewMappingDialog extends TitleAreaDialog {

	private ComboViewer cmbIntelAttribute;
	private ComboViewer cmbDmAttribute;
	
	private IntelEntityType type;
	private List<Attribute> dmAttributes;
	
	private Composite cmpFixedMap;
	private Composite cmpDmMap;
	
	private EntityMapping mapping;
	
	private Button btnFixed; 
	private Button btnDm;
	private Button btnObsPosition;
	
	public NewMappingDialog(Shell parentShell, IntelEntityType type, List<Attribute> dmAttributes) {
		super(parentShell);
		
		this.type = type;
		this.dmAttributes = dmAttributes;
	}

	public EntityMapping getMapping() {
		return this.mapping;
	}
	
	@Override
	public void okPressed() {
		EntityMapping.Type type = EntityMapping.Type.FIXED;
		if (btnFixed.getSelection()) {
			type = EntityMapping.Type.FIXED;
		}
		if (btnDm.getSelection()) {
			type = EntityMapping.Type.DM;
		}
		if (btnObsPosition.getSelection()) {
			type = EntityMapping.Type.POSITION;
		}
		mapping = new EntityMapping(type);
		
		IntelEntityTypeAttribute attribute = (IntelEntityTypeAttribute) cmbIntelAttribute.getStructuredSelection().getFirstElement();
		mapping.setEntityAttribute(attribute.getAttribute());
		
		if (btnFixed.getSelection()) {
			AttributeFieldEditor fieldEditor = (AttributeFieldEditor) cmpFixedMap.getData("EDITOR");
			IntelEntityAttributeValue temp = new IntelEntityAttributeValue();
			temp.setAttribute(attribute.getAttribute());
			fieldEditor.updateValue(temp);
			
			switch(attribute.getAttribute().getType()) {
			case BOOLEAN:
				mapping.setFixedValue(temp.getNumberValue() >= 0.5);
				break;
			case DATE:
				mapping.setFixedValue(temp.getDateValue());
				break;
			case EMPLOYEE:
				//TODO: don't support employee mappings
				break;
			case LIST:
				mapping.setIntelListItem(temp.getAttributeListItem());
				break;
			case NUMERIC:
				mapping.setFixedValue(temp.getNumberValue());
				break;
			case POSITION:
				mapping.setFixedValue(temp.getNumberValue(), temp.getNumberValue2());
				break;
			case TEXT:
				mapping.setFixedValue(temp.getStringValue());
				break;
			default:
				break;
			
			}
		}
		if (btnDm.getSelection()) {
			Attribute dmAttribute = (Attribute)cmbDmAttribute.getStructuredSelection().getFirstElement();
			mapping.setDataModelAttribute(dmAttribute);
		}
		
		super.okPressed();
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite) super.createDialogArea(parent);
		
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Label l = new Label(temp, SWT.NONE);
		l.setText("Attribute:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		
		cmbIntelAttribute = new ComboViewer(temp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbIntelAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbIntelAttribute.setContentProvider(ArrayContentProvider.getInstance());
		cmbIntelAttribute.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelEntityTypeAttribute) {
					return MessageFormat.format("{0} ({1})", ((IntelEntityTypeAttribute)element).getAttribute().getName(), ((IntelEntityTypeAttribute)element).getAttribute().getType().name().toLowerCase());
				}
				return super.getText(element);
			}
		});
		
		cmbIntelAttribute.addSelectionChangedListener(e->{
			boolean isPosition = ((IntelEntityTypeAttribute)cmbIntelAttribute.getStructuredSelection().getFirstElement()).getAttribute().getType() == IntelAttribute.AttributeType.POSITION;
			btnObsPosition.setEnabled(isPosition);
			
			if (!isPosition && btnObsPosition.getSelection()) {
				btnFixed.setSelection(true);
				btnObsPosition.setSelection(false);
			}
			configureFixedValue();
			configureDm();
		});
		
		
		l = new Label(temp, SWT.NONE);
		l.setText("Map To:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, true));
		
		Composite addArea = new Composite(temp, SWT.NONE);
		addArea.setLayout(new GridLayout(1, false));
		addArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnFixed = new Button(addArea, SWT.RADIO);
		btnFixed.setText("Fixed Value:");
		btnFixed.setSelection(true);
		
		cmpFixedMap = new Composite(addArea, SWT.NONE);
		cmpFixedMap.setLayout(new GridLayout(2, false));
		cmpFixedMap.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmpFixedMap.setEnabled(true);
		 
		btnDm = new Button(addArea, SWT.RADIO);
		btnDm.setText("Data Model Attribute:");
		
		cmpDmMap = new Composite(addArea, SWT.NONE);
		cmpDmMap.setLayout(new GridLayout());
		cmpDmMap.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmpDmMap.setEnabled(false);
		btnFixed.addSelectionListener(new SelectionListener() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setEnabled(cmpFixedMap, true);
				setEnabled(cmpDmMap, false);
				
				configureFixedValue();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { }
		});
		
		btnDm.addSelectionListener(new SelectionListener() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setEnabled(cmpFixedMap, false);
				setEnabled(cmpDmMap, true);
				
				configureDm();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { }
		});
		
		cmbDmAttribute = new ComboViewer(cmpDmMap, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbDmAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbDmAttribute.setContentProvider(ArrayContentProvider.getInstance());
		cmbDmAttribute.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Attribute) {
					return MessageFormat.format("{0} ({1})", ((Attribute) element).getName(), ((Attribute) element).getType().name().toLowerCase());
				}
				return super.getText(element);
			}
		});
		
		btnObsPosition = new Button(addArea, SWT.RADIO);
		btnObsPosition.setText("Observation Position");
		btnObsPosition.setEnabled(false);
		
		cmbIntelAttribute.setInput(type.getAttributes());
		cmbIntelAttribute.setSelection(new StructuredSelection(type.getAttributes().get(0)));
		
		setTitle(type.getName());
		setMessage(MessageFormat.format("Create new attribute mapping for entity type {0}", type.getName()));
		getShell().setText("New Attribute Mapping");
		return parent;
	}
	
	private void configureFixedValue() {
		IntelEntityTypeAttribute ea = (IntelEntityTypeAttribute) cmbIntelAttribute.getStructuredSelection().getFirstElement();
		if (ea == null) return;
		Object x = cmpFixedMap.getData("ATTRIBUTE");
		if (x != null && x == ea.getAttribute()) return;
		for (Control c : cmpFixedMap.getChildren()) c.dispose();
		
		//create entity editor
		AttributeFieldEditor fieldEditor = new AttributeFieldEditor(cmpFixedMap, ea.getAttribute(), false);
		cmpFixedMap.setData("EDITOR", fieldEditor);
		cmpFixedMap.setData("ATTRIBUTE", ea.getAttribute());
		cmpFixedMap.layout(true);
				
		setEnabled(cmpFixedMap, cmpFixedMap.getEnabled());
	}
	
	private void configureDm() {
		if (dmAttributes == null) return;
		IntelEntityTypeAttribute ea = (IntelEntityTypeAttribute) cmbIntelAttribute.getStructuredSelection().getFirstElement();
		if (ea == null) {
			cmbDmAttribute.setInput(Collections.emptyList());
			return;
		}
		Object x = cmpDmMap.getData("ATTRIBUTE");
		if (x != null && x == ea.getAttribute()) return;
		
		cmpDmMap.setData("ATTRIBUTE", ea.getAttribute());
		List<Attribute> filtered = new ArrayList<>();
		for (Attribute a : dmAttributes) {
			boolean add = false;
			switch(a.getType()) {
			case BOOLEAN:
				if (ea.getAttribute().getType() == IntelAttribute.AttributeType.BOOLEAN) add = true;
				break;
			case DATE:
				if (ea.getAttribute().getType() == IntelAttribute.AttributeType.DATE) add = true;
				break;
			case LIST:
				if (ea.getAttribute().getType() == IntelAttribute.AttributeType.LIST) add = true;
				break;
			case NUMERIC:
				if (ea.getAttribute().getType() == IntelAttribute.AttributeType.NUMERIC) add = true;
				break;
			case TEXT:
				if (ea.getAttribute().getType() == IntelAttribute.AttributeType.TEXT) add = true;
				break;
			case TREE:
				if (ea.getAttribute().getType() == IntelAttribute.AttributeType.LIST) add = true;
				break;
			default:
				break;
			
			}
			if (add) filtered.add(a);
		}
		cmbDmAttribute.setInput(filtered);
		setEnabled(cmpDmMap, cmpDmMap.getEnabled());
	}

	private void setEnabled(Control c, boolean enabled) {
		List<Control> cts = new ArrayList<>();
		cts.add(c);
		while(!cts.isEmpty()) {
			Control cc = cts.remove(0);
			cc.setEnabled(enabled);
			if (cc instanceof Composite) {
				for (Control kid : ((Composite)cc).getChildren()) {
					cts.add(kid);
				}
			}
		}
	}
}
