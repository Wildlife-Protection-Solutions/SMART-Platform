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
package org.wcs.smart.entity.ui.editor;

import java.util.HashMap;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.EntityLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.util.ReprojectUtils;

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

	private ObservationOptions observationOptions;
	private CoordinateReferenceSystem crs;
	private Entity entity;
	private EntityType etype;
	
	private Text txtId;
	private Text txtX;
	private Text txtY;
	private Text txtStatus;
	
	private HashMap<String, Text> attributeToUi = null;
	
	/**
	 * Creates a new panel.
	 * 
	 * @param parent
	 */
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
	 * Updates the entity type of the panel.  This disposes of
	 * all existing composites then rebuilds them
	 * as required for the new entity type.
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
			this.observationOptions = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), s);
			this.entity = (Entity) s.load(Entity.class, entity.getUuid());
			try{
				this.crs = ReprojectUtils.stringToCrs(observationOptions.getViewProjection().getDefinition());
			}catch (Exception ex){
				EntityPlugIn.displayLog(ex.getMessage(), ex);
			}
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
		if (attributeToUi == null){
			//widgets not yet created
			return;
		}
		if (entity == null){
			clearFields();
			return;
		}
		
		txtId.setText(entity.getId());
		if (txtX != null){
			if ( entity.getX() != null){
				txtX.setText(String.valueOf(ReprojectUtils.transform(entity.getX(), entity.getY(), crs).getX()));
			}else{
				txtX.setText(""); //$NON-NLS-1$
			}
		}
		if (txtY != null){
			if ( entity.getY() != null){
				txtY.setText(String.valueOf(ReprojectUtils.transform(entity.getX(), entity.getY(), crs).getY()));
			}else{
				txtY.setText(""); //$NON-NLS-1$
			}
		}
		
		txtStatus.setText(entity.getStatus().getGuiName(Locale.getDefault()));
		for (Text t : attributeToUi.values()){
			t.setText(""); //$NON-NLS-1$
		}
		
		for(EntityAttributeValue v : entity.getAttributes()){
			Text txt = attributeToUi.get(v.getEntityAttribute().getKeyId());
			if (txt != null){
				txt.setText(v.getValueAsString(Locale.getDefault()));
			}
		}
	}
	
	/**
	 * Updates the entity fields without
	 * opening a session.
	 */
	private void clearFields(){
		if (txtId == null){
			return;
		}
		txtId.setText(""); //$NON-NLS-1$
		if (txtX != null){
			txtX.setText(""); //$NON-NLS-1$
		}
		if (txtY != null){
			txtY.setText(""); //$NON-NLS-1$
		}
		
		txtStatus.setText(""); //$NON-NLS-1$
		for (Text t : attributeToUi.values()){
			t.setText(""); //$NON-NLS-1$
		}
	}
	
	private void createComposite(Composite parent){
		attributeToUi = new HashMap<String, Text>();
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		toolkit.setBorderStyle(SWT.BORDER);
		
		Composite comp = toolkit.createComposite(parent);
		comp.setLayout(new GridLayout(4, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		toolkit.createLabel(comp, EntityLabelProvider.ID_FIELD_NAME + ":"); //$NON-NLS-1$
		txtId = toolkit.createText(comp, ""); //$NON-NLS-1$
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtId.getLayoutData()).widthHint = 100;
		txtId.setEditable(false);
		
		toolkit.createLabel(comp, EntityLabelProvider.STATUS_FIELD_NAME + ":"); //$NON-NLS-1$
		txtStatus = toolkit.createText(comp, ""); //$NON-NLS-1$
		txtStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtStatus.getLayoutData()).widthHint = 100;
		txtStatus.setEditable(false);
		
		if (etype.getType() == EntityType.Type.FIXED){
			toolkit.createLabel(comp, EntityLabelProvider.X_FIELD_NAME + ":"); //$NON-NLS-1$
			txtX = toolkit.createText(comp, ""); //$NON-NLS-1$
			txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtX.getLayoutData()).widthHint = 100;
			txtX.setEditable(false);
			
			toolkit.createLabel(comp, EntityLabelProvider.Y_FIELD_NAME + ":"); //$NON-NLS-1$
			txtY = toolkit.createText(comp, ""); //$NON-NLS-1$
			txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtY.getLayoutData()).widthHint = 100;
			txtY.setEditable(false);
		}
		
		if (etype.getAttributes() != null){
			for (EntityAttribute ea : etype.getAttributes()){
				toolkit.createLabel(comp, ea.getName() + ":"); //$NON-NLS-1$
				
				Text txt = toolkit.createText(comp, ""); //$NON-NLS-1$
				txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)txt.getLayoutData()).widthHint = 100;
				txt.setEditable(false);
			
				attributeToUi.put(ea.getKeyId(), txt);
			}
		}
		
		toolkit.dispose();
		//update the entity info
		if (entity != null){
			initEntityFields();
		}
	}
}
