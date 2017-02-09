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
package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.i2.ui.AttributeListItemLabelProvider;
import org.wcs.smart.ui.CheckBoxDropDown;
import org.wcs.smart.ui.OnOffButton;
import org.wcs.smart.util.SmartUtils;

/**
 * Attribute field editor 
 * 
 * @author Emily
 *
 */
public class AttributeFieldEditor {

	private IntelAttribute attribute;
	private boolean isMulti;
	private Composite parent;
	
	private Text txtValue;
	private Text txtValue2;
	private OnOffButton btnOnOff;
	private ComboViewer cmbViewer;
	private CheckBoxDropDown cmbMultiSelect;
	private DateTime dtDateTime;
	private Button btnChDateTime;
	private Button btnChOnOff;
	private ControlDecoration cd;
	private String warnMessage;
	
	private String name = null;
	
	private List<SelectionListener> listeners = new ArrayList<SelectionListener>();
	
	/**
	 * Assumption is the parent layout is a 2 column grid layout
	 * @param parent
	 * @param attribute
	 */
	public AttributeFieldEditor(Composite parent, IntelAttribute attribute) {
		this(parent, attribute, false, null);
	}
	
	/**
	 * Assumption is the parent layout is a 2 column grid layout
	 * @param parent
	 * @param attribute
	 * @param name field name or null if attribute name to be used
	 * @param multiSelect - if multiple list items can be selected; only valid for list attributes
	 */
	public AttributeFieldEditor(Composite parent, IntelAttribute attribute, Boolean multiSelect, String name) {
		this.parent = parent;
		this.attribute = attribute;
		this.isMulti = multiSelect == null ? false : multiSelect;
		if (name == null){
			this.name = attribute.getName();
		}else{
			this.name = name;
		}
		createControl();
	}

	public void adapt(FormToolkit toolkit){
		List<Control> kids = new ArrayList<Control>();
		kids.add(parent);
		while(!kids.isEmpty()){
			Control kid = kids.remove(0);
			toolkit.adapt(kid, true, true);
			if (kid instanceof Composite){
				for (Control c : ((Composite)kid).getChildren()){
				 kids.add(c);
				}
			}
		}
	}
	
	/**
	 * Validates field before returning
	 * @return true if field is valid; false otherwise.  
	 */
	public boolean isValid(){
		return validate();
	}
	
