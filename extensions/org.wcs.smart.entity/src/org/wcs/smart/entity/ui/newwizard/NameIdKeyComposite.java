package org.wcs.smart.entity.ui.newwizard;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
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
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

public class NameIdKeyComposite extends AbstractEntityComposite{

	private Text txtName;
	private IdComposite txtId;
	private Text txtKey;
	
	private List<NamedKeyItem> sharedKeys;
	
	@Override
	public String getName() {
		return "Entity Type Name, ID, Key";
	}

	@Override
	public String getDescription() {
		return "The entity type name, key and id value.";
	}

	@Override
	public String validate() {
		String error = txtId.validate();
		if (error != null){
			return error;
		}
		
		if (!SmartUtils.isSimpleString(txtName.getText().trim(), RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Entity.NAME_MAX_LENGTH, 0)){
			return MessageFormat.format("The name must only contain the characters {0}.", new String[]{RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc});
		}
		if (txtKey.getText().trim().length() == 0){
			return "A key must be provided";
		}
		for (NamedKeyItem i : sharedKeys){
			if (i.getKeyId().equals(txtKey.getText().trim())){
				return "The key is already used.  Please provide a unique key";
			}
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		
		txtId = new IdComposite();
		Composite idComp = txtId.createComposite(main);
		idComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		// NAME
		Label l = new Label(main, SWT.NONE);
		l.setText("Entity Type Name:");
		l.setToolTipText("A name for the entity type.  Optional.");
		txtName = new Text(main, SWT.BORDER);
		txtName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtKey.getData() == null){
					txtKey.setText(NamedKeyItem.generateKey(txtName.getText(), sharedKeys));
				}
				fireChange(new Event());
			}
		});
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		// KEY
		l = new Label(main, SWT.NONE);
		l.setText("Key:");
		l.setToolTipText("A unique key for the entity type.  This is used for cross conservation area analysis. Required.");
		txtKey = new Text(main, SWT.BORDER);
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		Button btnEditKey = new Button(main, SWT.PUSH);
		btnEditKey.setText("...");
		btnEditKey.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog
						.openConfirm(
								txtKey.getShell(),
								"Set Key",
								"Keys effect reporting across multiple conservation areas, and should not be changed unless you understand the implications.  Are you sure you want to continue?")){
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
		txtId.updateEntityType(entityType);
		entityType.setName(txtName.getText().trim());
		entityType.setKeyId(txtKey.getText().trim());
		
		entityType.updateName(SmartDB.getCurrentLanguage(), txtName.getText());
		entityType.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), txtName.getText());
	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		txtId.initFields(entityType, session);
		
		if (entityType.getKeyId() != null){
			txtKey.setText(entityType.getKeyId());
			txtKey.setData(entityType.getKeyId());
		}
		if (entityType.getName() != null){
			txtName.setText(entityType.getName());
		}
		
		//load other entity keys
		Query q = session.createQuery("SELECT keyId from EntityType WHERE conservationArea = :ca");
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		List<String> keys = q.list();
		sharedKeys = new ArrayList<NamedKeyItem>();
		
		for (String k : keys){
			NamedKeyItem keyItem = new EntityType();
			keyItem.setKeyId(k);
			sharedKeys.add(keyItem);
		}
		
	}

}