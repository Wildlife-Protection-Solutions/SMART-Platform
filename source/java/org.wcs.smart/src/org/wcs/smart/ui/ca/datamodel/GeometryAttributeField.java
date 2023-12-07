package org.wcs.smart.ui.ca.datamodel;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Attribute.GeometrySource;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.ca.datamodel.GeometryAttributeValue;
import org.wcs.smart.ui.DrawOnMapDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.SmartUtils;

public class GeometryAttributeField implements IAttributeField<GeometryAttributeValue>{

	private Attribute attribute;
	private boolean isModified = false;
	private Collection<Listener> listeners;
	
	private Label lbl;
	
	private Label lblinfo;
	
	private Button btnEdit, btnClear;
	private ControlDecoration cd;

	private GeometryAttributeValue originalValue;
	private GeometryAttributeValue value;
	
	/**
	 * Creates a new boolean attribute field
	 * @param attribute must be boolean type attribute
	 */
	public GeometryAttributeField(Attribute attribute){
		this.attribute = attribute;
		listeners = new ArrayList<>();
	}
	
	@Override
	public GeometryAttributeValue getValue() {
		return value;
	}

	@Override
	public void createComposite(Composite parent) {
		// TODO Auto-generated method stub
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(SmartUtils.formatStringForLabel(attribute.getName()) + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		Composite editorpart = new Composite(parent, SWT.NONE);
		editorpart.setLayout(new GridLayout(4, false));
		((GridLayout)editorpart.getLayout()).marginWidth = 0;
		((GridLayout)editorpart.getLayout()).marginHeight = 0;
		editorpart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnEdit = new Button(editorpart, SWT.PUSH);
		btnEdit.setBackground(btnEdit.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.addListener(SWT.Selection,e->editGeometry());
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnEdit.getLayoutData()).horizontalIndent = 5;
		
		cd = new ControlDecoration(btnEdit, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());

		btnClear = new Button(editorpart, SWT.PUSH);
		btnClear.setBackground(btnClear.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnClear.setText("Clear");
		btnClear.setToolTipText("remove all geometries associated with this attribute");
		btnClear.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnClear.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnClear.addListener(SWT.Selection,e->setValue(null));
		
		lblinfo = new Label(editorpart, SWT.NONE);
		lblinfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	
		
		validate();		
	}

	private void editGeometry() {
		//DrawOnMapDialog
		DrawOnMapDialog.Type type = DrawOnMapDialog.Type.POLYGON;
		if (attribute.getType() == AttributeType.LINE) {
			type = DrawOnMapDialog.Type.LINESTRING;
		}
		if (value == null) value = new GeometryAttributeValue(null, GeometrySource.MANUAL_DRAW);
		DrawOnMapDialog dialog = new DrawOnMapDialog(lbl.getShell(), type, value.getGeometry());
		if (dialog.open() != Window.OK) return;
		
		value.setGeometry(dialog.getGeometry());
		updateLabel();
		setModified(true);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		btnEdit.setEnabled(false);
	}

	@Override
	public String validate() {
		String error = AttributeValidator.validateAttribute(attribute, getValue());
		if (error != null){
			cd.setDescriptionText(error);
			cd.show();
		}else{
			cd.hide();
		}
		return error;
	}

	@Override
	public void addModifyListener(Listener listener) {
		listeners.add(listener);
		
	}
	
	protected void fireModified() {
		Event evt = new Event();
		for (Listener l : listeners)l.handleEvent(evt);
	}
	
	
	private void setModified(boolean isModified) {
		if (this.isModified != isModified) {
			this.isModified = isModified;
			fireModified();
		}
	}

	@Override
	public Attribute getAttribute() {
		return this.attribute;
	}

	@Override
	public void clear() {
		this.isModified = false;
		this.value = null;
		this.originalValue = null;
		updateLabel();
		validate();
	}
	

	@Override
	public boolean isModified() {
		return this.isModified;
	}

	private void updateLabel() {
		if (this.value == null) {
			this.lblinfo.setText("");
		}else {
			this.lblinfo.setText(GeometryUtils.getAttributeGeometryLabel(this.value));
		}
		this.btnClear.setEnabled(this.value != null);
	}
	
	@Override
	public void setValue(Object x) {
		
		if (x != null) {
			if(!(x instanceof GeometryAttributeValue)) {
				throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
			}
			if (attribute.getType() == Attribute.AttributeType.POLYGON && !((GeometryAttributeValue)x).isPolygon()) {
				throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
			}
			if (attribute.getType() == Attribute.AttributeType.LINE && !((GeometryAttributeValue)x).isLineString()) {
				throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
			}
		}
			
		this.originalValue = (GeometryAttributeValue)x;
		this.value = originalValue;
		updateLabel();
		validate();
		this.isModified = false;
	}

	@Override
	public void setFocus() {
		btnEdit.setFocus();
	}

	@Override
	public void dispose() {
	}

}
