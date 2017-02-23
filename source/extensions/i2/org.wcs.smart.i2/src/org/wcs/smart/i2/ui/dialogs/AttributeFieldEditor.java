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

import org.eclipse.jface.action.IAction;
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.geotools.referencing.CRS;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.internal.tool.display.ToolManager;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
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
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Attribute field editor 
 * 
 * @author Emily
 *
 */
public class AttributeFieldEditor {

	private static final String SELELECTION_KEY = "sel";
	private IntelAttribute attribute;
	private boolean isMulti;
	private Composite parent;
	
	private Text txtValue;
	private Text txtValue2;
	private Label lblProj;
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
	
	private CoordinateReferenceSystem crs = GeometryUtils.SMART_CRS;
	private String crsLabel = null;
	
	
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
		
		if (attribute.getType() == AttributeType.POSITION){
			
			crs = GeometryUtils.SMART_CRS;
			Projection currentProjection = HibernateManager.getCurrentViewProjection();
			if (currentProjection != null ){
				try{
					CoordinateReferenceSystem parsed = ReprojectUtils.stringToCrs(currentProjection.getDefinition());
					if (!CRS.equalsIgnoreMetadata(crs, parsed)){
						crs= parsed;
					}
				}catch (Exception ex){
					Intelligence2PlugIn.log(ex.getMessage(), ex);
				}
			}
			
			if (currentProjection != null){
				crsLabel = currentProjection.getName();
			}else{
				crsLabel = GeometryUtils.SMART_CRS.getName().toString();
			}
		}
		createControl();
	}

	public void adapt(FormToolkit toolkit){
		List<Control> kids = new ArrayList<Control>();
		kids.add(parent);
		while(!kids.isEmpty()){
			Control kid = kids.remove(0);
			toolkit.adapt(kid, false, false);
			if (kid instanceof Hyperlink){
				kid.setForeground(kid.getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND));
			}
			
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
			Double x = null;
			Double y = null;
			try{
				if (!txtValue.getText().trim().isEmpty()){
					x = Double.parseDouble(txtValue.getText());
				}
				if (!txtValue2.getText().trim().isEmpty()){
					y = Double.parseDouble(txtValue2.getText());
				}
			}catch(Exception ex){
				msg = "Unable to positiong coorindate numbers from text";
			}
			//try to reproject to database crs
			if (x != null && y != null){
				if (crs != null && crs != GeometryUtils.SMART_CRS){
					//reproject to lat lon
					try{
						ReprojectUtils.reproject(x, y, crs, GeometryUtils.SMART_CRS);
					}catch (Exception ex){
						msg = "Unable to reproject position attribute to database projection";
					}
				}
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
			Double[] values = parsePositionValues();
			if (values == null){
				add = false;
			}else{
				value.setNumberValue(values[0]);
				value.setNumberValue2(values[1]);
				add = true;
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
			Double[] values = parsePositionValues();
			if (values == null){
				add = false;
			}else{
				value.setNumberValue(values[0]);
				value.setNumberValue2(values[1]);
				add = true;
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
			initPositionValues(value.getNumberValue(), value.getNumberValue2());
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
			Double[] values = parsePositionValues();
			if (values == null){
				add = false;
			}else{
				value.setNumberValue(values[0]);
				value.setNumberValue2(values[1]);
				add = true;
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
			initPositionValues(value.getNumberValue(), value.getNumberValue2());
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
			initPositionValues(value.getNumberValue(), value.getNumberValue2());
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
	
	private void initPositionValues(Double value1, Double value2){
		if (value1 == null || value2 == null){
			txtValue.setText("");
			txtValue2.setText("");
		}
		
		//get view projection
		
		lblProj.setToolTipText(crsLabel);
		if (crsLabel.length() > 10){
			crsLabel = crsLabel.substring(0, 10) + "...";
		}
		lblProj.setText(crsLabel);
		if (crs == GeometryUtils.SMART_CRS){
			txtValue.setText(String.valueOf(value1));
			txtValue2.setText(String.valueOf(value2));
		}else{
			try {
				Coordinate viewCoordinate = ReprojectUtils.reproject(value1, value2, GeometryUtils.SMART_CRS, crs);
				txtValue.setText(String.valueOf(viewCoordinate.x));
				txtValue2.setText(String.valueOf(viewCoordinate.y));
			} catch (Exception e) {
				Intelligence2PlugIn.displayLog("Unable to reproject position attribute to view projection.", e);
				txtValue.setText(String.valueOf(value1));
				txtValue2.setText(String.valueOf(value2));
			}
		}		
	}
	
	private Double[] parsePositionValues(){
		Double x = null;
		Double y = null;
		try{
			String dvalue = ((Text)txtValue).getText();
			if (!dvalue.trim().isEmpty()){
				x = Double.parseDouble(dvalue);
			}
		}catch (Exception ex){ }
		
		try{
			String dvalue = ((Text)txtValue2).getText();
			if (!dvalue.trim().isEmpty()){
				y = Double.parseDouble(dvalue);
			}
		}catch (Exception ex){ }
		
		if (x == null || y == null) return null;
		
		
		if (crs == null || crs == GeometryUtils.SMART_CRS){
			return new Double[]{x,y};
		}else{
			//reproject to lat lon
			try{
				Coordinate c = ReprojectUtils.reproject(x, y, crs, GeometryUtils.SMART_CRS);
				return new Double[]{c.x, c.y};
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog("Unable to reproject position attribute to database projection", ex);
				return new Double[]{x,y};
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
				cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				((GridData)cmbViewer.getControl().getLayoutData()).widthHint = 100;
				
				cd = createDecoration(cmbViewer.getControl());
			}else{
				CheckBoxDropDown control = createMultiSelectWidget(parent);
				cd = createDecoration(control);
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
			c.setLayout(new GridLayout(5, false));
			((GridLayout)c.getLayout()).marginWidth = 1;
			((GridLayout)c.getLayout()).marginHeight = 2;
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			c.addListener(SWT.Paint, e->{
				GC gc = e.gc;
				if (c.getData(SELELECTION_KEY) != null && (boolean)c.getData(SELELECTION_KEY)){
					gc.setForeground(c.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
				}else{
					gc.setForeground(c.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
				}
				gc.setLineWidth(1);
				gc.drawRectangle(0, 0, c.getBounds().width-1, c.getBounds().height-1);
			});
			
			txtValue = new Text(c, SWT.NONE);
			txtValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)txtValue.getLayoutData()).widthHint = 50;
			cd = createDecoration(txtValue);
				
			l = new Label(c, SWT.NONE);
			l.setText(":");
			l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

			txtValue2 = new Text(c, SWT.NONE);
			txtValue2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)txtValue2.getLayoutData()).widthHint = 50;
			
			for (Text t : new Text[]{txtValue, txtValue2}){
				t.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						validate();
						modified();
					}
				});
				t.addListener(SWT.FocusIn, e->{
					c.setData(SELELECTION_KEY, true);
					c.redraw();
					t.setBackground(txtValue.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
				});
				t.addListener(SWT.FocusOut, e->{
					c.setData(SELELECTION_KEY, false);
					c.redraw();
					t.setBackground(c.getBackground());
					t.setSelection(0);
				});
			}

			
			
			Hyperlink link = new Hyperlink(c, SWT.NONE);
			link.setText("map...");
			link.setUnderlined(true);
			link.setForeground(link.getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND));
			link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			link.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					selectOnMap(link.getShell());
				}
			});
			
			lblProj = new Label(c, SWT.NONE);
			lblProj.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
