package org.wcs.smart.entity.ui.newwizard;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

public class IdComposite extends AbstractEntityComposite{

	private Text txtId;
	
	@Override
	public String getName() {
		return "Entity Type ID";
	}

	@Override
	public String getDescription() {
		return "Modify the entity type indentifier.";
	}

	@Override
	public String validate() {
		if (txtId.getText().length() == 0){
			return "An id must be provided";
		}
		if (!SmartUtils.isSimpleString(txtId.getText().trim(), RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX, Entity.KEY_MAX_LENGTH, 1)){
			return MessageFormat.format("The id must be between {0} and {1} characters long and can only contain {2}.",
					new Object[]{1, Entity.KEY_MAX_LENGTH, RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX.textDesc});
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)main.getLayout()).marginWidth = 0;
		//ID
		Label l = new Label(main, SWT.NONE);
		l.setText("Entity Type ID:");
		l.setToolTipText("A unique identifier for the entity type.  Required.");
		
		txtId = new Text(main, SWT.BORDER);
		txtId.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fireChange(new Event());
			}
		});
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		return main;
	}
	
	@Override
	public void updateEntityType(EntityType entityType) {
		entityType.setId(txtId.getText().trim());
	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		if (entityType.getId() != null){
			txtId.setText(entityType.getId());
		}
	}

}