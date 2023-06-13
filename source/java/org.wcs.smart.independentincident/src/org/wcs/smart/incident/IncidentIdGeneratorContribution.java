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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.wcs.smart.IdGeneratorManager;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
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
	private Text txtUnqPattern;
	private ControlDecoration cdPatternErr;
	private ControlDecoration cdPatternUnqErr;
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
		
		prop = getProperty(IncidentIdGenerator.UNIQUE_PATTERN_PROPERTY_KEY, session);
		if (prop != null && prop.getValue() != null) {
			txtUnqPattern.setText(prop.getValue());
		}else {
			txtUnqPattern.setText(IdGeneratorEngine.DEFAULT_UNIQUE_STR);
		}
		
		txtUnqPattern.setEnabled(btnUnique.getSelection());
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
		txtPattern.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
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
		btnUnique.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true, 1, 1));
		
		Label pl = new Label(inner, SWT.NONE);
		pl.setText(Messages.IncidentIdGeneratorContribution_UnqPatternLbl);
		pl.setToolTipText(Messages.IncidentIdGeneratorContribution_UnqPatternTooltip);

		txtUnqPattern = new Text(inner, SWT.BORDER);
		txtUnqPattern.setText(IdGeneratorEngine.DEFAULT_UNIQUE_STR);
		txtUnqPattern.addListener(SWT.Modify,e->updateDecorations());
		txtUnqPattern.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtUnqPattern.getLayoutData()).horizontalIndent = 3;
		
		cdPatternUnqErr = new ControlDecoration(txtUnqPattern, SWT.LEFT);
		cdPatternUnqErr.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdPatternUnqErr.setShowHover(true);
		cdPatternUnqErr.hide();
		
		btnUnique.addListener(SWT.Selection,e->{
			updateDecorations();
			txtUnqPattern.setEnabled(btnUnique.getSelection());
			pl.setEnabled(btnUnique.getSelection());	
		});
		txtUnqPattern.setEnabled(btnUnique.getSelection());
		pl.setEnabled(btnUnique.getSelection());
		
		Text info = new Text(inner, SWT.BORDER | SWT.MULTI);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		info.setEditable(false);
		
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.IncidentIdGeneratorContribution_incidentpatteronly);
		sb.append("\n"); //$NON-NLS-1$
		IdGeneratorEngine.Token[] tokens = new IdGeneratorEngine.Token[] {
				IdGeneratorEngine.Token.OBSERVER_FAMILY, 
				IdGeneratorEngine.Token.OBSERVER_GIVEN, 
				IdGeneratorEngine.Token.OBSERVER_INITIALS};
		
		for (IdGeneratorEngine.Token token : tokens) {
			sb.append(token.token);
			sb.append(" - "); //$NON-NLS-1$
			sb.append(IdGeneratorManager.INSTANCE.getDescription(token));
			sb.append("\n"); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		info.setText(sb.toString());
		
		return part;
	}
	
	private void updateDecorations() {
		String error = validateUniquePattern();
		if (error != null) {
			cdPatternUnqErr.setDescriptionText(error);
			cdPatternUnqErr.show();
		}else {
			cdPatternUnqErr.hide();
		}
		
		
		error = validatePattern();
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
	
	private String validatePattern() {
		String text = txtPattern.getText();
		
		if (text.trim().isBlank()) return null;
		
		for (IdGeneratorEngine.Token token : IdGeneratorEngine.Token.values()) {
			text = text.replace(token.token, ""); //$NON-NLS-1$
		}
		
		if (!text.isBlank() && !SmartUtils.isSimpleString(text.trim(), 
				SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Waypoint.ID_MAX_LENGTH) ) {
			return MessageFormat.format(Messages.IncidentIdGeneratorContribution_InvalidPattern, Waypoint.ID_MAX_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
		}
		return null;
	}

	private String validateUniquePattern() {
		//validate unique pattern
		if (btnUnique.getSelection()) {
			String text = txtUnqPattern.getText();
			if (text.isBlank() ) {
				return Messages.IncidentIdGeneratorContribution_InvalidUnqPattern;
			}
			Pattern ptn = Pattern.compile("(.*)\\{(0+)\\}(.*)"); //$NON-NLS-1$
			Matcher m = ptn.matcher(text);
			if (!m.matches()) {
				return Messages.IncidentIdGeneratorContribution_InvalidUnqPattern;
			}
			String part = text.replaceAll("\\{" + m.group(2) + "\\}", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (part.length() > 0 && 
					!SmartUtils.isSimpleString(part,SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX)) {
				return Messages.IncidentIdGeneratorContribution_InvalidUnqPattern;
			}
		}
		return null;
	}
	
	private String validate() {
		String x = validatePattern();
		if (x != null) return x;
		x = validateUniquePattern();
		return x;			
	}
	
	
	@Override
	public boolean save(Session session) {
		
		if (validate() != null) return false;
		ConservationAreaProperty prop = getOrCreateProperty(IncidentIdGenerator.PATTERN_PROPERY_KEY, session);
		prop.setValue(txtPattern.getText());
		HibernateManager.saveOrMerge(session,  prop);
		
		prop = getOrCreateProperty(IncidentIdGenerator.UNIQUE_PROPERTY_KEY, session);
		if (btnUnique.getSelection()) {
			prop.setValue(IncidentIdGenerator.UNQIUE_VALUE);
		}else {
			prop.setValue(IncidentIdGenerator.NOTUNIQUE_VALUE);
		}
		HibernateManager.saveOrMerge(session,  prop);
		
		prop = getOrCreateProperty(IncidentIdGenerator.UNIQUE_PATTERN_PROPERTY_KEY, session);
		prop.setValue(txtUnqPattern.getText());
		HibernateManager.saveOrMerge(session,  prop);
		
		return true;
	}
	
	private ConservationAreaProperty getOrCreateProperty(String key, Session session) {
		ConservationAreaProperty prop = getProperty(key, session);
		if (prop == null) {
			prop = new ConservationAreaProperty();
			prop.setConservationArea(SmartDB.getCurrentConservationArea());
			prop.setKey(key);
		}
		return prop;
	}
	
	private ConservationAreaProperty getProperty(String key, Session session) {
		return QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"key", key}).uniqueResult(); //$NON-NLS-1$
	}

}
