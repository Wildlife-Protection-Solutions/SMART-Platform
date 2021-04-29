/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident;

import java.text.MessageFormat;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.IdGeneratorContribution;
import org.wcs.smart.IdGeneratorEngine;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

/**
 * Incident ID pattern configuration 
 * 
 * @author Emily
 *
 */
public class IncidentIdGeneratorContribution implements IdGeneratorContribution {

	private Text txtPattern;
	private Button btnUnique;
	private ControlDecoration cdPatternErr;
	private ControlDecoration cdPatternWarn;

	public IncidentIdGeneratorContribution() {
	}

	@Override
	public void initComponent(Session session) {
		
		ConservationAreaProperty prop = getProperty(IncidentIdGenerator.PATTERN_PROPERY_KEY, session);
		if (prop != null && prop.getValue() != null) {
			txtPattern.setText(prop.getValue());
		}
		
		prop = getProperty(IncidentIdGenerator.UNIQUE_PROPERTY_KEY, session);
		if (prop != null && prop.getValue() != null) {
			if (prop.getValue().equalsIgnoreCase(IncidentIdGenerator.UNQIUE_VALUE)) {
				btnUnique.setSelection(true);
			}else {
				btnUnique.setSelection(false);
			}
		}
		updateDecorations();
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;

		SmartUiUtils.createHeaderLabel(part, Messages.IncidentIdGeneratorContribution_SectionHeader);
		
		Composite inner = new Composite(part, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(inner, SWT.NONE);
		l.setText(Messages.IncidentIdGeneratorContribution_PatternLabel);
		
		txtPattern = new Text(inner, SWT.BORDER);
		txtPattern.setText(""); //$NON-NLS-1$
		txtPattern.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtPattern.getLayoutData()).widthHint = 100;
		txtPattern.addListener(SWT.Modify,e->updateDecorations());

		cdPatternErr = new ControlDecoration(txtPattern, SWT.LEFT);
		cdPatternErr.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdPatternErr.setShowHover(true);
		cdPatternErr.hide();
		
		cdPatternWarn = new ControlDecoration(txtPattern, SWT.LEFT);
		cdPatternWarn.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
		cdPatternWarn.setShowHover(true);
		cdPatternWarn.hide();
		
		l = new Label(inner, SWT.NONE);
		l.setText(Messages.IncidentIdGeneratorContribution_UniqueLabel);
		l.setToolTipText(Messages.IncidentIdGeneratorContribution_UniqueTooltip);

		btnUnique = new Button(inner, SWT.CHECK);
		btnUnique.setSelection(true);
		btnUnique.addListener(SWT.Selection,e->updateDecorations());
		
		return part;
	}
	
	private void updateDecorations() {
		String error = validate();
		cdPatternWarn.hide();
		
		if (error != null) {
			cdPatternErr.setDescriptionText(error);
			cdPatternErr.show();
			return;
		}
		
		cdPatternErr.hide();
		if (!txtPattern.getText().trim().isBlank()) {
			if (!btnUnique.getSelection() && IdGeneratorEngine.INSTANCE.likelyDuplicate(txtPattern.getText())) {
				cdPatternWarn.setDescriptionText(Messages.IncidentIdGeneratorContribution_DuplicateWarning);
				cdPatternWarn.show();
			}
		}
	}
	
	
	private String validate() {
		String text = txtPattern.getText();
		
		if (text.trim().isBlank()) return null;
		
		for (IdGeneratorEngine.Token token : IdGeneratorEngine.Token.values()) {
			text = text.replace(token.token, ""); //$NON-NLS-1$
		}
		
		if (!SmartUtils.isSimpleString(text.trim(), 
				SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Waypoint.ID_MAX_LENGTH) ) {
			return MessageFormat.format(Messages.IncidentIdGeneratorContribution_InvalidPattern, Waypoint.ID_MAX_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
		}
		return null;		
	}
	
	
	@Override
	public boolean save(Session session) {
		
		if (validate() != null) return false;
		ConservationAreaProperty prop = getProperty(IncidentIdGenerator.PATTERN_PROPERY_KEY, session);
		if (prop == null) {
			prop = new ConservationAreaProperty();
			prop.setConservationArea(SmartDB.getCurrentConservationArea());
			prop.setKey(IncidentIdGenerator.PATTERN_PROPERY_KEY);
		}
		prop.setValue(txtPattern.getText());
		session.saveOrUpdate(prop);
		
		prop = getProperty(IncidentIdGenerator.UNIQUE_PROPERTY_KEY, session);
		if (prop == null) {
			prop = new ConservationAreaProperty();
			prop.setConservationArea(SmartDB.getCurrentConservationArea());
			prop.setKey(IncidentIdGenerator.UNIQUE_PROPERTY_KEY);
		}
		if (btnUnique.getSelection()) {
			prop.setValue(IncidentIdGenerator.UNQIUE_VALUE);
		}else {
			prop.setValue(IncidentIdGenerator.NOTUNIQUE_VALUE);
		}
		session.saveOrUpdate(prop);
		
		return true;
	}
	
	private ConservationAreaProperty getProperty(String key, Session session) {
		return QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"key", key}).uniqueResult(); //$NON-NLS-1$
	}

}
