/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog to create a new station location or edit a station location and associated attributes.
 * 
 * @author Emily
 *
 */
public class StationLocationDialog extends SmartStyledTitleDialog{

	private AssetStationLocation toUpdate;
	
	private Text txtStationLocation;
	
	private AttributeFieldEditor locationEditor;
	private BufferAttributeFieldEditor bufferEditor;
	
	private List<AttributeFieldEditor> attributeEditors;
	
	@Inject
	private IEclipseContext context;
	
	public StationLocationDialog(Shell parentShell, AssetStationLocation toUpdate) {
		super(parentShell);
		this.toUpdate = toUpdate;
		
		if (toUpdate.getBuffer() == null) toUpdate.setBuffer(AssetModuleSettings.LOCATION_BUFFER_DEFAULT_VALUE);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button okBtn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
		okBtn.setEnabled(false);
		validate();
	}
	
	@Override
	public void okPressed() {
		if (!validate()) {
			MessageDialog.openWarning(getShell(), Messages.StationLocationDialog_ErrorTitle, Messages.StationLocationDialog_InvalidAttributes);
			return;
		}
		
		
		toUpdate.setId(txtStationLocation.getText());
		
		AssetStationLocationAttributeValue tmp = new AssetStationLocationAttributeValue();
		locationEditor.updateValue(tmp);
		toUpdate.setX(tmp.getNumberValue());
		toUpdate.setY(tmp.getNumberValue2());
		
		bufferEditor.updateValue(tmp);
		toUpdate.setBuffer(tmp.getNumberValue());
		
		if (toUpdate.getAttributeValues() == null) toUpdate.setAttributeValues(new ArrayList<>());
		
		for (AttributeFieldEditor editor : attributeEditors) {
			AssetStationLocationAttributeValue valueToUpdate = null;
			for (AssetStationLocationAttributeValue v : toUpdate.getAttributeValues()) {
				if (v.getAttribute().equals(editor.getAttribute())) {
					valueToUpdate = v;
					break;
				}
			}
			if (valueToUpdate == null) {
				valueToUpdate = new AssetStationLocationAttributeValue();
				valueToUpdate.setAttribute(editor.getAttribute());
				valueToUpdate.setStationLocation(toUpdate);
			}
			boolean add = editor.updateValue(valueToUpdate);
			if (add) {
				toUpdate.getAttributeValues().add(valueToUpdate);
			}else {
				toUpdate.getAttributeValues().remove(valueToUpdate);
			}
		}
		
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				HibernateManager.saveOrMerge(s, toUpdate);
				s.getTransaction().commit();
				
			}catch(Exception ex) {
				AssetPlugIn.displayLog(Messages.StationLocationDialog_SaveError + ex.getMessage(), ex);
				s.getTransaction().rollback();
				return;
			}
		}
		context.get(IEventBroker.class).post(AssetEvents.ASSETSTATION_MODIFIED, Collections.singletonList(toUpdate.getStation()));

		super.okPressed();
	}
	
	private boolean validate() {
		Button btnOk = getButton(IDialogConstants.OK_ID);
		if(btnOk == null) return false;
		
		btnOk.setEnabled(false);
		setErrorMessage(null);
		
		if (txtStationLocation.getText().isEmpty()) {
			setErrorMessage(Messages.StationLocationDialog_IdRequired);
			return false;
		}
		if (!locationEditor.isValid()) {
			setErrorMessage(Messages.StationLocationDialog_ValidPositionRequired);
			return false;
		}
		AssetStationLocationAttributeValue tmp = new AssetStationLocationAttributeValue();
		locationEditor.updateValue(tmp);
		if (tmp.getNumberValue() == null || tmp.getNumberValue2() == null) {
			setErrorMessage(Messages.StationLocationDialog_ValidPositionRequired);
			return false;
		}
		if (!bufferEditor.isValid()) {
			setErrorMessage(Messages.StationLocationDialog_InvalidBuffer);
			return false;
		}
		//check duplicate station id
		
		if (attributeEditors != null) {
			boolean ok = true;
			for (AttributeFieldEditor e : attributeEditors) {
				if (!e.isValid()) {
					ok = false;
					break;
				}
			};
			if (!ok) return false;
		}
		
		
		btnOk.setEnabled(true);
		return true;
	}
	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
