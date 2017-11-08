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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.Query;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog to create a new station or edit a station and associated attributes.
 * 
 * @author Emily
 *
 */
public class StationDialog extends TitleAreaDialog{

	private AssetStation toUpdate;
	
	private Text txtStation;
	private AttributeFieldEditor locationEditor;
	
	private List<AttributeFieldEditor> attributeEditors;
	
	@Inject
	private IEclipseContext context;
	
	public StationDialog(Shell parentShell, AssetStation toUpdate) {
		super(parentShell);
		this.toUpdate = toUpdate;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button okBtn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		
		okBtn.setEnabled(false);
	}
	
	@Override
	public void okPressed() {
		if (!validate()) {
			MessageDialog.openWarning(getShell(), "Error", "Cannot save changes until all attributes are valid.");
			return;
		}
		
		boolean isNew = toUpdate.getUuid() == null;
		toUpdate.setId(txtStation.getText());
		
		AssetStationAttributeValue tmp = new AssetStationAttributeValue();
		locationEditor.updateValue(tmp);
		toUpdate.setX(tmp.getNumberValue());
		toUpdate.setY(tmp.getNumberValue2());
		
		if (toUpdate.getAttributeValues() == null) toUpdate.setAttributeValues(new ArrayList<>());
		
		for (AttributeFieldEditor editor : attributeEditors) {
			AssetStationAttributeValue valueToUpdate = null;
			for (AssetStationAttributeValue v : toUpdate.getAttributeValues()) {
				if (v.getAttribute().equals(editor.getAttribute())) {
					valueToUpdate = v;
					break;
				}
			}
			if (valueToUpdate == null) {
				valueToUpdate = new AssetStationAttributeValue();
				valueToUpdate.setAttribute(editor.getAttribute());
				valueToUpdate.setStation(toUpdate);
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
				
				List<AssetStation> items = QueryFactory.buildQuery(s, AssetStation.class, 
						new Object[] {"conservationArea", toUpdate.getConservationArea()},
						new Object[] {"id", toUpdate.getId()}).list();
				for (AssetStation i : items) {
					if (!i.equals(toUpdate)) {
						MessageDialog.openError(getShell(), "Duplicate ID", 
								MessageFormat.format("A station with the id ''{0}'' already exists.  You cannot duplicate Station IDs.  Please use a different ID and try again.", toUpdate.getId()));
						return;
					}
				}
				
				s.saveOrUpdate(toUpdate);
				s.getTransaction().commit();
				
			}catch(Exception ex) {
				AssetPlugIn.displayLog("Unable to save changes to station: " + ex.getMessage(), ex);
				s.getTransaction().rollback();
				if (isNew) toUpdate.setUuid(null);
				return;
			}
		}
		context.get(IEventBroker.class).post(isNew ? AssetEvents.ASSETSTATION_NEW : AssetEvents.ASSETSTATION_MODIFIED, Collections.singletonList(toUpdate));
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	private boolean validate() {
		Button btnOk = getButton(IDialogConstants.OK_ID);
		btnOk.setEnabled(false);
		setErrorMessage(null);
		
		if (txtStation.getText().isEmpty()) {
			setErrorMessage("Station ID required");
			return false;
		}
		
		if (!locationEditor.isValid()) {
			setErrorMessage("A valid location for the station is required");
			return false;
		}
		AssetStationAttributeValue tmp = new AssetStationAttributeValue();
		locationEditor.updateValue(tmp);
		if (tmp.getNumberValue() == null || tmp.getNumberValue2() == null) {
			setErrorMessage("A location for the station is required");
			return false;
		}
		
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
		l.setText("ID:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		txtStation = new Text(form, SWT.BORDER);
		txtStation.setTextLimit(AssetStation.MAX_LENGTH);
		if (toUpdate.getId() != null) txtStation.setText(toUpdate.getId());
		txtStation.addListener(SWT.Modify, e->validate());
		txtStation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		AssetAttribute tmp = new AssetAttribute();
		tmp.setName("Position");
		tmp.setType(AttributeType.POSITION);
		
		SelectionListener validateListener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		
		locationEditor = new AttributeFieldEditor(form, tmp);
		locationEditor.addSelectionListener(validateListener);
		if (toUpdate.getX() != null) {
			AssetStationAttributeValue value = new AssetStationAttributeValue();
			value.setAttribute(tmp);
			value.setNumberValue(toUpdate.getX());
			value.setNumberValue2(toUpdate.getY());
			locationEditor.initControl(value);
		}
		
		//TODO:
		//locationEditor.initControl(value);
		
		l = new Label(form, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		ScrolledComposite scroll = new ScrolledComposite(form, SWT.V_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		
		Composite attributeComp = new Composite(scroll, SWT.NONE);
		scroll.setContent(attributeComp);
		attributeComp.setLayout(new GridLayout(2, false));
		
		List<AssetStationAttribute> attributes = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			String hql = "FROM AssetStationAttribute a WHERE a.attribute.conservationArea = :ca ORDER BY a.order";
			Query query = session.createQuery(hql);
			query.setParameter("ca",  SmartDB.getCurrentConservationArea());
			attributes.addAll(query.getResultList());
			attributes.forEach(a->{
				a.getAttribute().getName();
				if (a.getAttribute().getAttributeList() != null) {
					a.getAttribute().getAttributeList().forEach(li -> li.getName());
				}
			});
		}
		attributeEditors = new ArrayList<>();
		for (AssetStationAttribute stationattribute : attributes) {
			AttributeFieldEditor editor = new AttributeFieldEditor(attributeComp, stationattribute.getAttribute());
			attributeEditors.add(editor);

			if (editor.getTextAttributeControl() != null) editor.getTextAttributeControl().addListener(SWT.Resize, e-> scroll.setMinSize(attributeComp.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			editor.addSelectionListener(validateListener);
			
			if (toUpdate.getAttributeValues() != null) {
				for (AssetStationAttributeValue v : toUpdate.getAttributeValues()) {
					if (v.getAttribute().equals(stationattribute.getAttribute())) editor.initControl(v);
				}
			}

		}
		scroll.setMinSize(attributeComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setTitle("Station Attributes");
		setMessage("Configure the station and associated attributes");
		getShell().setText("Station Attributes");
		
		return parent;
	}
	
	
	
	@Override
	public boolean isResizable() {
		return true;
	}
}

