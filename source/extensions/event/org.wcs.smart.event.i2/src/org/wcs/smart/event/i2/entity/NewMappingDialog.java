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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
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
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.event.i2.entity.EntityMapping.Type;
import org.wcs.smart.event.i2.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.dialogs.AttributeFieldEditor;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog for creating entity mappings for the create entity action
 * 
 * @author Emily
 *
 */
public class NewMappingDialog extends SmartStyledTitleDialog {

	private static final String ATTRIBUTE_KEY = "ATTRIBUTE"; //$NON-NLS-1$
	private static final String EDITOR_KEY = "EDITOR"; //$NON-NLS-1$
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
	
	private TableViewer tblList;
	private ComboBoxViewerCellEditor cellEditor ;
	
	public NewMappingDialog(Shell parentShell, IntelEntityType type, List<Attribute> dmAttributes) {
		super(parentShell);
		
		this.type = type;
		this.dmAttributes = dmAttributes;
	}

	public NewMappingDialog(Shell parentShell, IntelEntityType type, List<Attribute> dmAttributes, EntityMapping toEdit) {
		this(parentShell, type, dmAttributes);
		this.mapping = toEdit;
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
			
			
			Attribute dmAttribute = (Attribute)cmbDmAttribute.getStructuredSelection().getFirstElement();
			if (dmAttribute == null) {
				MessageDialog.openInformation(getParentShell(), Messages.NewMappingDialog_WarningTitle, Messages.NewMappingDialog_WarningMsg);
				return;
			}
				
		}
		if (btnObsPosition.getSelection()) {
			type = EntityMapping.Type.POSITION;
		}
		mapping = new EntityMapping(type);
		
		IntelEntityTypeAttribute attribute = (IntelEntityTypeAttribute) cmbIntelAttribute.getStructuredSelection().getFirstElement();
		mapping.setEntityAttribute(attribute.getAttribute());
		
