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
package org.wcs.smart.entity.ui.typelist.editor;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.Entity.Status;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.ui.ca.datamodel.AttributeFieldFactory;
import org.wcs.smart.ui.ca.datamodel.IAttributeField;
import org.wcs.smart.util.SmartUtils;

/**
 * Creates a panel for entering/editing
 * entity properties.
 * 
 * @author Emily
 *
 */

public class EntityEditPanelComposite extends Composite{
	
	private Entity entity;
	private EntityType etype;
	
	private Text txtId;
	private Text txtX;
	private Text txtY;
	private ComboViewer cmbStatus;
	
	private ControlDecoration cdId;
	private ControlDecoration cdX;
	private ControlDecoration cdY;
	
	private Composite main;
	
	private HashMap<EntityAttribute, IAttributeField<?>> attributeToEdit = null;
	
	/**
	 * Creates a new panel
	 * @param parent
	 */
	public EntityEditPanelComposite(Composite parent){
		super(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);
		
		main = new Composite(this, SWT.NONE);
		gl = new GridLayout();
		setLayout(gl);
		main.setLayout(gl);		
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	/**
	 * Updates the entity type of the panel.  This defines
	 * which fields are displayed in this composite
	 * 
	 * This function disposes of any existing fields then
	 * recreates them.
	 * 
	 * @param et the new entity type.
	 */
	
	public void setEntityType(EntityType et){
		this.etype = et;
		
		if (attributeToEdit != null){
			for (IAttributeField<?> att : attributeToEdit.values()){
				att.dispose();
			}
		}
		for (Control c : main.getChildren()){
			c.dispose();
		}
		
		createEditComposite(main);
	}
	
	@Override
	public void dispose(){
		for (IAttributeField<?> att : attributeToEdit.values()){
			att.dispose();
		}
	}
	
	/**
	 * Sets the entity to add/edit
	 * 
	 * @param e
	 */
	public void setEditEntity(Entity e){
		this.entity = e;
		if (attributeToEdit != null){
			if (e.getId() != null){
				txtId.setText(e.getId());
			}else{
				txtId.setText(""); //$NON-NLS-1$
			}
			
			if (e.getEntityType().getType() == EntityType.Type.FIXED){
				if (e.getX() != null){
					txtX.setText(String.valueOf(e.getX()));
				}else{
					txtX.setText(""); //$NON-NLS-1$
				}
				if (e.getY() != null){
					txtY.setText(String.valueOf(e.getY()));
				}else{
					txtY.setText(""); //$NON-NLS-1$
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
	
	/**
	 * Determines if all fields contain valid data.
	 * 
	 * @return null if all data is valid, an error
	 * string if a field is in error
	 */
	public String isValid(){
		if (cdId.getDescriptionText() != null){
			return cdId.getDescriptionText();
		}
		if (cdX != null && cdX.getDescriptionText() != null){
			return cdX.getDescriptionText();
		}
		if (cdY != null && cdY.getDescriptionText() != null){
			return cdY.getDescriptionText();
		}
		for (IAttributeField<?> et : attributeToEdit.values()){
			String x = et.validate();
			if (x != null){
				return x;
			}
		}
		return null;
	}
	
	/**
	 * Updates the entity with the values
	 * from the fields.
	 */
	public void updateEntity(){
		entity.setId(txtId.getText());
		if (entity.getEntityType().getType() == EntityType.Type.FIXED){
			entity.setX(Double.parseDouble(txtX.getText()));
			entity.setY(Double.parseDouble(txtY.getText()));
		}		
		entity.setStatus((Status) ((IStructuredSelection)cmbStatus.getSelection()).getFirstElement());
		
		for (Entry<EntityAttribute, IAttributeField<?>> entry : attributeToEdit.entrySet()){
			EntityAttributeValue toUpdate = entity.findAttribute(entry.getKey());
			
			if (entry.getValue().getValue() == null){
				//value removed; remove attribute value
				if (toUpdate != null){
					entity.getAttributes().remove(toUpdate);
					toUpdate.setId(null);
				}
			}else{
				//update value of create new value
				if (toUpdate == null){
					toUpdate = new EntityAttributeValue();
					toUpdate.setEntity(entity);
					toUpdate.setEntityAttribute(entry.getKey());
					entity.getAttributes().add(toUpdate);
				}
				toUpdate.setValue(entry.getValue().getValue());
			}
		}
	}
	
	
	private void createEditComposite(Composite parent){
		attributeToEdit = new HashMap<EntityAttribute, IAttributeField<?>>();
	
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		//ID
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText(Entity.ID_FIELD_NAME + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		txtId = new Text(comp, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtId.getLayoutData()).widthHint = 100;
		((GridData)txtId.getLayoutData()).horizontalIndent = 5;
		
		cdId = new ControlDecoration(txtId, SWT.LEFT | SWT.TOP);
		cdId.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdId.hide();
		
		txtId.addListener(SWT.Modify, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (!SmartUtils.isSimpleString(txtId.getText(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Entity.ID_MAX_LENGTH, 1)){
					cdId.setDescriptionText(MessageFormat.format(Messages.EntityEditPanelComposite_IdError, new Object[]{Entity.ID_MAX_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX}));
					cdId.show();
				}else{
					cdId.setDescriptionText(null);
					cdId.hide();
				}
			}});
		
		//STATUS
		lbl = new Label(comp, SWT.NONE);
		lbl.setText(Entity.STATUS_FIELD_NAME + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		cmbStatus = new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbStatus.setContentProvider(ArrayContentProvider.getInstance());
		cmbStatus.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbStatus.getControl().getLayoutData()).horizontalIndent = 5;
		cmbStatus.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				return ((Entity.Status)element).getGuiName();
			}
		});
		cmbStatus.setInput(Entity.Status.values());
			
		if (etype.getType() == EntityType.Type.FIXED){
			
			lbl = new Label(comp, SWT.NONE);
			lbl.setText(Entity.X_FIELD_NAME + ":"); //$NON-NLS-1$
			
			txtX = new Text(comp, SWT.BORDER);
			txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtX.getLayoutData()).horizontalIndent = 5;
			((GridData)txtX.getLayoutData()).widthHint = 100;
			cdX = new ControlDecoration(txtX, SWT.LEFT | SWT.TOP);
			cdX.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			cdX.hide();
			txtX.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent e) {
					if (txtX.getText().trim().length() == 0){
						cdX.setDescriptionText(Messages.EntityEditPanelComposite_xError);
						cdX.show();
						return;
					}
					try{
						Double.parseDouble(txtX.getText());
						cdX.setDescriptionText(null);
						cdX.hide();
					}catch (Exception ex){
						cdX.setDescriptionText(Messages.EntityEditPanelComposite_xError);
						cdX.show();
					}
				}
			});
			
			lbl = new Label(comp, SWT.NONE);
			lbl.setText(Entity.Y_FIELD_NAME + ":"); //$NON-NLS-1$
			
			txtY = new Text(comp, SWT.BORDER);			
			txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtY.getLayoutData()).horizontalIndent = 5;
			((GridData)txtY.getLayoutData()).widthHint = 100;
			cdY = new ControlDecoration(txtY, SWT.LEFT | SWT.TOP);
			cdY.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			cdY.hide();
			txtY.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent e) {
					if (txtY.getText().trim().length() == 0){
						cdY.setDescriptionText(Messages.EntityEditPanelComposite_yError);
						cdY.show();
						return;
					}
					try{
						Double.parseDouble(txtY.getText());
						cdY.setDescriptionText(null);
						cdY.hide();
					}catch (Exception ex){
						cdY.setDescriptionText(Messages.EntityEditPanelComposite_yError);
						cdY.show();
					}
				}
			});
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
	}

}
