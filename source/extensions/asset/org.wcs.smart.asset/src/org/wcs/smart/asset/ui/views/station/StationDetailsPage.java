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
package org.wcs.smart.asset.ui.views.station;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetSecurityManager;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.asset.ui.AttributeFieldEditor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Station editor details/properties page
 * 
 * @author Emily
 *
 */
public class StationDetailsPage {

	private static final String STATION_KEY = "STATION"; //$NON-NLS-1$

	private StationEditor parentEditor;
	
	private List<AttributeFieldEditor> fieldEditors;
	
	private Composite attributePanel;
	private FormToolkit toolkit;
	
	private AssetStationAttributeValue tmpLocationAttribute;
	private AttributeFieldEditor locFieldEditor;
	
	private boolean isInitializing = false;
	
	public StationDetailsPage(StationEditor editor) {
		this.parentEditor = editor;
		
		AssetAttribute tmp = new AssetAttribute();
		tmp.setType(AttributeType.POSITION);
		tmp.setName(Messages.StationDetailsPage_PositionLabel);
		tmpLocationAttribute = new AssetStationAttributeValue();
		tmpLocationAttribute.setAttribute(tmp);
	}
	
	public void createControl(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		Composite toppanel = toolkit.createComposite(panel, SWT.BORDER);
		toppanel.setLayout(new GridLayout(3, false));
		toppanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		locFieldEditor = new AttributeFieldEditor(toppanel, tmpLocationAttribute.getAttribute());
		locFieldEditor.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!locFieldEditor.isValid()) return;
				if (isInitializing) return;
				if (locFieldEditor.updateValue(tmpLocationAttribute)) {
					AssetStation station = (AssetStation) attributePanel.getData(STATION_KEY);
					if (station != null) {
						station.setX(tmpLocationAttribute.getNumberValue());
						station.setY(tmpLocationAttribute.getNumberValue2());
					}
				}
				parentEditor.setDirty(true);
				
			}
		});
		locFieldEditor.setEnabled(AssetSecurityManager.INSTANCE.canEditStationLocation());

		Composite attributeComp = toolkit.createComposite(panel, SWT.BORDER);
		attributeComp.setLayout(new GridLayout());
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label l = toolkit.createLabel(attributeComp, Messages.StationDetailsPage_AttributesLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.setFont(boldFont);
		l.addListener(SWT.Dispose,  e-> boldFont.dispose());

		attributePanel = toolkit.createComposite(attributeComp);
		attributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributePanel.setLayout(new GridLayout());
		((GridLayout)attributePanel.getLayout()).marginWidth = 0;
		((GridLayout)attributePanel.getLayout()).marginHeight = 0;
	}
	
	public void initializeAttributes(AssetStation station) {
		if (station == null) return;
		tmpLocationAttribute.setNumberValue(station.getX());
		tmpLocationAttribute.setNumberValue2(station.getY());
		
		try {
			isInitializing = true;
			locFieldEditor.initControl(tmpLocationAttribute);
			attributePanel.setData(STATION_KEY, station);
		}finally {
			isInitializing = false;
		}
		
		for (Control c : attributePanel.getChildren()) c.dispose();
		fieldEditors = new ArrayList<>();
		
		ScrolledComposite scroll = new ScrolledComposite(attributePanel, SWT.V_SCROLL);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(scroll);
		
		Composite attributes = toolkit.createComposite(scroll);
		attributes.setLayout(new GridLayout(2, false));
		scroll.setContent(attributes);
		
		List<AssetStationAttribute> stationAttributes = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			stationAttributes.addAll(
					session.createQuery("FROM AssetStationAttribute WHERE attribute.conservationArea = :ca", AssetStationAttribute.class) //$NON-NLS-1$
					.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.list());
			stationAttributes.forEach(ss->{
				ss.getAttribute().getName();
				if (ss.getAttribute().getAttributeList() != null) ss.getAttribute().getAttributeList().forEach(li->li.getName());
			});
		}
		
		for (AssetStationAttribute attribute : stationAttributes) {
			
			AttributeFieldEditor editor = new AttributeFieldEditor(attributes, attribute.getAttribute());
			editor.adapt(toolkit);
			fieldEditors.add(editor);
			editor.setEnabled(AssetSecurityManager.INSTANCE.canEditStationLocation());
			if (editor.getTextAttributeControl() != null) {
				editor.getTextAttributeControl().addListener(SWT.Resize, e-> scroll.setMinSize(attributes.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			}
			
			for (AssetStationAttributeValue v : station.getAttributeValues()) {
				if (v.getAttribute().equals(attribute.getAttribute())) editor.initControl(v);
			}
			
			editor.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (!editor.isValid()) return;
					AssetStationAttributeValue toUpdate = null;
					for (AssetStationAttributeValue v : station.getAttributeValues()) {
						if (v.getAttribute().equals(editor.getAttribute())) {
							toUpdate = v;
							break;
						}
					}
					boolean isNew = false;
					if (toUpdate == null) {
						isNew = true;
						toUpdate = new AssetStationAttributeValue();
						toUpdate.setStation(station);
						toUpdate.setAttribute(editor.getAttribute());
					}
					if (editor.updateValue(toUpdate)) {
						if (isNew) station.getAttributeValues().add(toUpdate);
					}else {
						if (!isNew) station.getAttributeValues().remove(toUpdate);
					}
					parentEditor.setDirty(true);
					
				}
			});
		}
		scroll.setMinSize(attributes.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scroll.getParent().layout(true);
	}
}
