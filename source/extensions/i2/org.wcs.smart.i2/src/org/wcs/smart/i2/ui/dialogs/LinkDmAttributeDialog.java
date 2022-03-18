/*
 * Copyright (C) 2020 Wildlife Conservation Society
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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.common.control.NameKeyDialog;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.search.AdvancedEntitySearch;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.views.entity.search.EntitySearchDropPanel;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItemFactory;
import org.wcs.smart.i2.ui.views.query.dropitem.ErrorDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem;
import org.wcs.smart.util.SharedUtils;

/**
 * Dialog for linking entity type to a data model attribute
 * and managing associated active filter.
 * 
 * @author Emily
 *
 */
public class LinkDmAttributeDialog  extends NameKeyDialog<Attribute>{

	private IntelEntityType entityType;
	
	private EntitySearchDropPanel dropArea ;
	private List<IntelEntityTypeAttribute> currentAttributes;
	
	private Label lblError;
	private Composite compError;
	
	protected LinkDmAttributeDialog(Shell parentShell, Attribute item, 
			List<Attribute> siblings, IntelEntityType entityType, List<IntelEntityTypeAttribute> currentAttributes) {
		super(parentShell, item, siblings, entityType.getDmAttribute() == null );
		this.entityType = entityType;
		this.currentAttributes = currentAttributes;
	}

	public Point getInitialSize(){
		return new Point(500,500);
	}
	
