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
package org.wcs.smart.observation.query.ui;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.ui.ca.datamodel.AttributeFieldFactory;
import org.wcs.smart.ui.ca.datamodel.IAttributeField;
import org.wcs.smart.ui.properties.CategoryTreeDropDown;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog box for editing an observation.  Allows user to
 * edit the category and associated categories.  Does not
 * make modifications to the database; to get the new observation
 * values call getUpdatedObservation
 * 
 * @author Emily
 *
 */
public class EditObservationDialog extends TitleAreaDialog{

	private UUID woUuid;
	private WaypointObservation toEdit;
	private WaypointObservation updatedObservation;
	
	private ScrolledComposite attributeComposite;
	private Composite attributeKidComposite;
	private List<IAttributeField<?>> attributeFields;
	
	private CategoryTreeDropDown categoryViewer;
	private Category lastCategory = null;
	
	public EditObservationDialog(Shell parentShell, UUID woUuid) {
		super(parentShell);
		this.woUuid = woUuid;
		
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	protected void cancelPressed() {
		updatedObservation = null;
		super.cancelPressed();
	}
	
	@Override
	protected void okPressed() {
		
		for (IAttributeField<?> field : attributeFields){
			String error = field.validate();
			if (error != null){
				MessageDialog.openError(getShell(), Messages.EditObservationDialog_ErrorDialogTitle, error);
				return;
			}
		}
		
		updatedObservation = new WaypointObservation();
		updatedObservation.setUuid(woUuid);
		if (updatedObservation.getAttributes() == null) updatedObservation.setAttributes(new ArrayList<WaypointObservationAttribute>());
		
		Category c = categoryViewer.getValue();
		updatedObservation.getAttributes().clear();
		updatedObservation.setCategory(c);
		
		for (IAttributeField<?> field : attributeFields){
			WaypointObservationAttribute a =  new WaypointObservationAttribute();
			a.setAttribute(field.getAttribute());
			a.setAttributeValue(field.getValue());
			a.setObservation(updatedObservation);
			updatedObservation.getAttributes().add(a);
			
		} 
		
		setReturnCode(OK);
		close();
	}

	/**
	 * 
	 * @return the updated waypoint observation value
	 */
	public WaypointObservation getUpdatedObservation(){
		return updatedObservation;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	public Point getInitialSize(){
		Point p = super.getInitialSize();
		if (p.y > 400) p.y = 400;
		return p;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Control createDialogArea(Composite parent) {
		List<Attribute> allAttributes = new ArrayList<>();
		Category category = null;
		
		Session s = HibernateManager.openSession();
		try{
			if (woUuid == null){
				toEdit = new WaypointObservation();
				toEdit.setAttributes(new ArrayList<>());
			}else{
				toEdit = (WaypointObservation) s.get(WaypointObservation.class, woUuid);
				toEdit.getWaypoint().getId();
				toEdit.getWaypoint().getDateTime();
			}
			
			category = toEdit.getCategory();
			if (category != null){
				category.getAllAttribute(allAttributes, true);
			}
			
			parent = (Composite)super.createDialogArea(parent);
			
			Composite all = new Composite(parent,  SWT.NONE);
			all.setLayout(new GridLayout(1, false));
			all.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
			List<Category> roots = s.createCriteria(Category.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.add(Restrictions.isNull("parent")) //$NON-NLS-1$
					.list();
			
			List<Category> lazy = new ArrayList<>(roots);
			while(!lazy.isEmpty()){
				Category l = lazy.remove(0);
				lazy.addAll(l.getActiveChildren());
				l.getName();
			}
			
			categoryViewer = new CategoryTreeDropDown();
			categoryViewer.createComposite(all);
			all.addListener(SWT.Dispose, e->categoryViewer.dispose());
			categoryViewer.setInput(roots);
			categoryViewer.setValue(category);
			categoryViewer.addSelectionChangedListener(e-> {
					Session session = HibernateManager.openSession();
					try{
						Category c = (Category) session.get(Category.class, categoryViewer.getValue().getUuid());
						List<Attribute> allatts = new ArrayList<>();
						c.getAllAttribute(allatts, true);
						if (!c.equals(lastCategory)) configureAttribute(allatts);
						lastCategory = c;
					}finally{
						session.close();
					}
					
			});
			
			Label seperatorLabel = new Label(all, SWT.SEPARATOR | SWT.HORIZONTAL);
			seperatorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			attributeComposite = new ScrolledComposite(all, SWT.V_SCROLL | SWT.NONE);
			attributeComposite.setLayout(new GridLayout());
			attributeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			attributeComposite.setExpandHorizontal(true);
			attributeComposite.setExpandVertical(true);
			
			lastCategory = category;
			configureAttribute(allAttributes);
		}finally{
			s.close();
		}
		
		setTitle(Messages.EditObservationDialog_Title);
		if (toEdit.getWaypoint() == null){
			setMessage(Messages.EditObservationDialog_NewMessage);
		}else{
			setMessage(MessageFormat.format(Messages.EditObservationDialog_EditMessage, DateFormat.getDateTimeInstance().format(toEdit.getWaypoint().getDateTime()), toEdit.getWaypoint().getId()));
		}
		getShell().setText(Messages.EditObservationDialog_ShellTitle);
		return parent;
	}


	private void configureAttribute(List<Attribute> allAttributes){
		if (attributeKidComposite != null){
			attributeKidComposite.dispose();
		}
		attributeKidComposite = new Composite(attributeComposite, SWT.NONE);
		attributeKidComposite.setLayout(new GridLayout(2, false));
		attributeKidComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		attributeComposite.setContent(attributeKidComposite);
		
		attributeFields = new ArrayList<>();
		for (Attribute attribute : allAttributes){
			IAttributeField<?> field = AttributeFieldFactory.findAttributeField(attribute);
			
			field.createComposite(attributeKidComposite);
			attributeFields.add(field);
			attributeKidComposite.addListener(SWT.Dispose,e->field.dispose());
			
			for (WaypointObservationAttribute a : toEdit.getAttributes()){
				if (a.getAttribute().equals(attribute)){
					field.setValue(a.getAttributeValue());
					break;
				}
			}
		}
		
		attributeComposite.layout(true,true);
		attributeComposite.setMinSize(attributeKidComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
}
