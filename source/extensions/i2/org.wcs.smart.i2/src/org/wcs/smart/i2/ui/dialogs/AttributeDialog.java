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

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelHibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.ui.AttributeTypeLabelProvider;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing intelligence attribute
 * 
 * @author Emily
 *
 */
public class AttributeDialog extends TitleAreaDialog {

	public static void showAttributeDialog(Shell parentShell, IntelAttribute attribute, IEclipseContext context){
		AttributeDialog dialog = new AttributeDialog(parentShell, attribute);
		ContextInjectionFactory.inject(dialog, context);
		dialog.open();
	}
	
	
	@Inject
	private IEventBroker eventBroker;
	
	private IntelAttribute attribute;
	
	private NameKeyComposite nameKeyInfo;
	private ComboViewer cmbType;
	private List<IntelAttribute> attributeSiblings;
	private List<IntelAttributeListItem> allItems;
	
	private AttributeListPanel listPanel;
		
	private AttributeDialog(Shell parentShell, IntelAttribute attribute) {
		super(parentShell);
		this.attribute = attribute;
	}
	
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.4));
	}
	
	@SuppressWarnings("unchecked")
	protected void okPressed() {
		Set<IntelEntity> modifiedEntities = new HashSet<>();
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			for (IntelAttributeListItem i : allItems){
				if(!attribute.getAttributeList().contains(i)){
					//delete all reference to attribute list item
					List<IntelEntityAttributeValue> items = s.createCriteria(IntelEntityAttributeValue.class)
							.add(Restrictions.eq("attributeListItem", i))
							.list();
					for (IntelEntityAttributeValue item : items){
						modifiedEntities.add(item.getEntity());
						s.delete(item);
					}
					
					List<IntelEntityRelationshipAttributeValue> items2 = s.createCriteria(IntelEntityRelationshipAttributeValue.class)
							.add(Restrictions.eq("attributeListItem", i))
							.list();
					for (IntelEntityRelationshipAttributeValue item : items2){
						modifiedEntities.add(item.getRelationship().getSourceEntity());
						modifiedEntities.add(item.getRelationship().getTargetEntity());
						s.delete(item);
					}
					
					Query q = s.createQuery("DELETE FROM IntelRecordAttributeValueList where id.elementUuid = :uuid");
					q.setParameter("uuid", i.getUuid());
					q.executeUpdate();
				}
			}
			s.flush();
			s.clear();
			
			s.saveOrUpdate(attribute);
			
			this.allItems = new ArrayList<IntelAttributeListItem>();
			this.allItems.addAll(attribute.getAttributeList());
			s.getTransaction().commit();
		}catch (Exception ex){
			if (s.getTransaction().isActive())s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Unable to save changes: " +ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		
		if (!modifiedEntities.isEmpty()) eventBroker.send(IntelEvents.ENTITY_MODIFIED, modifiedEntities);		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
		initFields();
	}
	
	private void modified(){
		if (!nameKeyInfo.validate()){
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}else{
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		nameKeyInfo = new NameKeyComposite();
		nameKeyInfo.createControls(parent, true, attribute.getUuid() == null, new IChangeListener() {
			@Override
			public void itemModified() {
				if (!nameKeyInfo.validate()){
					nameKeyInfo.updateFields(attribute);
				}
				modified();
			}
		});
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Type:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		cmbType = new ComboViewer(parent);
		cmbType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new AttributeTypeLabelProvider());
		cmbType.setInput(AttributeType.values());
		cmbType.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				modified();
				
				AttributeType type = (AttributeType) ((IStructuredSelection)cmbType.getSelection()).getFirstElement();
				if (type != AttributeType.LIST){
					//clean out list items
					listPanel.setVisible(false);
					if (attribute.getAttributeList() != null){
						for (IntelAttributeListItem i: attribute.getAttributeList()){
							i.setAttribute(null);
						}
						attribute.getAttributeList().clear();
					}
				}else{
					listPanel.setVisible(true);
				}
				attribute.setType(type);
				
			}
		});
		
		listPanel = new AttributeListPanel(parent);
		listPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		listPanel.addChangeListener(new IChangeListener() {
			@Override
			public void itemModified() {
				modified();
			}
		});
		
		setTitle("Intelligence Attribute");
		getShell().setText("Intelligence Attribute");
		setMessage("Create or edit intelligence attributes.");
		
		return parent;
	}
	
	private void initFields(){
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
		pmd.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				Session s = HibernateManager.openSession();
				try{
					if (attribute.getUuid() != null){
						attribute = (IntelAttribute) s.get(IntelAttribute.class, attribute.getUuid());
						attribute.getNames().size();
						if (attribute.getAttributeList() != null){
							for (IntelAttributeListItem item : attribute.getAttributeList()){
								item.getNames().size();
							}
						}
					}
					attributeSiblings = IntelHibernateManager.getAttributes(s, SmartDB.getCurrentConservationArea());
					attributeSiblings.remove(attribute);
				}finally{
					s.close();
				}
				
				allItems = new ArrayList<IntelAttributeListItem>();
				if (attribute.getAttributeList() != null){
					allItems.addAll(attribute.getAttributeList());
				}
				
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						nameKeyInfo.initFields(attribute, attributeSiblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());					
						getButton(IDialogConstants.OK_ID).setEnabled(attribute.getUuid() == null);
						if (attribute.getType() != null){
							cmbType.setSelection(new StructuredSelection(attribute.getType()));
						}else{
							cmbType.setSelection(new StructuredSelection(AttributeType.NUMERIC));
						}
						
						cmbType.getControl().setEnabled(attribute.getUuid() == null);
						listPanel.setInput(attribute);
					}
				});				
			}
		});
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(MessageFormat.format("Uanble to load attribute: {0}", ex.getMessage()), ex);
		}
		
		
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