//		parent.setLayout(new GridLayout());
		
		Composite form = new Composite(parent, SWT.NONE);
		form.setLayout(new GridLayout(2, false));
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(form, SWT.NONE);
		l.setText(Messages.StationLocationDialog_StationLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		l = new Label(form, SWT.NONE);
		l.setText(toUpdate.getStation().getId());
		
		
		l = new Label(form, SWT.NONE);
		l.setText(Messages.StationLocationDialog_IdLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		SelectionListener validatelistener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		
		txtStationLocation = new Text(form, SWT.BORDER);
		txtStationLocation.setTextLimit(AssetStation.MAX_LENGTH);
		if (toUpdate.getId() != null) {
			txtStationLocation.setText(toUpdate.getId());
		}else if (toUpdate.getStation() != null){
			txtStationLocation.setText(toUpdate.getStation().getId() + " - " + Messages.StationLocationDialog_LocationLabel + (toUpdate.getStation().getLocations().size() + 1)); //$NON-NLS-1$
		}
		txtStationLocation.addListener(SWT.Modify, e->validate());
		txtStationLocation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		AssetAttribute tmp = new AssetAttribute();
		tmp.setName(Messages.StationLocationDialog_PositionAttributeName);
		tmp.setType(AttributeType.POSITION);
		
		locationEditor = new AttributeFieldEditor(form, tmp);
		if (toUpdate.getX() != null) {
			AssetStationLocationAttributeValue value = new AssetStationLocationAttributeValue();
			value.setAttribute(tmp);
			value.setNumberValue(toUpdate.getX());
			value.setNumberValue2(toUpdate.getY());
			locationEditor.initControl(value);
		}else if (toUpdate.getStation() != null) {
			AssetStationLocationAttributeValue value = new AssetStationLocationAttributeValue();
			value.setAttribute(tmp);
			value.setNumberValue(toUpdate.getStation().getX());
			value.setNumberValue2(toUpdate.getStation().getY());
			locationEditor.initControl(value);
		}
		locationEditor.addSelectionListener(validatelistener);
		
		tmp = new AssetAttribute();
		tmp.setName(Messages.StationLocationDialog_BufferLabel);
		tmp.setType(AttributeType.NUMERIC);
		
		bufferEditor = new BufferAttributeFieldEditor(form, tmp);
		AssetStationLocationAttributeValue value = new AssetStationLocationAttributeValue();
		value.setAttribute(tmp);
		value.setNumberValue(toUpdate.getBuffer());
		bufferEditor.initControl(value);
		bufferEditor.addSelectionListener(validatelistener);
		
		l = new Label(form, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		ScrolledComposite scroll = new ScrolledComposite(form, SWT.V_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		
		Composite attributeComp = new Composite(scroll, SWT.NONE);
		scroll.setContent(attributeComp);
		attributeComp.setLayout(new GridLayout(2, false));
		
		List<AssetStationLocationAttribute> attributes = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			String hql = "FROM AssetStationLocationAttribute a WHERE a.attribute.conservationArea = :ca ORDER BY a.order"; //$NON-NLS-1$
			Query<AssetStationLocationAttribute> query = session.createQuery(hql, AssetStationLocationAttribute.class);
			query.setParameter("ca",  SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
			attributes.addAll(query.getResultList());
			attributes.forEach(a->{
				a.getAttribute().getName();
				if (a.getAttribute().getAttributeList() != null) {
					a.getAttribute().getAttributeList().forEach(li -> li.getName());
				}
			});
		}
		attributeEditors = new ArrayList<>();
		for (AssetStationLocationAttribute stationattribute : attributes) {
			AttributeFieldEditor editor = new AttributeFieldEditor(attributeComp, stationattribute.getAttribute());
			attributeEditors.add(editor);

			if (editor.getTextAttributeControl() != null) editor.getTextAttributeControl().addListener(SWT.Resize, e-> scroll.setMinSize(attributeComp.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			editor.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					validate();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
			
			if (toUpdate.getAttributeValues() != null) {
				for (AssetStationLocationAttributeValue v : toUpdate.getAttributeValues()) {
					if (v.getAttribute().equals(stationattribute.getAttribute())) editor.initControl(v);
				}
			}
		}
		scroll.setMinSize(attributeComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setTitle(Messages.StationLocationDialog_Title);
		setMessage(Messages.StationLocationDialog_Message);
		getShell().setText(Messages.StationLocationDialog_Title);
		
		
		return parent;
	}
	
	
	
	@Override
	public boolean isResizable() {
		return true;
	}
}

