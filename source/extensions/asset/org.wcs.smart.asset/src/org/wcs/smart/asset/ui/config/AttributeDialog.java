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
package org.wcs.smart.asset.ui.config;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
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
import org.hibernate.Session;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetHibernateManager;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.asset.ui.AttributeTypeLabelProvider;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing asset attributes
 * 
 * @author Emily
 *
 */
public class AttributeDialog extends SmartStyledTitleDialog {

	public static void showAttributeDialog(Shell parentShell, AssetAttribute attribute, IEclipseContext context){
		AttributeDialog dialog = new AttributeDialog(parentShell, attribute);
		ContextInjectionFactory.inject(dialog, context);
		dialog.open();
	}
	
	
	@Inject
	private IEventBroker eventBroker;
	
	private AssetAttribute attribute;
	
	private NameKeyComposite nameKeyInfo;
	private ComboViewer cmbType;
	private List<AssetAttribute> attributeSiblings;
	//private List<AssetAttributeListItem> allItems;
	private List<AssetAttributeListItem> currentItem = Collections.emptyList();
	
	private AttributeListPanel listPanel;
		
	private AttributeDialog(Shell parentShell, AssetAttribute attribute) {
		super(parentShell);
		this.attribute = attribute;
	}
	
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.4));
	}
	
	protected void okPressed() {
		Set<Asset> modifiedAssets = new HashSet<>();
		Set<AssetDeployment> modifiedDeployments = new HashSet<>();
		Set<AssetStation> modifiedStations = new HashSet<>();

		
		try(Session s = HibernateManager.openSession()){
			try{
				s.beginTransaction();
				
				if (this.attribute.getUuid() == null) {
					s.persist(this.attribute);
				}else {
					//save any new items
					this.attribute = HibernateManager.saveOrMerge(s, attribute);
				}
				s.flush();
				
				List<AssetAttributeListItem> delete = new ArrayList<>();
				for (AssetAttributeListItem i : attribute.getAttributeList()){				
					if(!currentItem.contains(i)){

						//delete all reference to attribute list item
						
						List<AssetAttributeValue> items = QueryFactory.buildQuery(s, AssetAttributeValue.class, "attributeListItem", i).getResultList();  //$NON-NLS-1$
						for (AssetAttributeValue item : items){
							modifiedAssets.add(item.getAsset());
							s.remove(item);
						}
						
						List<AssetDeploymentAttributeValue> items2 = QueryFactory.buildQuery(s, AssetDeploymentAttributeValue.class, "attributeListItem", i).getResultList();  //$NON-NLS-1$
						for (AssetDeploymentAttributeValue item : items2){
							modifiedDeployments.add(item.getAssetDeployment());
							s.remove(item);
						}
						
						List<AssetStationAttributeValue> items3 = QueryFactory.buildQuery(s, AssetStationAttributeValue.class, "attributeListItem", i).getResultList();  //$NON-NLS-1$
						for (AssetStationAttributeValue item : items3){
							modifiedStations.add(item.getStation());
							s.remove(item);
						}
						
						s.createMutationQuery("DELETE FROM IntelRecordAttributeValueList where id.elementUuid = :uuid") //$NON-NLS-1$
							.setParameter("uuid", i.getUuid()) //$NON-NLS-1$
							.executeUpdate();
						delete.add(i);
								
						
					}
				}
				this.attribute.getAttributeList().removeAll(delete);
				s.flush();

				for (AssetAttributeListItem i : currentItem){
					if (i.getUuid() == null) {
						i.setAttribute(this.attribute);
						s.persist(i);
					}else {
						s.merge(i);
					}
					
				}
				s.flush();
				//HibernateManager.save(s,  attribute.getAttributeList());
				
				
				s.getTransaction().commit();
				this.attribute = s.get(AssetAttribute.class, this.attribute.getUuid());
				this.currentItem.clear();
				this.currentItem.addAll(this.attribute.getAttributeList());
				
			}catch (Exception ex){
				if (s.getTransaction().isActive())s.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AttributeDialog_SaveError + ex.getMessage(), ex);
				return;
			}
		}
		
		if (!modifiedAssets.isEmpty()) eventBroker.send(AssetEvents.ASSET_MODIFIED, modifiedAssets);
		if (!modifiedDeployments.isEmpty()) eventBroker.send(AssetEvents.ASSETDEPLOYMENT_MODIFIED, modifiedDeployments);
		if (!modifiedStations.isEmpty()) eventBroker.send(AssetEvents.ASSETSTATION_MODIFIED, modifiedStations);
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
		l.setText(Messages.AttributeDialog_TypeLabel);
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
					currentItem.clear();
					if (attribute.getAttributeList() != null)  currentItem.addAll(attribute.getAttributeList());
				}else{
					listPanel.setVisible(true);
					currentItem.clear();
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
		
		setTitle(Messages.AttributeDialog_Title);
		getShell().setText(Messages.AttributeDialog_Title);
		setMessage(Messages.AttributeDialog_Message);
		
		return parent;
	}
	
	private void initFields(){
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
		pmd.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				try(Session s = HibernateManager.openSession()){
					s.get(ConservationArea.class, attribute.getConservationArea().getUuid()).getLanguages();
					
					if (attribute.getUuid() != null){
						attribute = (AssetAttribute) s.get(AssetAttribute.class, attribute.getUuid());
						attribute.getNames().size();
						if (attribute.getAttributeList() != null){
							for (AssetAttributeListItem item : attribute.getAttributeList()){
								item.getNames().size();
							}
						}
					}
					attributeSiblings = AssetHibernateManager.getAttributes(s, SmartDB.getCurrentConservationArea());
					attributeSiblings.remove(attribute);
				}
				
//				allItems = new ArrayList<AssetAttributeListItem>();
//				if (attribute.getAttributeList() != null){
//					allItems.addAll(attribute.getAttributeList());
//				}
				

				
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
						
						currentItem = new ArrayList<AssetAttributeListItem>();
						if (attribute.getAttributeList() != null){
							currentItem.addAll(attribute.getAttributeList());
						}
						listPanel.setInput(currentItem);
					}
				});				
			}
		});
		}catch (Exception ex){
			AssetPlugIn.displayLog(MessageFormat.format(Messages.AttributeDialog_LoadError, ex.getMessage()), ex);
		}
		
		
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