//			((GridData)lblProj.getLayoutData()).widthHint = 20;
			FontData fc = lblProj.getFont().getFontData()[0];
			fc.setHeight(fc.getHeight() - 2);
			Font smaller = new Font(lblProj.getDisplay(), fc);
			lblProj.setFont(smaller);
			lblProj.addListener(SWT.Dispose, e-> smaller.dispose());
		}
	}
	
	private void selectOnMap(Shell parent){
		
		SelectPointMapDialog md = new SelectPointMapDialog(parent);
		Double[] position = parsePositionValues();
		if (position != null){
			md.setInitPoint(position[0], position[1]);
		}
		
		MapPart currentPart = ApplicationGIS.getToolManager().getActiveTool().getContext().getViewportPane().getMapEditor();
		IAction lastToolAction = ((ToolManager)ApplicationGIS.getToolManager()).getActiveToolProxy().getAction();
		try{
			if (md.open() == SelectPointMapDialog.OK){
				if (md.getPoint() != null){
					double x = md.getPoint().getX();
					double y = md.getPoint().getY();
					initPositionValues(x, y);
				}
			}
		}finally{
			ApplicationGIS.getToolManager().setCurrentEditor(currentPart);
			lastToolAction.run();
		}
	}
	
	private CheckBoxDropDown createMultiSelectWidget(Composite parent){
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
		
		return cmbMultiSelect;
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