		if (btnFixed.getSelection()) {
			AttributeFieldEditor fieldEditor = (AttributeFieldEditor) cmpFixedMap.getData(EDITOR_KEY);
			IntelEntityAttributeValue temp = new IntelEntityAttributeValue();
			temp.setAttribute(attribute.getAttribute());
			fieldEditor.updateValue(temp);
			
			switch(attribute.getAttribute().getType()) {
			case BOOLEAN:
				if (temp.getNumberValue() == null) {
					mapping = null;
					super.okPressed();
					return;
				}
				mapping.setFixedValue(temp.getNumberValue() >= 0.5);
				break;
			case DATE:
				if (temp.getDateValue() == null) {
					mapping = null;
					super.okPressed();
					return;
				}
				mapping.setFixedValue(temp.getDateValue());
				break;
			case EMPLOYEE:
				if (temp.getEmployee() == null) {
					mapping = null;
					super.okPressed();
					return;
				}
				mapping.setFixedEmployee(temp.getEmployee());
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
			mapping.getListItemMappings().clear();
			if (dmAttribute.getType() == Attribute.AttributeType.LIST) {
				List<ListItemMapping> mappings = (List<ListItemMapping>) tblList.getInput();
				for (ListItemMapping m : mappings) {
					if (m.dmItem == null) continue;
					mapping.addListItemMapping(m.iItem, m.dmItem);
				}
			}
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
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Label l = new Label(temp, SWT.NONE);
		l.setText(Messages.NewMappingDialog_AttributeLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbIntelAttribute = new ComboViewer(temp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbIntelAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbIntelAttribute.setContentProvider(ArrayContentProvider.getInstance());
		cmbIntelAttribute.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelEntityTypeAttribute) {
					return MessageFormat.format("{0} ({1})", ((IntelEntityTypeAttribute)element).getAttribute().getName(), ((IntelEntityTypeAttribute)element).getAttribute().getType().name().toLowerCase()); //$NON-NLS-1$
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
		l.setText(Messages.NewMappingDialog_MapToLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, true));
		
		Composite addArea = new Composite(temp, SWT.NONE);
		addArea.setLayout(new GridLayout(1, false));
		addArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		btnObsPosition = new Button(addArea, SWT.RADIO);
		btnObsPosition.setText(Messages.NewMappingDialog_PositionLabel);
		btnObsPosition.setEnabled(false);
		
		btnFixed = new Button(addArea, SWT.RADIO);
		btnFixed.setText(Messages.NewMappingDialog_FixedLabel);
		btnFixed.setSelection(true);
		
		cmpFixedMap = new Composite(addArea, SWT.NONE);
		cmpFixedMap.setLayout(new GridLayout(2, false));
		cmpFixedMap.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmpFixedMap.setEnabled(true);
		 
		btnDm = new Button(addArea, SWT.RADIO);
		btnDm.setText(Messages.NewMappingDialog_DmAttributeLabel);
		
		cmpDmMap = new Composite(addArea, SWT.NONE);
		cmpDmMap.setLayout(new GridLayout());
		cmpDmMap.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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
					return MessageFormat.format("{0} ({1})", ((Attribute) element).getName(), ((Attribute) element).getType().name().toLowerCase()); //$NON-NLS-1$
				}
				return super.getText(element);
			}
		});
		
		cmbDmAttribute.addSelectionChangedListener(e->{
			Attribute dAttribute = (Attribute) cmbDmAttribute.getStructuredSelection().getFirstElement();
			if (dAttribute == null) return;
			try(Session s = HibernateManager.openSession()){
				dAttribute = s.get(Attribute.class, dAttribute.getUuid());
				dAttribute.getAttributeList().forEach(a->a.getName());
			}
			
			//clear mappings if different attribute
			List<ListItemMapping> itemMappings = (List<ListItemMapping>) tblList.getInput();
			Attribute existingAttribute = null;
			for (ListItemMapping m : itemMappings) {
				if (m.dmItem != null) {
					existingAttribute = m.dmItem.getAttribute();
					break;
				}
			}
			if (!dAttribute.equals(existingAttribute)) {
				itemMappings.forEach(z->z.dmItem = null);
				tblList.refresh();	
			}
			
			if (dAttribute.getType() != Attribute.AttributeType.LIST) {
				cellEditor.setInput(Collections.emptyList());
				return;
			}
			List<Object> items = new ArrayList<>();
			items.add(""); //$NON-NLS-1$
			items.addAll(dAttribute.getAttributeList());
			cellEditor.setInput(items);
			
		});
		
		tblList = new TableViewer(cmpDmMap, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
		tblList.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblList.getTable().setHeaderVisible(true);
		tblList.getTable().setLinesVisible(true);
		tblList.setContentProvider(ArrayContentProvider.getInstance());
		tblList.getTable().setEnabled(false);
		
		TableViewerColumn iItemColumn = new TableViewerColumn(tblList, SWT.NONE);
		iItemColumn.getColumn().setText(Messages.NewMappingDialog_IListItems);
		iItemColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((ListItemMapping)element).iItem.getName() + " (" + ((ListItemMapping)element).iItem.getKeyId() +")"; //$NON-NLS-1$ //$NON-NLS-2$
				
			}
		});
		iItemColumn.getColumn().pack();
		
		TableViewerColumn dmItemColumn = new TableViewerColumn(tblList, SWT.NONE);
		dmItemColumn.getColumn().setText(Messages.NewMappingDialog_DmListItem);
		dmItemColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ListItemMapping m = (ListItemMapping)element;
				if (m.dmItem == null) return ""; //$NON-NLS-1$
				return m.dmItem.getName() + " (" + m.dmItem.getKeyId() +")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		dmItemColumn.getColumn().pack();
		
		dmItemColumn.setEditingSupport(new EditingSupport(dmItemColumn.getViewer()) {
			
			@Override
			protected void setValue(Object element, Object value) {
				if (value instanceof AttributeListItem) {
					((ListItemMapping)element).dmItem = (AttributeListItem) value;
				}else {
					((ListItemMapping)element).dmItem = null;
				}
				tblList.refresh();
			}
			
			@Override
			protected Object getValue(Object element) {
				return ((ListItemMapping)element).dmItem;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return cellEditor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		cellEditor = new ComboBoxViewerCellEditor(tblList.getTable(), SWT.DROP_DOWN | SWT.READ_ONLY);
		cellEditor.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AttributeListItem) {
					return ((AttributeListItem)element).getName() + " (" + ((AttributeListItem)element).getKeyId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return super.getText(element);
			}
		});
		cellEditor.setContentProvider(ArrayContentProvider.getInstance());
		
		cmbIntelAttribute.setInput(type.getAttributes());
		cmbIntelAttribute.setSelection(new StructuredSelection(type.getAttributes().get(0)));
		
		
		//initialize controls
		if (mapping != null) {
			for (IntelEntityTypeAttribute a : type.getAttributes()) {
				if (a.getAttribute().equals(mapping.getEntityAttribute())) {
					cmbIntelAttribute.setSelection(new StructuredSelection(a));		
				}
			}
			
			if (mapping.getType() == Type.DM) {
				btnDm.setSelection(true);
				btnFixed.setSelection(false);
				btnObsPosition.setSelection(false);
				cmbDmAttribute.setSelection(new StructuredSelection(mapping.getDataModelAttribute()));
				configureFixedValue();
				configureDm();
			
			}else if (mapping.getType() == Type.FIXED) {
				btnDm.setSelection(false);
				btnFixed.setSelection(true);
				btnObsPosition.setSelection(false);
				
				configureFixedValue();
				configureDm();
			}else if (mapping.getType() == Type.POSITION) {
				btnDm.setSelection(false);
				btnFixed.setSelection(false);
				btnObsPosition.setSelection(true);
				configureFixedValue();
				configureDm();
			}
			
			
			List<ListItemMapping> mappings = (List<ListItemMapping>) tblList.getInput();
			if (mappings != null) {
				Attribute dAttribute = mapping.getDataModelAttribute();
				if (dAttribute != null) {
					try(Session s = HibernateManager.openSession()){
						dAttribute = s.get(Attribute.class, dAttribute.getUuid());
						dAttribute.getAttributeList().forEach(a->a.getName());
					}	
				}
				
				for (ListItemMapping mi : mappings) {
					String diKey = mapping.getListItemMappings().get(mi.iItem.getKeyId());
					for (AttributeListItem ii : dAttribute.getAttributeList()) {
						if (ii.getKeyId().equals(diKey)) {
							mi.dmItem = ii;
							break;
						}
					}
				}
			}
			tblList.refresh();
		}
		
		setTitle(type.getName());
		setMessage(MessageFormat.format(Messages.NewMappingDialog_Message, type.getName()));
		getShell().setText(Messages.NewMappingDialog_Title);
		return parent;
	}
	
	private void configureFixedValue() {
		setEnabled(cmpFixedMap, btnFixed.getSelection());
		IntelEntityTypeAttribute ea = (IntelEntityTypeAttribute) cmbIntelAttribute.getStructuredSelection().getFirstElement();
		if (ea == null) return;
		Object x = cmpFixedMap.getData(ATTRIBUTE_KEY);
		if (x != null && x == ea.getAttribute()) return;
		for (Control c : cmpFixedMap.getChildren()) c.dispose();
		
		//create entity editor
		AttributeFieldEditor fieldEditor = new AttributeFieldEditor(cmpFixedMap, ea.getAttribute(), false);
		cmpFixedMap.setData(EDITOR_KEY, fieldEditor);
		cmpFixedMap.setData(ATTRIBUTE_KEY, ea.getAttribute());
		cmpFixedMap.layout(true);
		
		if (mapping != null && mapping.getType() == Type.FIXED && mapping.getEntityAttribute().equals(ea.getAttribute())) {
			IntelEntityAttributeValue temp = new IntelEntityAttributeValue();
			temp.setAttribute(ea.getAttribute());
			switch(ea.getAttribute().getType()){
			case BOOLEAN:
				temp.setNumberValue(mapping.getFixedBooleanValue() ? 1.0 : 0.0);
				break;
			case DATE:
				temp.setDateValue(mapping.getFixedDateValue());
				break;
			case EMPLOYEE:
				temp.setEmployee(mapping.getFixedEmployee());
				break;
			case LIST:
				temp.setAttributeListItem(mapping.getIntelListItem());
				break;
			case NUMERIC:
				temp.setNumberValue(mapping.getFixedDouble1Value());
				break;
			case POSITION:
				temp.setNumberValue(mapping.getFixedDouble1Value());
				temp.setNumberValue2(mapping.getFixedDouble2Value());
				break;
			case TEXT:
				temp.setStringValue(mapping.getFixedStringValue());
				break;
			default:
				break;
				
			}
			fieldEditor.initControl(temp);
		}else {
			IntelEntityAttributeValue temp = new IntelEntityAttributeValue();
			temp.setAttribute(ea.getAttribute());
			if (ea.getAttribute().getType() == IntelAttribute.AttributeType.DATE) {
				temp.setDateValue(new Date());
				fieldEditor.initControl(temp);
			}else if (ea.getAttribute().getType() == IntelAttribute.AttributeType.BOOLEAN) {
				temp.setNumberValue(1.0);
				fieldEditor.initControl(temp);
			}
		}
				
		setEnabled(cmpFixedMap, cmpFixedMap.getEnabled());
	}
	
	private void configureDm() {
		setEnabled(cmpDmMap, btnDm.getSelection());
		if (dmAttributes == null) return;
		IntelEntityTypeAttribute ea = (IntelEntityTypeAttribute) cmbIntelAttribute.getStructuredSelection().getFirstElement();
		if (ea == null) {
			cmbDmAttribute.setInput(Collections.emptyList());
			return;
		}
		Object x = cmpDmMap.getData(ATTRIBUTE_KEY);
		if (x != null && x == ea.getAttribute()) return;
		tblList.getTable().setVisible(ea.getAttribute().getType() == AttributeType.LIST);
		cellEditor.setInput(Collections.emptyList());
		
		cmpDmMap.setData(ATTRIBUTE_KEY, ea.getAttribute());
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
		setEnabled(cmpDmMap, btnDm.getSelection());
		
		IntelAttribute iAttribute = ea.getAttribute();
		if (iAttribute.getType() != IntelAttribute.AttributeType.LIST) {
			tblList.setInput(Collections.emptyList());
		}else {
			Attribute dAttribute = (Attribute) cmbDmAttribute.getStructuredSelection().getFirstElement();
			if (dAttribute != null) {
				try(Session s = HibernateManager.openSession()){
					dAttribute = s.get(Attribute.class, dAttribute.getUuid());
					dAttribute.getAttributeList().forEach(a->a.getName());
				}	
			}
			
			List<ListItemMapping> itemMappings = new ArrayList<>();
			
			for (IntelAttributeListItem iItem : iAttribute.getAttributeList()) {
				ListItemMapping mp = new ListItemMapping();
				mp.iItem = iItem;
				itemMappings.add(mp);
		
				String dmKey = iItem.getKeyId();
				if (dAttribute != null) {
					for (AttributeListItem i : dAttribute.getAttributeList()) {
						if (i.getKeyId().equals(dmKey)) {
							mp.dmItem = i;
							break;
						}
					}
				}
			}
			itemMappings.sort((a,b)->Collator.getInstance().compare(a.iItem.getName(), b.iItem.getName()));
			tblList.setInput(itemMappings);
			tblList.refresh();
			for (TableColumn tc : tblList.getTable().getColumns()) {
				tc.pack();
				if (tc.getWidth() > 200) tc.setWidth(200);
			}
		}
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
	
	private class ListItemMapping{
		AttributeListItem dmItem;
		IntelAttributeListItem iItem;
	}
}
