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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.Entity.Status;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.ui.ProjectionLabelProvider;
import org.wcs.smart.ui.ca.datamodel.AttributeFieldFactory;
import org.wcs.smart.ui.ca.datamodel.IAttributeField;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

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

	private final GeometryFactory gf = new GeometryFactory();
	private Projection currentProjection;
	private Projection dbProjection;
	
	private Text txtId;
	private Text txtX;
	private Text txtY;
	private ComboViewer cmbProjection;
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
	
	public void setEntityType(EntityType et, Session session){
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
		if (cmbProjection != null){
			List<Projection> projs = HibernateManager.getCaProjectionList(session);
			dbProjection = currentProjection = getDatabaseProjection(projs);
			cmbProjection.setInput(projs);
			Projection selectedProjection = ObservationHibernateManager.getCurrentViewProjection(session);
			if (selectedProjection == null) {
				selectedProjection = projs.get(0);
			}
			cmbProjection.setSelection(new StructuredSelection(selectedProjection));
		}
	}

	private Projection getDatabaseProjection(List<Projection> projs) {
		Projection defaultp = null;
		for(Projection p : projs){
			try{
				if (CRS.equalsIgnoreMetadata(p.getCrs(), SmartDB.DATABASE_CRS)){
					defaultp = p;
					break;
				}
			}catch (Exception ex){
				EntityPlugIn.log("Error parsing projection info", ex); //$NON-NLS-1$
			}
		}
		if (defaultp == null){
			defaultp = new Projection();
			defaultp.setCrs(SmartDB.DATABASE_CRS);
			projs.add(defaultp);
		}
		return defaultp;
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
			transformInput(dbProjection, currentProjection);
			
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
		try{
			getPosition();
		}catch (Exception ex){
			EntityPlugIn.log(Messages.EntityEditPanelComposite_ProjectionError, ex);
			return ex.getMessage();
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
			try{
				Coordinate c = getPosition();
				entity.setX(c.x);
				entity.setY(c.y);
			}catch (Exception ex){
				
			}
			
		}		
		entity.setStatus((Status) ((IStructuredSelection)cmbStatus.getSelection()).getFirstElement());
		
		for (Entry<EntityAttribute, IAttributeField<?>> entry : attributeToEdit.entrySet()){
			EntityAttributeValue toUpdate = entity.findAttribute(entry.getKey().getKeyId());
			
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
		((GridData)comp.getLayoutData()).widthHint = 300;
		
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
			lbl.setText(Messages.EntityEditPanelComposite_LocationLabel);
			lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			((GridData)lbl.getLayoutData()).verticalIndent =8;
			
			//Position
			Composite postComp = new Composite(comp, SWT.NONE);
			postComp.setLayout(new GridLayout(2, false));
			postComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			lbl = new Label(postComp, SWT.NONE);
			lbl.setText(Messages.EntityEditPanelComposite_ProjectionLabel );
			
			cmbProjection = new ComboViewer(postComp, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbProjection.setLabelProvider(ProjectionLabelProvider.getInstance());
			cmbProjection.setContentProvider(ArrayContentProvider.getInstance());
			cmbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)cmbProjection.getControl().getLayoutData()).widthHint = 100;
			cmbProjection.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					transformInput();
					currentProjection = (Projection)((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
				}
			});
			
			Composite posComp = new Composite(postComp, SWT.NONE);
			GridLayout gl = new GridLayout(4, false);
			gl.marginWidth = gl.marginHeight = 0;
			posComp.setLayout(gl);
			posComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			lbl = new Label(posComp, SWT.NONE);
			lbl.setText(Entity.X_FIELD_NAME + ":"); //$NON-NLS-1$
			
			txtX = new Text(posComp, SWT.BORDER);
			txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtX.getLayoutData()).horizontalIndent = 5;
			((GridData)txtX.getLayoutData()).widthHint = 50;
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
			
			lbl = new Label(posComp, SWT.NONE);
			lbl.setText(Entity.Y_FIELD_NAME + ":"); //$NON-NLS-1$
			
			txtY = new Text(posComp, SWT.BORDER);			
			txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtY.getLayoutData()).horizontalIndent = 5;
			((GridData)txtY.getLayoutData()).widthHint = 50;
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
			
			Link lnk = new Link(postComp,  SWT.NONE);
			lnk.setText("<a>" + Messages.EntityEditPanelComposite_SelectOnMapLink + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			lnk.addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {	
					selectOnMap();
				}});
			lnk.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 2, 1));
		}
		
		for (EntityAttribute ea : etype.getAttributes()){
			EntityAttributeWrapper wrapper = new EntityAttributeWrapper(ea);
			IAttributeField<?> field = AttributeFieldFactory.findAttributeField(wrapper);
			field.createComposite(comp);
			attributeToEdit.put(ea, field);
		}
	}

	private void transformInput() {
		Projection target = (Projection)((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
		transformInput(currentProjection, target);
	}

	private void transformInput(final Projection source, final Projection target) {
		try {
			//reproject
			Point point = gf.createPoint(new Coordinate(Double.parseDouble(txtX.getText()),Double.parseDouble(txtY.getText())));
			Point p = (Point) JTS.transform(point, CRS.findMathTransform(source.getCrs(), target.getCrs()));

			txtX.setText(String.valueOf(p.getX()));
			txtY.setText(String.valueOf(p.getY()));
		} catch (Exception ex) {
			//nothing
		}
	}
	
	private Coordinate getPosition() throws Exception{
		if (txtX == null || txtY == null || cmbProjection == null){
			return null;
		}
		
		Double x = null;
		Double y = null;
		try{
			x = Double.parseDouble(txtX.getText());
			y = Double.parseDouble(txtY.getText());
		}catch (Exception ex){
			
		}
		if (x == null || y == null){
			return null;
		}
		
		//we need to get the x and y and projection and transform to latLong
		Projection proj = (Projection) ((StructuredSelection)cmbProjection.getSelection()).getFirstElement();
		if (CRS.equalsIgnoreMetadata(proj.getCrs(), SmartDB.DATABASE_CRS)){
			//this is in db crs so we don't need to do anything
			return new Coordinate(x,y);
		}else{
			//need to reproject 
			return ReprojectUtils.reproject(x, y, proj.getCrs(), SmartDB.DATABASE_CRS);
		}
		
	}
	private void selectOnMap(){
		MapDialog md = new MapDialog(Display.getCurrent().getActiveShell());		
		if (entity.getUuid() != null){
			try{
				Coordinate c = getPosition();
				if (c != null){
					md.setInitPoint(c.x, c.y);
				}
			}catch (Exception ex){}
			
		}
		
		if (md.open() == MapDialog.OK){
			if (md.getPoint() != null){
				txtX.setText(String.valueOf(md.getPoint().getX()));
				txtY.setText(String.valueOf(md.getPoint().getY()));
				transformInput(dbProjection, currentProjection);
			}
		}
	}
	
	private class EntityAttributeWrapper extends Attribute{
		
		private EntityAttribute wrapped;
		
		/**
		 * Creates a new attribute
		 */
		public EntityAttributeWrapper(EntityAttribute ea){
			this.wrapped = ea;
		}
		
		/**
		 * Key that represents the team.  Teams that
		 * are to be considered the "same" in cross-ca analysis should
		 * have the same key.
		 *    
		 * @return
		 */
		public String getKeyId(){
			return wrapped.getDmAttribute().getKeyId();
		}
		
		/**
		 * Unique key
		 * @param keyId
		 */
		public void setKeyId(String keyId){
			wrapped.getDmAttribute().setKeyId(keyId);
		}

		public String getName(){
			return wrapped.getName();
		}
		/**
		 * 
		 * @return <code>true</code> if attribute must be populated, <code>false</code> otherwise
		 */
		public boolean getIsRequired(){
			return wrapped.getIsRequired();
		}
		/**
		 * 
		 * @param isRequired if attribute is required
		 */
		public void setIsRequired(boolean isRequired){
			wrapped.setIsRequired(isRequired);
		}
		
		/**
		 * 
		 * @return the attribute type
		 */
		public AttributeType getType() {
			return wrapped.getDmAttribute().getType();
		}
		/**
		 * 
		 * @param type the attribute type
		 */
		public void setType(AttributeType type) {
			wrapped.getDmAttribute().setType(type);
		}
		
		/**
		 * 
		 * @return the conservation area associated with the attribute
		 */
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
		public ConservationArea getConservationArea() {
			return wrapped.getDmAttribute().getConservationArea();
		}
		/**
		 * 
		 * @param ca the conservation area to be associated with the attribute
		 */
		public void setConservationArea(ConservationArea ca) {
			wrapped.getDmAttribute().setConservationArea(ca);
		}
		
		/**
		 * Only valid for numeric attributes.
		 * @return the minimum value of the attribute
		 */
		@Column(name="min_value")
		public Double getMinValue() {
			return wrapped.getDmAttribute().getMinValue();
		}


		/**
		 * Only valid for numeric attributes.
		 * @param minValue the minimum value of the attribute
		 */
		public void setMinValue(Double minValue) {
			wrapped.getDmAttribute().setMinValue(minValue);
		}

		/**
		 * Only valid for numeric attributes.
		 * @return the maximum value of the attribute
		 */
		@Column(name="max_value")
		public Double getMaxValue() {
			return wrapped.getDmAttribute().getMaxValue();
		}

		/**
		 * Only valid for numeric attributes.
		 * @param maxValue the maximum value of the attribute
		 */
		public void setMaxValue(Double maxValue) {
			wrapped.getDmAttribute().setMaxValue(maxValue);
		}

		/**
		 * Only valid for text attributes.
		 * 
		 * @return a regex pattern for validating string values
		 */
		public String getRegex() {
			return wrapped.getDmAttribute().getRegex();
		}


		/**
		 * Only valid for text attributes.
		 * 
		 * @param regex the regex pattern for validating string values
		 */
		public void setRegex(String regex) {
			wrapped.getDmAttribute().setRegex(regex);
		}


		/**
		 * 
		 * Only valid for numeric attributes.
		 * 
		 * @return the set of aggregations that are valid for the attribute
		 */
		public List<Aggregation> getAggregations(){
			return wrapped.getDmAttribute().getAggregations();
		}
		
		/**
		 *  Only valid for numeric attributes.
		 *  
		 * @param aggs the set of aggregations that are valid for the attribute
		 */
		public void setAggregations(List<Aggregation> aggs){
			wrapped.getDmAttribute().setAggregations(aggs);
		}
		
		/**
		 * 
		 * @return list of active list items
		 */
		public List<AttributeListItem> getActiveListItems(){
			return wrapped.getDmAttribute().getActiveListItems();
		}

		/**
		 * Only valid for list attributes.
		 * 
		 * @return set of valid list elements
		 */
		public List<AttributeListItem> getAttributeList(){
			return wrapped.getDmAttribute().getAttributeList();
		}
		/**
		 * Only valid for list attributes.
		 * 
		 * @param attributeList the set of valid list elements
		 */
		public void setAttributeList(List<AttributeListItem> attributeList){
			wrapped.getDmAttribute().setAttributeList(attributeList);
		}
		

		/**
		 * Only valid for tree attributes.
		 * @return  set of root tree nodes
		 */
		public List<AttributeTreeNode> getTree(){
			return wrapped.getDmAttribute().getTree();
		}
		/**
		 * Only valid for tree attributes.
		 * 
		 * @param tree the set of root tree nodes
		 */
		public void setTree(List<AttributeTreeNode> tree){
			wrapped.getDmAttribute().setTree(tree);
		}
		
		
		/**
		 * Only valid for tree attributes.
		 * @return  set of root tree nodes
		 */
		public List<AttributeTreeNode> getActiveTreeNodes(){
			return wrapped.getDmAttribute().getActiveTreeNodes();
		}
		/**
		 * Only valid for tree attributes.
		 * 
		 * @param tree the set of root tree nodes
		 */
		public void setActiveTreeNodes(List<AttributeTreeNode> activeTootTreeNodes){
			wrapped.getDmAttribute().setActiveTreeNodes(activeTootTreeNodes);
		}
		
		
	}
}
