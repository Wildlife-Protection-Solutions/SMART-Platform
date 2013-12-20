package org.wcs.smart.entity.ui.typelist.editor;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Creates a panel for displaying all
 * entity properties.
 * 
 * @author Emily
 *
 */
public class EntityInfoPanelComposite extends Composite{

	private Composite main;
	private ScrolledComposite scroll;
	
	private Entity entity;
	private EntityType etype;
	
	private Text txtId;
	private Text txtX;
	private Text txtY;
	private Text txtStatus;
	
	private HashMap<EntityAttribute, Text> attributeToUi = null;
	
	
	public EntityInfoPanelComposite(Composite parent){
		super(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);
		
		scroll = new ScrolledComposite(this, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main = new Composite(scroll, SWT.NONE);
		scroll.setContent(main);
		
		gl = new GridLayout();
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);
		main.setLayout(gl);		
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	/**
	 * Updates the entity type; rebuilding
	 * the info composite as required.
	 * 
	 * @param et
	 */
	public void setEntityType(EntityType et){
		this.etype = et;
		
		//dispose any existing children
		for (Control c : main.getChildren()){
			c.dispose();
		}
		createComposite(main);
		scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scroll.layout(true,true);
		
	}
	

	/**
	 * Opens a session and updates
	 * the entity fields.
	 * 
	 * @param e
	 */
	public void setEntity(Entity e){
		this.entity = e;
		if (e == null){
			clearFields();
			return;
		}
		Session s = HibernateManager.openSession();
		try{
			this.entity = (Entity) s.load(Entity.class, entity.getUuid());
//			s.saveOrUpdate(entity);
			initEntityFields();
		}finally{
			s.close();
		}
	}
	
	/**
	 * Updates the entity fields without
	 * opening a session.
	 */
	public void initEntityFields(){
		if (entity == null){
			clearFields();
			return;
		}
		if (attributeToUi != null){
			txtId.setText(entity.getId());
			if (txtX != null){
				if ( entity.getX() != null){
					txtX.setText(String.valueOf(entity.getX()));
				}else{
					txtX.setText("");
				}
			}
			if (txtY != null){
				if ( entity.getY() != null){
					txtY.setText(String.valueOf(entity.getY()));
				}else{
					txtY.setText("");
				}
			}
		}
		
		txtStatus.setText(entity.getStatus().getGuiName());
		for (Text t : attributeToUi.values()){
			t.setText("");
		}
		
		for(EntityAttributeValue v : entity.getAttributes()){
			Text txt = attributeToUi.get(v.getEntityAttribute());
			if (txt != null){
				txt.setText(v.getValueAsString());
			}
		}
	}
	
	/**
	 * Updates the entity fields without
	 * opening a session.
	 */
	private void clearFields(){
		txtId.setText("");
		if (txtX != null){
			txtX.setText("");
		}
		if (txtY != null){
			txtY.setText("");
		}
		
		txtStatus.setText("");
		for (Text t : attributeToUi.values()){
			t.setText("");
		}
	}
	
	private void createComposite(Composite parent){
		attributeToUi = new HashMap<EntityAttribute, Text>();
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Composite comp = toolkit.createComposite(parent);
		comp.setLayout(new GridLayout(4, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		toolkit.createLabel(comp, Entity.ID_FIELD_NAME + ":");
		txtId = toolkit.createText(comp, "");
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtId.getLayoutData()).widthHint = 100;
		txtId.setEditable(false);
		
		toolkit.createLabel(comp, Entity.STATUS_FIELD_NAME + ":");
		txtStatus = toolkit.createText(comp, "");
		txtStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtStatus.getLayoutData()).widthHint = 100;
		txtStatus.setEditable(false);
		
		if (etype.getType() == EntityType.Type.FIXED){
			toolkit.createLabel(comp, Entity.X_FIELD_NAME + ":");
			txtX = toolkit.createText(comp, "");
			txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtX.getLayoutData()).widthHint = 100;
			txtX.setEditable(false);
			
			toolkit.createLabel(comp, Entity.Y_FIELD_NAME + ":");
			txtY = toolkit.createText(comp, "");
			txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtY.getLayoutData()).widthHint = 100;
			txtY.setEditable(false);
		}
		
		if (etype.getAttributes() != null){
			for (EntityAttribute ea : etype.getAttributes()){
				Label l = toolkit.createLabel(comp, ea.getName());
				Text txt = toolkit.createText(comp, "");
				txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)txt.getLayoutData()).widthHint = 100;
				txt.setEditable(false);
			
				attributeToUi.put(ea, txt);
			}
		}
		
		//update the entity info
		if (entity != null){
			initEntityFields();
		}
	}
}
