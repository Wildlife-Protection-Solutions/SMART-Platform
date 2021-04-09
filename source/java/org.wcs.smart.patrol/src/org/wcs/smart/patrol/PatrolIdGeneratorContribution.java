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
package org.wcs.smart.patrol;

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
import org.wcs.smart.IdGeneratorManager;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol ID pattern configuration 
 * 
 * @author Emily
 *
 */
public class PatrolIdGeneratorContribution implements IdGeneratorContribution {


	
	private Text txtPattern;
	private Button btnUnique;
	private ControlDecoration cdPattern;
	
	public PatrolIdGeneratorContribution() {
	}

	private ConservationAreaProperty getProperty(String key, Session session) {
		return QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"key", key}).uniqueResult(); //$NON-NLS-1$
	}
	
	@Override
	public void initComponent(Session session) {
		
		ConservationAreaProperty prop = getProperty(PatrolIdGenerator.PATTERN_PROPERY_KEY, session);
		if (prop != null && prop.getValue() != null) {
			txtPattern.setText(prop.getValue());
		}
		
		prop = getProperty(PatrolIdGenerator.UNIQUE_PROPERTY_KEY, session);
		if (prop != null && prop.getValue() != null) {
			if (prop.getValue().equalsIgnoreCase(PatrolIdGenerator.UNIQUE_VALUE)) {
				btnUnique.setSelection(true);
			}else {
				btnUnique.setSelection(false);
			}
		}
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;

		SmartUiUtils.createHeaderLabel(part, Messages.PatrolIdGeneratorContribution_SectionHeader);
		
		Composite inner = new Composite(part, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(inner, SWT.NONE);
		l.setText(Messages.PatrolIdGeneratorContribution_PatternLabel);
		
		txtPattern = new Text(inner, SWT.BORDER);
		txtPattern.setText(""); //$NON-NLS-1$
		txtPattern.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtPattern.addListener(SWT.Modify,e->{
			String error = validate();
			if (error == null) {
				cdPattern.hide();
			}else {
				cdPattern.setDescriptionText(error);
				cdPattern.show();
			}
		});
		
		cdPattern = new ControlDecoration(txtPattern, SWT.LEFT);
		cdPattern.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdPattern.setShowHover(true);
		cdPattern.hide();
		
		l = new Label(inner, SWT.NONE);
		l.setText(Messages.PatrolIdGeneratorContribution_UnqiueLabel);
		l.setToolTipText(Messages.PatrolIdGeneratorContribution_UniqueTooltip);

		btnUnique = new Button(inner, SWT.CHECK);
		btnUnique.setSelection(true);
		
		Text info = new Text(inner, SWT.BORDER | SWT.MULTI);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		info.setEditable(false);
		
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.PatrolIdGeneratorContribution_PatrolTokens);
		sb.append("\n"); //$NON-NLS-1$
		IdGeneratorEngine.Token[] tokens = new IdGeneratorEngine.Token[] {
				IdGeneratorEngine.Token.LEADER_FAMILY, 
				IdGeneratorEngine.Token.LEADER_GIVEN, 
				IdGeneratorEngine.Token.LEADER_INITIALS};
		
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

	private String validate() {
		String text = txtPattern.getText();
		
		for (IdGeneratorEngine.Token token : IdGeneratorEngine.Token.values()) {
			text = text.replace(token.token, ""); //$NON-NLS-1$
		}
		
		if (!SmartUtils.isSimpleString(text.trim(), 
				SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Patrol.MAX_ID_LENGTH) ) {
			return MessageFormat.format(Messages.PatrolIdGeneratorContribution_InvalidPattern, Patrol.MAX_ID_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
		}
		return null;		
	}
	
	
	@Override
	public boolean save(Session session) {
		
		if (validate() != null) return false;
		
		ConservationAreaProperty prop = getProperty(PatrolIdGenerator.PATTERN_PROPERY_KEY, session);
		if (prop == null) {
			prop = new ConservationAreaProperty();
			prop.setConservationArea(SmartDB.getCurrentConservationArea());
			prop.setKey(PatrolIdGenerator.PATTERN_PROPERY_KEY);
		}
		prop.setValue(txtPattern.getText());
		session.saveOrUpdate(prop);
		
		prop = getProperty(PatrolIdGenerator.UNIQUE_PROPERTY_KEY, session);
		if (prop == null) {
			prop = new ConservationAreaProperty();
			prop.setConservationArea(SmartDB.getCurrentConservationArea());
			prop.setKey(PatrolIdGenerator.UNIQUE_PROPERTY_KEY);
		}
		if (btnUnique.getSelection()) {
			prop.setValue(PatrolIdGenerator.UNIQUE_VALUE);
		}else {
			prop.setValue(PatrolIdGenerator.NOTUNIQUE_VALUE);
		}
		session.saveOrUpdate(prop);
		
		return true;
	}

}
