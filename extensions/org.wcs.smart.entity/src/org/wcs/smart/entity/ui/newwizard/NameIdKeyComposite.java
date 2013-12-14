package org.wcs.smart.entity.ui.newwizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.SmartDB;

public class NameIdKeyComposite extends AbstractEntityComposite{

	private Text txtName;
	private Text txtId;
	private Text txtKey;
	
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
		if (txtKey.getText().length() == 0){
			return "A key must be provided";
		}
		if (txtId.getText().length() == 0){
			return "An id must be provided";
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("ID:");
		
		txtId = new Text(parent, SWT.BORDER);
		txtId.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				fireChange(new Event());
			}
		});
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		l = new Label(main, SWT.NONE);
		l.setText("Name:");
		
		txtName = new Text(parent, SWT.BORDER);
		txtName.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				fireChange(new Event());
			}
		});
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		l = new Label(main, SWT.NONE);
		l.setText("Key:");
		txtKey = new Text(parent, SWT.BORDER);
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		Button btnEditKey = new Button(main, SWT.PUSH);
		btnEditKey.setText("...");
		btnEditKey.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO: edit key
			}
		});
		
		return main;
	}

	@Override
	public void updateEntityType(EntityType entityType) {
		entityType.setId(txtId.getText());
		entityType.setName(txtName.getText());
		entityType.setKeyId(txtKey.getText());
		
		entityType.updateName(SmartDB.getCurrentLanguage(), txtName.getText());
		entityType.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), txtName.getText());
	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		if (entityType.getId() != null){
			txtId.setText(entityType.getId());
		}
		if (entityType.getKeyId() != null){
			txtKey.setText(entityType.getKeyId());
		}
		if (entityType.getName() != null){
			txtName.setText(entityType.getName());
		}
		
	}

}