package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.ui.AttributeListItemLabelProvider;
import org.wcs.smart.ui.OnOffButton;
import org.wcs.smart.util.SmartUtils;

public class AttributeFieldEditor {

	private IntelAttribute attribute;
	private Composite parent;
	
	private Text txtValue;
	private OnOffButton btnOnOff;
	private ComboViewer cmbViewer ;
	private DateTime dtDateTime;
	private Button btnChDateTime;
	private Button btnChOnOff;
	private ControlDecoration cdNumber;
	
	private List<SelectionListener> listeners = new ArrayList<SelectionListener>();
	
	/**
	 * Assumption is the parent layout is a 2 column grid layout
	 * @param parent
	 * @param attribute
	 */
	public AttributeFieldEditor(Composite parent, IntelAttribute attribute) {
		this.parent = parent;
		this.attribute = attribute;
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
	public boolean isValid(){
		if (cdNumber != null) return !cdNumber.isVisible();
		return true;
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
		if (attribute.getType() == IAttributeType.BOOLEAN){
			if (((OnOffButton)btnOnOff).isEnabled()){
				add = true;
				if (((OnOffButton)btnOnOff).getSelection()){
					value.setNumberValue(1d);
				}else{
					value.setNumberValue(0d);
				}
			}
		}else if (attribute.getType() == IAttributeType.DATE){
			if (((DateTime)dtDateTime).getEnabled()){
				add = true;
				value.setDateValue( SmartUtils.getDate((DateTime)dtDateTime));
			}
		}else if (attribute.getType() == IAttributeType.LIST){
			IStructuredSelection selection = (IStructuredSelection)((ComboViewer)cmbViewer).getSelection();
			if (!selection.isEmpty()){
				Object item = selection.getFirstElement();
				if (item instanceof IntelAttributeListItem){
					add = true;
					value.setAttributeListItem((IntelAttributeListItem) item);
				}
			}
		}else if (attribute.getType() == IAttributeType.NUMERIC){
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
		}else if (attribute.getType() == IAttributeType.TEXT){
			String svalue = ((Text)txtValue).getText();
			if (!svalue.trim().isEmpty()){
				value.setStringValue(svalue.trim());
				add = true;
			}
		}
		return add;
	}
	
	public void initControl(IntelEntityRelationshipAttributeValue value){
		if (attribute.getType() == IAttributeType.TEXT){
			txtValue.setText(value.getStringValue());
		}else if (attribute.getType() == IAttributeType.NUMERIC){
			txtValue.setText(String.valueOf(value.getNumberValue()));
		}else if (attribute.getType() ==  IAttributeType.LIST){
			cmbViewer.setSelection(new StructuredSelection(value.getAttributeListItem()));
		}else if (attribute.getType() ==  IAttributeType.DATE){
			if(value.getDateValue() == null){
				btnChDateTime.setSelection(false);
				dtDateTime.setEnabled(false);
			}else{
				btnChDateTime.setSelection(true);
				SmartUtils.initDateDateTimeWidget(dtDateTime, value.getDateValue());
				dtDateTime.setEnabled(true);
			}
		}else if (attribute.getType() ==  IAttributeType.BOOLEAN){
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
	/**
	 * returns true if the value is set; false if not set and should be removed
	 * from attribute list.
	 * 
	 * @param value
	 * @return
	 */
	public boolean updateValue(IntelEntityAttributeValue value){
		boolean add = false;
		if (attribute.getType() == IAttributeType.BOOLEAN){
			if (((OnOffButton)btnOnOff).isEnabled()){
				add = true;
				if (((OnOffButton)btnOnOff).getSelection()){
					value.setNumberValue(1d);
				}else{
					value.setNumberValue(0d);
				}
			}
		}else if (attribute.getType() == IAttributeType.DATE){
			if (((DateTime)dtDateTime).getEnabled()){
				add = true;
				value.setDateValue( SmartUtils.getTime((DateTime)dtDateTime));
			}
		}else if (attribute.getType() == IAttributeType.LIST){
			IStructuredSelection selection = (IStructuredSelection)((ComboViewer)cmbViewer).getSelection();
			if (!selection.isEmpty()){
				Object item = selection.getFirstElement();
				if (item instanceof IntelAttributeListItem){
					add = true;
					value.setAttributeListItem((IntelAttributeListItem) item);
				}
			}
		}else if (attribute.getType() == IAttributeType.NUMERIC){
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
		}else if (attribute.getType() == IAttributeType.TEXT){
			String svalue = ((Text)txtValue).getText();
			if (!svalue.trim().isEmpty()){
				value.setStringValue(svalue.trim());
				add = true;
			}
		}
		return add;
	}
	
	public void initControl(IntelEntityAttributeValue value){
		if (attribute.getType() == IAttributeType.TEXT){
			txtValue.setText(value.getStringValue());
		}else if (attribute.getType() == IAttributeType.NUMERIC){
			txtValue.setText(String.valueOf(value.getNumberValue()));
		}else if (attribute.getType() ==  IAttributeType.LIST){
			cmbViewer.setSelection(new StructuredSelection(value.getAttributeListItem()));
		}else if (attribute.getType() ==  IAttributeType.DATE){
			if(value.getDateValue() == null){
				btnChDateTime.setSelection(false);
				dtDateTime.setEnabled(false);
			}else{
				btnChDateTime.setSelection(true);
				SmartUtils.initDateDateTimeWidget(dtDateTime, value.getDateValue());
				dtDateTime.setEnabled(true);
			}
		}else if (attribute.getType() ==  IAttributeType.BOOLEAN){
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
		l.setText(attribute.getName() + ":");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		if (attribute.getType() == IAttributeType.TEXT){
			txtValue = new Text(parent, SWT.BORDER);
			txtValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			txtValue.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					modified();
				}
			});
		}else if (attribute.getType() == IAttributeType.NUMERIC){
			txtValue = new Text(parent, SWT.BORDER);
			txtValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cdNumber = createDecoration(txtValue);
			txtValue.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					try{
						Double.parseDouble(txtValue.getText());
						cdNumber.hide();
					}catch(Exception ex){
						cdNumber.show();
						cdNumber.setDescriptionText("Unable to parse number from text");
					}
					modified();
				}
			});
		}else if (attribute.getType() ==  IAttributeType.LIST){
			cmbViewer = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
			cmbViewer.setLabelProvider(AttributeListItemLabelProvider.INSTANCE);
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
		}else if (attribute.getType() ==  IAttributeType.DATE){
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
		}else if (attribute.getType() ==  IAttributeType.BOOLEAN){
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
		}
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