	/*
	 * validates and updates control decoration icon/message
	 */
	private boolean validate(){
		String msg = null;
		
		if (attribute.getType() == AttributeType.NUMERIC){
			try{
				if (!txtValue.getText().trim().isEmpty()){
					Double.parseDouble(txtValue.getText());
				}
			}catch(Exception ex){
				msg = "Unable to parse number from text";
			}
		}
		if (attribute.getType() == AttributeType.POSITION){
			try{
				if (!txtValue.getText().trim().isEmpty()){
					Double.parseDouble(txtValue.getText());
				}
				if (!txtValue2.getText().trim().isEmpty()){
					Double.parseDouble(txtValue2.getText());
				}
			}catch(Exception ex){
				msg = "Unable to positiong coorindate numbers from text";
			}
		}
		if (msg != null){
			cd.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_ERROR));
			cd.setDescriptionText(msg);
			cd.show();
			return false;
		}else if (warnMessage != null){
			cd.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_WARNING));
			cd.setDescriptionText(warnMessage);
			cd.show();
			return true;
		}else{
			cd.hide();
			return true;
		}	
	}
	
	/**
	 * Sets a warning message for the field.  This does not affect the validity of the field; but if the
	 * field is valid this warning message will be shown.  Set to null to clear; 
	 * @param warnMessage
	 */
	public void setWarningMessage(String warnMessage){
		this.warnMessage = warnMessage;
		validate();
	}
	
	public IntelAttribute getAttribute(){
		return this.attribute;
	}
	public void addSelectionListener(SelectionListener listener){
		listeners.add(listener);
	}
	public void removeSelectionListener(SelectionListener listener){
		listeners.remove(listener);
	}
	
	private void modified(){
		for (SelectionListener l : listeners){
			l.widgetSelected(null);
		}
	}
	
	/**
	 * returns true if the value is set; false if not set and should be removed
	 * from attribute list.
	 * 
	 * @param value
	 * @return
	 */
	public boolean updateValue(IntelEntityRelationshipAttributeValue value){
		boolean add = false;
		if (attribute.getType() == AttributeType.BOOLEAN){
			if (((OnOffButton)btnOnOff).isEnabled()){
				add = true;
				if (((OnOffButton)btnOnOff).getSelection()){
					value.setNumberValue(1d);
				}else{
					value.setNumberValue(0d);
				}
			}
		}else if (attribute.getType() == AttributeType.DATE){
			if (((DateTime)dtDateTime).getEnabled()){
				add = true;
				value.setDateValue( SmartUtils.getDate((DateTime)dtDateTime));
			}
		}else if (attribute.getType() == AttributeType.LIST){
			if (isMulti) throw new IllegalStateException("Multi select lists not supported for entity relationship attributes");
			IStructuredSelection selection = (IStructuredSelection)((ComboViewer)cmbViewer).getSelection();
			if (!selection.isEmpty()){
				Object item = selection.getFirstElement();
				if (item instanceof IntelAttributeListItem){
					add = true;
					value.setAttributeListItem((IntelAttributeListItem) item);
				}
			}
		}else if (attribute.getType() == AttributeType.NUMERIC){
			try{
				String dvalue = ((Text)txtValue).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue(d);
					add = true;
				}
			}catch (Exception ex){
				//
			}
		}else if (attribute.getType() == AttributeType.POSITION){
			try{
				String dvalue = ((Text)txtValue).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue(d);
					add = true;
				}
			}catch (Exception ex){
			}
			try{
				String dvalue = ((Text)txtValue2).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue2(d);
					add = true;
				}
			}catch (Exception ex){
				//
			}
		}else if (attribute.getType() == AttributeType.TEXT){
			String svalue = ((Text)txtValue).getText();
			if (!svalue.trim().isEmpty()){
				value.setStringValue(svalue.trim());
				add = true;
			}
		}
		return add;
	}
	
	public boolean updateValue(IntelRecordAttributeValue value){
		boolean add = false;
		if (attribute.getType() == AttributeType.BOOLEAN){
			if (((OnOffButton)btnOnOff).isEnabled()){
				add = true;
				if (((OnOffButton)btnOnOff).getSelection()){
					value.setNumberValue(1d);
				}else{
					value.setNumberValue(0d);
				}
			}
		}else if (attribute.getType() == AttributeType.DATE){
			if (((DateTime)dtDateTime).getEnabled()){
				add = true;
				value.setDateValue( SmartUtils.getDate((DateTime)dtDateTime));
			}
		}else if (attribute.getType() == AttributeType.LIST){
			if (value.getAttributeListItems() == null){
				value.setAttributeListItems(new ArrayList<>());
			}
			ArrayList<IntelRecordAttributeValueList> listValues = new ArrayList<IntelRecordAttributeValueList>();
			
			Collection<?> objects = Collections.emptyList();
			if (!isMulti){
				if (!cmbViewer.getSelection().isEmpty()){
					objects = Collections.singletonList(  ((IStructuredSelection)((ComboViewer)cmbViewer).getSelection()).getFirstElement() );
				}
			}else{
				objects = cmbMultiSelect.getCheckObjects();
			}
			for (Object item : objects) {					
				if (item instanceof IntelAttributeListItem){
					IntelRecordAttributeValueList list = new IntelRecordAttributeValueList();
					list.getId().setElementUuid(((IntelAttributeListItem) item).getUuid());
					list.getId().setValue(value);
					listValues.add(list);
					add = true;
				}
			}
			List<IntelRecordAttributeValueList> delete = new ArrayList<IntelRecordAttributeValueList>();
			for (IntelRecordAttributeValueList existing : value.getAttributeListItems()){
				if (!listValues.contains(existing)) delete.add(existing);
			}
			value.getAttributeListItems().removeAll(delete);
			for (IntelRecordAttributeValueList newItem: listValues){
				if (!value.getAttributeListItems().contains(newItem)){
					value.getAttributeListItems().add(newItem);
				}
			}
			
		}else if (attribute.getType() == AttributeType.NUMERIC){
			try{
				String dvalue = ((Text)txtValue).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue(d);
					add = true;
				}
			}catch (Exception ex){
			}
			try{
				String dvalue = ((Text)txtValue2).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue2(d);
					add = true;
				}
			}catch (Exception ex){
				//
			}
		}else if (attribute.getType() == AttributeType.POSITION){
			try{
				String dvalue = ((Text)txtValue).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue(d);
					add = true;
				}
				dvalue = ((Text)txtValue2).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue2(d);
					add = true;
				}
			}catch (Exception ex){
				//
			}
		}else if (attribute.getType() == AttributeType.TEXT){
			String svalue = ((Text)txtValue).getText();
			if (!svalue.trim().isEmpty()){
				value.setStringValue(svalue.trim());
				add = true;
			}
		}
		return add;
	}
	
	public void initControl(IntelEntityRelationshipAttributeValue value){
		if (attribute.getType() == AttributeType.TEXT){
			txtValue.setText(value.getStringValue());
		}else if (attribute.getType() == AttributeType.NUMERIC){
			txtValue.setText(String.valueOf(value.getNumberValue()));
		}else if (attribute.getType() ==  AttributeType.LIST){
			cmbViewer.setSelection(new StructuredSelection(value.getAttributeListItem()));
		}else if (attribute.getType() ==  AttributeType.DATE){
			if(value.getDateValue() == null){
				btnChDateTime.setSelection(false);
				dtDateTime.setEnabled(false);
			}else{
				btnChDateTime.setSelection(true);
				SmartUtils.initDateDateTimeWidget(dtDateTime, value.getDateValue());
				dtDateTime.setEnabled(true);
			}
		}else if (attribute.getType() ==  AttributeType.BOOLEAN){
			if (value.getNumberValue() == null){
				btnChOnOff.setSelection(false);
				btnOnOff.setEnabled(false);
			}else{
				btnChOnOff.setSelection(true);
				btnOnOff.setSelection(value.getNumberValue() >= 0.5);
				btnOnOff.setEnabled(true);
			}
		}else if (attribute.getType() == AttributeType.POSITION){
			txtValue.setText(String.valueOf(value.getNumberValue()));
			txtValue2.setText(String.valueOf(value.getNumberValue2()));
		}
	}
	/**
	 * returns true if the value is set; false if not set and should be removed
	 * from attribute list.
	 * 
	 * @param value
	 * @return
	 */
	public boolean updateValue(IntelEntityAttributeValue value){
		boolean add = false;
		if (attribute.getType() == AttributeType.BOOLEAN){
			if (((OnOffButton)btnOnOff).isEnabled()){
				add = true;
				if (((OnOffButton)btnOnOff).getSelection()){
					value.setNumberValue(1d);
				}else{
					value.setNumberValue(0d);
				}
			}
		}else if (attribute.getType() == AttributeType.DATE){
			if (((DateTime)dtDateTime).getEnabled()){
				add = true;
				value.setDateValue( SmartUtils.getDate((DateTime)dtDateTime));
			}
		}else if (attribute.getType() == AttributeType.LIST){
			if (isMulti) throw new IllegalStateException("Multi select lists not supported for entity attributes");
			IStructuredSelection selection = (IStructuredSelection)((ComboViewer)cmbViewer).getSelection();
			if (!selection.isEmpty()){
				Object item = selection.getFirstElement();
				if (item instanceof IntelAttributeListItem){
					add = true;
					value.setAttributeListItem((IntelAttributeListItem) item);
				}
			}
		}else if (attribute.getType() == AttributeType.NUMERIC){
			try{
				String dvalue = ((Text)txtValue).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue(d);
					add = true;
				}
			}catch (Exception ex){
				//
			}
		}else if (attribute.getType() == AttributeType.POSITION){
			try{
				String dvalue = ((Text)txtValue).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue(d);
					add = true;
				}
			}catch (Exception ex){
				//
			}
			try{
				String dvalue = ((Text)txtValue2).getText();
				if (!dvalue.trim().isEmpty()){
					Double d = Double.parseDouble(dvalue);
					value.setNumberValue2(d);
					add = true;
				}
			}catch (Exception ex){
				//
			}
		}else if (attribute.getType() == AttributeType.TEXT){
			String svalue = ((Text)txtValue).getText();
			if (!svalue.trim().isEmpty()){
				value.setStringValue(svalue.trim());
				add = true;
			}
		}
		return add;
	}
	
	public void initControl(IntelEntityAttributeValue value){
		if (attribute.getType() == AttributeType.TEXT){
			txtValue.setText(value.getStringValue());
		}else if (attribute.getType() == AttributeType.NUMERIC){
			txtValue.setText(String.valueOf(value.getNumberValue()));
		}else if (attribute.getType() == AttributeType.POSITION){
			txtValue.setText(String.valueOf(value.getNumberValue()));
			txtValue2.setText(String.valueOf(value.getNumberValue2()));
		}else if (attribute.getType() ==  AttributeType.LIST){
			cmbViewer.setSelection(new StructuredSelection(value.getAttributeListItem()));
		}else if (attribute.getType() ==  AttributeType.DATE){
			if(value.getDateValue() == null){
				btnChDateTime.setSelection(false);
				dtDateTime.setEnabled(false);
			}else{
				btnChDateTime.setSelection(true);
				SmartUtils.initDateDateTimeWidget(dtDateTime, value.getDateValue());
				dtDateTime.setEnabled(true);
			}
		}else if (attribute.getType() ==  AttributeType.BOOLEAN){
			if (value.getNumberValue() == null){
				btnChOnOff.setSelection(false);
				btnOnOff.setEnabled(false);
			}else{
				btnChOnOff.setSelection(true);
				btnOnOff.setSelection(value.getNumberValue() >= 0.5);
				btnOnOff.setEnabled(true);
			}
		}
	}
	
	public void initControl(IntelRecordAttributeValue value){
		if ( value.getAttribute() == null ) return;
		if (attribute.getType() == AttributeType.TEXT){
			txtValue.setText(value.getStringValue());
		}else if (attribute.getType() == AttributeType.NUMERIC){
			txtValue.setText(String.valueOf(value.getNumberValue()));
		}else if (attribute.getType() == AttributeType.POSITION){
			txtValue.setText(String.valueOf(value.getNumberValue()));
			txtValue2.setText(String.valueOf(value.getNumberValue2()));
		}else if (attribute.getType() ==  AttributeType.LIST){
			List<Object> selectedObjects = new ArrayList<>();
			if (value.getAttributeListItems() != null){
				for (IntelRecordAttributeValueList i : value.getAttributeListItems()){
					if (value.getAttribute().getAttribute() != null){
						IntelAttributeListItem temp = new IntelAttributeListItem();
						temp.setUuid(i.getId().getElementUuid());
						selectedObjects.add(temp);
					}
					if (value.getAttribute().getEntityType() != null){
						IntelEntityType temp = new IntelEntityType();
						temp.setUuid(i.getId().getElementUuid());
						selectedObjects.add(temp);
					}
				}
			}
			
			Collection<?> items = null;
			if (isMulti){
				items = cmbMultiSelect.getInput();
			}else{
				items = (Collection<?>)cmbViewer.getInput();
			}
			
			List<Object> selectedObjs = new ArrayList<Object>();
			for (Object x : selectedObjects){
				for (Object y : items){
					if (x.equals(y)){
						selectedObjs.add(y);
						break;
					}
				}
			}
			if (isMulti){
				cmbMultiSelect.setValue(selectedObjs);
			}else{
				cmbViewer.setSelection(new StructuredSelection(selectedObjs));
			}
		}else if (attribute.getType() ==  AttributeType.DATE){
			if(value.getDateValue() == null){
				btnChDateTime.setSelection(false);
				dtDateTime.setEnabled(false);
			}else{
				btnChDateTime.setSelection(true);
				SmartUtils.initDateDateTimeWidget(dtDateTime, value.getDateValue());
				dtDateTime.setEnabled(true);
			}
		}else if (attribute.getType() ==  AttributeType.BOOLEAN){
			if (value.getNumberValue() == null){
				btnChOnOff.setSelection(false);
				btnOnOff.setEnabled(false);
			}else{
				btnChOnOff.setSelection(true);
				btnOnOff.setSelection(value.getNumberValue() >= 0.5);
				btnOnOff.setEnabled(true);
			}
		}
	}
	
	private void createControl(){
		Label l = new Label(parent, SWT.NONE);
		l.setText(this.name + ":");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		if (attribute.getType() == AttributeType.TEXT){
			txtValue = new Text(parent, SWT.BORDER);
			txtValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtValue.getLayoutData()).widthHint = 100;
			cd = createDecoration(txtValue);
			txtValue.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					modified();
				}
			});
			
		}else if (attribute.getType() == AttributeType.NUMERIC){
			txtValue = new Text(parent, SWT.BORDER);
			txtValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtValue.getLayoutData()).widthHint = 100;
			cd = createDecoration(txtValue);
			txtValue.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					validate();
					modified();
				}
			});
		}else if (attribute.getType() ==  AttributeType.LIST){
			if (!isMulti){
				cmbViewer = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
				cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
				cmbViewer.setLabelProvider(new AttributeListItemLabelProvider());
				List<Object> items = new ArrayList<Object>();
				items.add("");
				items.addAll(attribute.getAttributeList());
				cmbViewer.setInput(items);
				cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						modified();
					}
				});
				cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)cmbViewer.getControl().getLayoutData()).widthHint = 100;
				
				cd = createDecoration(cmbViewer.getControl());
			}else{
				createMultiSelectWidget(parent);
			}
		}else if (attribute.getType() ==  AttributeType.DATE){
			Composite t = new Composite(parent, SWT.NONE);
			t.setLayout(new GridLayout(2, false));
			((GridLayout)t.getLayout()).marginWidth = 0;
			((GridLayout)t.getLayout()).marginHeight = 0;
			
			btnChDateTime = new Button(t, SWT.CHECK);
			btnChDateTime.setSelection(false);
			
			dtDateTime = new DateTime(t, SWT.DROP_DOWN | SWT.DATE | SWT.LONG);
			dtDateTime.setEnabled(false);
			dtDateTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			dtDateTime.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					modified();
				}
			});
			btnChDateTime.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					dtDateTime.setEnabled(btnChDateTime.getSelection());
					modified();
				}
			});
			cd = createDecoration(btnChDateTime);
		}else if (attribute.getType() ==  AttributeType.BOOLEAN){
			Composite t = new Composite(parent, SWT.NONE);
			t.setLayout(new GridLayout(2, false));
			((GridLayout)t.getLayout()).marginWidth = 0;
			((GridLayout)t.getLayout()).marginHeight = 0;
			
			btnChOnOff = new Button(t, SWT.CHECK);
			btnChOnOff.setSelection(false);
			
			btnOnOff = new OnOffButton(t, SWT.TOGGLE);
			btnOnOff.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					modified();
				}
			});
			btnChOnOff.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					btnOnOff.setEnabled(btnChOnOff.getSelection());
					modified();
				}
			});
			btnOnOff.setSelection(true);
			cd = createDecoration(btnOnOff);
		}else if (attribute.getType() == AttributeType.POSITION){
			Composite c = new Composite(parent, SWT.NONE);
			c.setLayout(new GridLayout(4, false));
			((GridLayout)c.getLayout()).marginWidth = 0;
			((GridLayout)c.getLayout()).marginHeight = 0;
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			l = new Label(c, SWT.NONE);
			l.setText("X:");
			l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			
			txtValue = new Text(c, SWT.BORDER);
			txtValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtValue.getLayoutData()).widthHint = 50;
			cd = createDecoration(txtValue);
			txtValue.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					validate();
					modified();
				}
			});
			
			l = new Label(c, SWT.NONE);
			l.setText("Y:");
			l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			
			txtValue2 = new Text(c, SWT.BORDER);
			txtValue2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtValue2.getLayoutData()).widthHint = 50;
			txtValue2.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					validate();
					modified();
				}
			});
		}
	}
	
	private void createMultiSelectWidget(Composite parent){
		cmbMultiSelect = new CheckBoxDropDown(parent){
			@Override
			protected String getTextLabel(Collection<?> objects){
				String value = super.getTextLabel(objects);
				if (!objects.isEmpty()){
					value = "(" + objects.size() + ") " + value;
				}
				return value;
			}
		};
		cmbMultiSelect.setContentProvider(ArrayContentProvider.getInstance());
		cmbMultiSelect.setLabelProvider(new AttributeListItemLabelProvider());
		cmbMultiSelect.setInput(attribute.getAttributeList());
		cmbMultiSelect.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				modified();
			}
		});
		cmbMultiSelect.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)cmbMultiSelect.getLayoutData()).widthHint = 100;
		
		cd = createDecoration(cmbMultiSelect);
	}
	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		cd.hide();
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				cd.dispose();
				
			}
		});
		return cd;
	}
}
