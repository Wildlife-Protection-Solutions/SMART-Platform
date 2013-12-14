package org.wcs.smart.entity.ui.newwizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.entity.model.EntityType;

public class AttributeNameField extends AbstractEntityComposite{

	private Text txtAttributeName;
	
	@Override
	public String getName() {
		return "Entity Attribute Name";
	}

	@Override
	public String getDescription() {
		return "An entity with the attribute name will be create in the data model to reference this entity type.";
	}

	@Override
	public String validate() {
		if (txtAttributeName.getText().length() == 0){
			return "An attribute name must be provided";
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Attribute Name:");
		
		txtAttributeName = new Text(parent, SWT.BORDER);
		txtAttributeName.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				fireChange(new Event());
			}
		});
		
		return main;
	}

	@Override
	public void updateEntityType(EntityType entityType) {
//		entityType.setId(txtAttributeName.getText());
		//TODO: create a new data model attribute
		//and link it here
	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		if (entityType.getDmAttribute() == null){
			txtAttributeName.setText(entityType.getName() + " ID:");
		}else{
			txtAttributeName.setText(entityType.getDmAttribute().getName());
		}
				
	}

}