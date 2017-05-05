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
package org.wcs.smart.entity.ui.newwizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.KeyInputDialog;

/**
 * Composite for entering attribute name for attribute
 * associated with entity type. 
 *  
 * @author Emily
 *
 */
public class AttributeNameField extends AbstractEntityComposite{

	private Text txtAttributeName;
	private Text txtAttributeId;
	
	private List<NamedKeyItem> sharedKeys;
	
	@Override
	public String getName() {
		return Messages.AttributeNameField_CompositeName;
	}

	@Override
	public String getDescription() {
		return Messages.AttributeNameField_CompositeDescription;
	}

	@Override
	public String validate() {
		if (txtAttributeName.getText().trim().length() == 0){
			return Messages.AttributeNameField_AttributeNameRequired;
		}
		return DataModel.validateName(txtAttributeName.getText().trim(), SmartDB.getCurrentLanguage());
		
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.AttributeNameField_NameLabel);
		
		txtAttributeName = new Text(main, SWT.BORDER);
		txtAttributeName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fireChange(new Event());
			}
		});
		txtAttributeName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,2,1));
		txtAttributeName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtAttributeId.getData() == null){
					txtAttributeId.setText(DataModelManager.INSTANCE.generateKey(txtAttributeName.getText(), sharedKeys));
				}
				fireChange(new Event());
			}
		});
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.AttributeNameField_NameKey);
		txtAttributeId = new Text(main, SWT.BORDER);
		txtAttributeId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtAttributeId.setEditable(false);
		
		Button btnEditKey = new Button(main, SWT.PUSH);
		btnEditKey.setText("..."); //$NON-NLS-1$
		btnEditKey.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog
						.openConfirm(
								txtAttributeId.getShell(),
								Messages.AttributeNameField_KeyDialogTitle,
								Messages.AttributeNameField_KeyDialogMessage)){
					return;
				}
				KeyInputDialog id = new KeyInputDialog(txtAttributeId.getShell(), txtAttributeId.getText(), sharedKeys);
				int ret = id.openNoWarning();
				if (ret != Window.CANCEL) {
					txtAttributeId.setText(id.getValue());
					fireChange(new Event());
				}
				
			}
		});
		
		return main;
	}

	@Override
	public void updateEntityType(EntityType entityType) {
		String name = txtAttributeName.getText().trim();
		
		if (txtAttributeName.getData() == null){
			Attribute attribute = new Attribute();
			attribute.setConservationArea(SmartDB.getCurrentConservationArea());
			attribute.setName(name);
			attribute.updateName(SmartDB.getCurrentLanguage(), name);
			attribute.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
			attribute.setIsRequired(false);
			attribute.setType(AttributeType.LIST);
			attribute.setKeyId(txtAttributeId.getText().trim());
		
			entityType.setDmAttribute(attribute);
		}else{
			Attribute attribute=  (Attribute) txtAttributeName.getData();
			attribute.setName(name);
			attribute.updateName(SmartDB.getCurrentLanguage(), name);
			attribute.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		}
	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		//load other entity keys
		Query q = session.createQuery("SELECT keyId from Attribute WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<String> keys = q.list();
		sharedKeys = new ArrayList<NamedKeyItem>();
				
		for (String k : keys){
			NamedKeyItem keyItem = new EntityType();
			keyItem.setKeyId(k);
			sharedKeys.add(keyItem);
		}
				
		if (entityType.getDmAttribute() == null){
			txtAttributeName.setText(entityType.getName() + Messages.AttributeNameField_IdLabel);
			txtAttributeId.setText(DataModelManager.INSTANCE.generateKey(txtAttributeName.getText(), sharedKeys));
			txtAttributeId.setData(null);
			txtAttributeName.setData(null);
		}else{
			txtAttributeName.setText(entityType.getDmAttribute().getName());
			txtAttributeName.setData(entityType.getDmAttribute());
			txtAttributeId.setText(entityType.getDmAttribute().getKeyId());
			txtAttributeId.setData(entityType.getDmAttribute().getKeyId());
		}
				
	}

}