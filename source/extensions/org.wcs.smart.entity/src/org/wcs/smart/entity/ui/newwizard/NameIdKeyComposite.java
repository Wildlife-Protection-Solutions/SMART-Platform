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

import java.text.MessageFormat;
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
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * Composite for modifying the name, id and key 
 * properties of an entity type.
 * 
 * @author Emily
 *
 */
public class NameIdKeyComposite extends AbstractEntityComposite{

	private Text txtName;
	private Text txtKey;
	
	private List<NamedKeyItem> sharedKeys;
	
	@Override
	public String getName() {
		return Messages.NameIdKeyComposite_CompositeName;
	}

	@Override
	public String getDescription() {
		return Messages.NameIdKeyComposite_CompositeDescription;
	}

	@Override
	public String validate() {
		if (txtName.getText().trim().length() == 0){
			return Messages.NameIdKeyComposite_NameRequiredMessage;
		}
		if (!SmartUtils.isSimpleString(txtName.getText().trim(), RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Entity.NAME_MAX_LENGTH, 0)){
			return MessageFormat.format(Messages.NameIdKeyComposite_NameError, new Object[]{RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc});
		}
		if (txtKey.getText().trim().length() == 0){
			return Messages.NameIdKeyComposite_KeyRequired;
		}
		for (NamedKeyItem i : sharedKeys){
			if (i.getKeyId().equals(txtKey.getText().trim())){
				return Messages.NameIdKeyComposite_DuplicateKey;
			}
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		
		// NAME
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.NameIdKeyComposite_NameLabel);
		l.setToolTipText(Messages.NameIdKeyComposite_NameTooltip);
		txtName = new Text(main, SWT.BORDER);
		txtName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtKey.getData() == null){
					txtKey.setText(DataModelManager.INSTANCE.generateKey(txtName.getText(), sharedKeys));
				}
				fireChange(new Event());
			}
		});
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		// KEY
		l = new Label(main, SWT.NONE);
		l.setText(Messages.NameIdKeyComposite_KeyLabel);
		l.setToolTipText(Messages.NameIdKeyComposite_KeyTooltip);
		txtKey = new Text(main, SWT.BORDER);
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		Button btnEditKey = new Button(main, SWT.PUSH);
		btnEditKey.setText("..."); //$NON-NLS-1$
		btnEditKey.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog
						.openConfirm(
								txtKey.getShell(),
								Messages.NameIdKeyComposite_KeyDialogTitle,
								Messages.NameIdKeyComposite_KeyDialogMessage)){
					return;
				}
				KeyInputDialog id = new KeyInputDialog(txtKey.getShell(), txtKey.getText(), sharedKeys);
				int ret = id.openNoWarning();
				if (ret != Window.CANCEL) {
					txtKey.setText(id.getValue());
					fireChange(new Event());
				}
				
			}
		});
		
		return main;
	}
	
	@Override
	public void updateEntityType(EntityType entityType) {
		entityType.setName(txtName.getText().trim());
		entityType.setKeyId(txtKey.getText().trim());
		
		entityType.updateName(SmartDB.getCurrentLanguage(), txtName.getText());
		entityType.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), txtName.getText());
	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		if (entityType.getKeyId() != null){
			txtKey.setText(entityType.getKeyId());
			txtKey.setData(entityType.getKeyId());
		}
		if (entityType.getName() != null){
			txtName.setText(entityType.getName());
		}
		
		//load other entity keys
		Query q = session.createQuery("SELECT keyId from EntityType WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<String> keys = q.list();
		sharedKeys = new ArrayList<NamedKeyItem>();
		
		for (String k : keys){
			NamedKeyItem keyItem = new EntityType();
			keyItem.setKeyId(k);
			sharedKeys.add(keyItem);
		}
		
	}

}