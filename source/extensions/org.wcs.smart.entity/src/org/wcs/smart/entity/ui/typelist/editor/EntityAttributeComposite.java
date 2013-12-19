package org.wcs.smart.entity.ui.typelist.editor;

import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.Entity.Status;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.ui.ca.datamodel.AttributeFieldFactory;
import org.wcs.smart.ui.ca.datamodel.IAttributeField;

public class EntityAttributeComposite {

	private Entity entity;
	private EntityType etype;
	
	private Text txtID;
	private Text txtX;
	private Text txtY;
	private Text txtStatus;
	
	private HashMap<EntityAttribute, Text> attributeToUi = null;
	private HashMap<EntityAttribute, IAttributeField<?>> attributeToEdit = null;
	
	
	public EntityAttributeComposite(){
		
	}
	
	public void setEntityType(EntityType et){
		this.etype = et;
	}
	
	public void dispose(){
		if (attributeToEdit != null){
			for (Entry<EntityAttribute, IAttributeField<?>> entry : attributeToEdit.entrySet()){
				entry.getValue().dispose();
			}
		}
	}
	public void setEntity(Entity e){
		this.entity = e;
		if (attributeToUi != null){
			
			txtID.setText(e.getId());
			txtX.setText(String.valueOf(e.getX()));
			txtY.setText(String.valueOf(e.getY()));
			txtStatus.setText(e.getStatus().getGuiName());
			
			for(EntityAttributeValue v : e.getAttributes()){
				Text txt = attributeToUi.get(v.getEntityAttribute());
				if (txt != null){
					txt.setText(v.getValueAsString());
				}
			}
		}
	}
	
	
	public Composite createComposite(Composite parent){
		attributeToUi = new HashMap<EntityAttribute, Text>();
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Composite comp = toolkit.createComposite(parent);
		comp.setLayout(new GridLayout(4, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(comp, Entity.ID_FIELD_NAME + ":");
		txtID = toolkit.createText(comp, "");
		txtID.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtID.getLayoutData()).widthHint = 100;
		txtID.setEditable(false);
		
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
		
		
		for (EntityAttribute ea : etype.getAttributes()){
			
			Label l = toolkit.createLabel(comp, ea.getName());
			Text txt = toolkit.createText(comp, "");
			txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txt.getLayoutData()).widthHint = 100;
			txt.setEditable(false);
			
			attributeToUi.put(ea, txt);
		}
		
		if (entity != null){
			setEntity(entity);
		}
		return comp;
	}
	
	private ComboViewer cmbStatus;
	
	
	public void setEditEntity(Entity e){
		this.entity = e;
		if (attributeToEdit != null){
			
			if (e.getId() != null){
				txtID.setText(e.getId());
			}
			if (e.getEntityType().getType() == EntityType.Type.FIXED){
				if (e.getX() != null){
					txtX.setText(String.valueOf(e.getX()));
				}
				if (e.getY() != null){
					txtY.setText(String.valueOf(e.getY()));
				}
			}
			cmbStatus.setSelection(new StructuredSelection(e.getStatus()));
			for(EntityAttributeValue v : e.getAttributes()){
				IAttributeField<?> field = attributeToEdit.get(v.getEntityAttribute());
				if (field != null){
					field.setValue(v.getValue());
				}
			}
		}
	}
	
	public void updateEntity(){
		
		entity.setId(txtID.getText());
		if (entity.getEntityType().getType() == EntityType.Type.FIXED){
			entity.setX(Double.parseDouble(txtX.getText()));
			entity.setY(Double.parseDouble(txtY.getText()));
		}		
		entity.setStatus((Status) ((IStructuredSelection)cmbStatus.getSelection()).getFirstElement());
		
		for (Entry<EntityAttribute, IAttributeField<?>> entry : attributeToEdit.entrySet()){
			EntityAttributeValue toUpdate = entity.findAttribute(entry.getKey());
			if (toUpdate == null){
				toUpdate = new EntityAttributeValue();
				toUpdate.setEntity(entity);
				toUpdate.setEntityAttribute(entry.getKey());
				entity.getAttributes().add(toUpdate);
			}
			toUpdate.setValue(entry.getValue().getValue());
		}
	}
	
	
	public Composite createEditComposite(Composite parent){
		attributeToEdit = new HashMap<EntityAttribute, IAttributeField<?>>();
	
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		//ID
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText(Entity.ID_FIELD_NAME + ":");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		txtID = new Text(comp, SWT.BORDER);
		txtID.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtID.getLayoutData()).widthHint = 100;
		
		
		//STATUS
		lbl = new Label(comp, SWT.NONE);
		lbl.setText(Entity.STATUS_FIELD_NAME + ":");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		cmbStatus = new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbStatus.setContentProvider(ArrayContentProvider.getInstance());
		cmbStatus.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbStatus.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				return ((Entity.Status)element).getGuiName();
			}
		});
		cmbStatus.setInput(Entity.Status.values());
			
		if (etype.getType() == EntityType.Type.FIXED){
			
			lbl = new Label(comp, SWT.DEFAULT);
			lbl.setText(Entity.X_FIELD_NAME + ":");
			
			txtX = new Text(comp, SWT.NONE);
			txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtX.getLayoutData()).widthHint = 100;
			
			lbl = new Label(comp, SWT.DEFAULT);
			lbl.setText(Entity.Y_FIELD_NAME + ":");
			
			txtY = new Text(comp, SWT.NONE);			
			txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtY.getLayoutData()).widthHint = 100;
			txtY.setEditable(false);
		}
		
		for (EntityAttribute ea : etype.getAttributes()){
			
			boolean isRequired = etype.getDmAttribute().getIsRequired();
			String name = etype.getDmAttribute().getName();
			try{
				etype.getDmAttribute().setIsRequired(ea.getIsRequired());
				etype.getDmAttribute().setName(ea.getName());
				IAttributeField<?> field = AttributeFieldFactory.findAttributeField(ea.getDmAttribute());
				
				field.createComposite(comp);
				attributeToEdit.put(ea, field);
			}finally{
				etype.getDmAttribute().setIsRequired(isRequired);
				etype.getDmAttribute().setName(name);
			}
		}
		return comp;
	}

}