	protected String getTitle(){
		return Messages.EntityTypeDialog_DmAttributeTitle;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		super.modified();
		if (entityType.getDmAttribute() != null)  getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public void okPressed(){
		entityType.setActiveFilter(dropArea.getQueryPart());
		super.okPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Composite c = SmartUiUtils.createHeaderLabel(composite, Messages.LinkDmAttributeDialog_DmAttributeHeader);
		c.moveAbove(composite.getChildren()[0]);
		
		((GridLayout)composite.getLayout()).marginWidth = 5;
		((GridLayout)composite.getLayout()).marginHeight = 5;
		
		SmartUiUtils.createHeaderLabel(composite, Messages.LinkDmAttributeDialog_EnabledFilterHEader);
		
		Label l = new Label(composite, SWT.WRAP);
		l.setText(Messages.LinkDmAttributeDialog_2);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 40;
		
		compError = new Composite(composite, SWT.NONE);
		compError.setLayout(new GridLayout(2, false));
		((GridLayout)compError.getLayout()).marginWidth = 0;
		((GridLayout)compError.getLayout()).marginHeight = 0;
		((GridLayout)compError.getLayout()).horizontalSpacing = 0;
		compError.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblErrorImage = new Label(compError, SWT.NONE);
		lblErrorImage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		lblErrorImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		
		lblError = new Label(compError, SWT.WRAP );
		lblError.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lblError.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		lblError.setText(""); //$NON-NLS-1$
		
		compError.setVisible(false);
		
		SashForm part = new SashForm(composite,  SWT.NONE);
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		dropArea = new EntitySearchDropPanel();
		Composite a = dropArea.createComposite(part);
		a.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite temp = new Composite(part, SWT.NONE);
		temp.setLayout(new TableColumnLayout());
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		TableViewer attributeList = new TableViewer(temp, SWT.BORDER);
		attributeList.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn col = new TableViewerColumn(attributeList, SWT.NONE);
		col.setLabelProvider(new AttributeLabelProvider() {
			public String getText(Object element) {
				if (element == Operator.NOT) return Operator.NOT.getLabel(Locale.getDefault());
				return super.getText(element);
			}
		});
		List<Object> attributes = new ArrayList<>();

		try(Session session = HibernateManager.openSession()){
			for (IntelEntityTypeAttribute ab : currentAttributes) {
				if (ab.getAttribute().getType() == AttributeType.LIST) {
					IntelAttribute ia  = session.get(IntelAttribute.class, ab.getAttribute().getUuid());
					ia.getAttributeList().forEach(e->e.getName());
					ab.setAttribute(ia);
					attributes.add(ia);
				}else if (ab.getAttribute().getType() != AttributeType.POSITION &&
						ab.getAttribute().getType() != AttributeType.EMPLOYEE) {
					attributes.add(ab.getAttribute());
					
				}
			}
		}
		attributes.add(0, Messages.LinkDmAttributeDialog_AttributesSection);
		attributes.add(Messages.LinkDmAttributeDialog_OperatorsSection);
		attributes.add(Operator.NOT);
		attributeList.setInput(attributes);
		((TableColumnLayout)temp.getLayout()).setColumnData(col.getColumn(), new ColumnWeightData(1));
		
		attributeList.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object e = attributeList.getStructuredSelection().getFirstElement();
				if (e instanceof IntelAttribute) {
					dropArea.addItem(DropItemFactory.createAttributeDropItem( ((IntelAttribute)e)));
				}else if (e == Operator.NOT){
					dropArea.addItem(DropItemFactory.createNotDropItem());
				}
			}
		});
		
		dropArea.addQueryModifiedListener(e->{
			String filter = dropArea.getQueryPart();
			String isValid = EntityTypeManager.INSTANCE.validateDmAttributeFilter(filter, currentAttributes);
						
			if (isValid != null) {
				compError.setVisible(true);
				super.getButton(IDialogConstants.OK_ID).setEnabled(false);
				lblError.setText(isValid);
				compError.layout();
			}else {
				compError.setVisible(false);
				lblError.setText(""); //$NON-NLS-1$
				super.modified();
			}
		});
		
		parseQueryFilter();
		part.setWeights(new int[] {7,3});
		
		
		return composite;
		
	}
	
	private void parseQueryFilter() {
		if (entityType.getActiveFilter() == null || entityType.getActiveFilter().isEmpty()) return;
		
		ArrayList<DropItem> dropItems = new ArrayList<>();
		
		String[] parts = entityType.getActiveFilter().split("\\" + DropItemFactory.PART_SEPARATOR); //$NON-NLS-1$
		
		for(String p : parts){
			if (p.equalsIgnoreCase(Operator.NOT.getKey())){
				dropItems.add(DropItemFactory.createNotDropItem());
			}else if (p.equalsIgnoreCase(Operator.AND.getKey())){
				OptionDropItem di = OptionDropItem.createAndOrDropItem(true);
				di.setInitialValue(Operator.AND.getKey());
				dropItems.add(di);
			}else if (p.equalsIgnoreCase(Operator.OR.getKey())){
				OptionDropItem di = OptionDropItem.createAndOrDropItem(true);
				di.setInitialValue(Operator.OR.getKey());
				dropItems.add(di);
			
			}else if (p.startsWith(AdvancedEntitySearch.ATTRIBUTE_KEY + DropItemFactory.ITEM_SEPARATOR)){ 
				String[] bits = p.split(" ")[0].split(DropItemFactory.ITEM_SEPARATOR); //$NON-NLS-1$ 
				String key = bits[2];
				IntelAttribute ia = null;
				
				
				for (IntelEntityTypeAttribute iea : currentAttributes) {
					if (iea.getAttribute().getKeyId().equalsIgnoreCase(key)) {
						ia = iea.getAttribute();
					}
				}
				
				if (ia == null) {
					dropItems.add(new ErrorDropItem(MessageFormat.format(Messages.LinkDmAttributeDialog_AttributeNotValid, key)));
					continue;
				}
				try(Session session = HibernateManager.openSession()){
					ia = session.get(IntelAttribute.class,  ia.getUuid());
			
					if (ia.getType() == AttributeType.LIST) {
						String listKey = p.split(" ")[2]; //$NON-NLS-1$
						boolean found = false;
						if (ia.getAttributeList() != null){
							for (IntelAttributeListItem listitem : ia.getAttributeList()){
								listitem.getName();
								if (listitem.getKeyId().equalsIgnoreCase(listKey)){
									found = true;
								}
							}
						}
						if (!found){
							dropItems.add(new ErrorDropItem(MessageFormat.format(Messages.LinkDmAttributeDialog_ListItemNotFound, listKey, ia.getName())) );
							continue;
						}
					}
				}
				try{
					DropItem di = DropItemFactory.createAttributeDropItem(ia);
					if (ia.getType() == IntelAttribute.AttributeType.TEXT){
						String[] queryParts = p.split(" "); //$NON-NLS-1$
						Operator op = Operator.parse(queryParts[1]);
						int startIndex = p.indexOf(queryParts[2], queryParts[0].length());
						String strValue = p.substring(startIndex).trim();
						String value = SharedUtils.stripQuotes(strValue);
						((TextBoxDropItem)di).setInitialValue(op, value);
					}else if (ia.getType() == IntelAttribute.AttributeType.NUMERIC){
						String[] queryParts = p.split(" "); //$NON-NLS-1$
						Operator op = Operator.parse(queryParts[1]);
						String value = queryParts[2];
						((TextBoxDropItem)di).setInitialValue(op, value);
					}else if (ia.getType() == IntelAttribute.AttributeType.DATE){
						String[] queryParts = p.split(" "); //$NON-NLS-1$
						Operator op = Operator.parse(queryParts[1]);
						LocalDate d1 = LocalDate.parse(queryParts[2], DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR ));
						LocalDate d2 = LocalDate.parse(queryParts[4], DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR ));
						((DateDropItem)di).setInitialValue(op, d1, d2);
					}else if (ia.getType() == IntelAttribute.AttributeType.LIST){
						String listKey = p.split(" ")[2]; //$NON-NLS-1$
						((OptionDropItem)di).setInitialValue(listKey);
					}
					dropItems.add(di);
				}catch (Exception ex){
					dropItems.add(new ErrorDropItem(MessageFormat.format(Messages.LinkDmAttributeDialog_ParseError, ex.getMessage())));
				}
			}
		}
		dropArea.initializeItems(dropItems);
	}
}
