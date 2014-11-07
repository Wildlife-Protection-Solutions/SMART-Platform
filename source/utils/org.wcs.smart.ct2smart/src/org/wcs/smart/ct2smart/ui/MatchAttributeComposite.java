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
package org.wcs.smart.ct2smart.ui;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.ui.support.Ct2AttributeTypeLabelProvider;
import org.wcs.smart.ct2smart.ui.support.SmartAttributeLabelProvider;
import org.wcs.smart.ct2smart.util.Ct2AttributeTypeUtil;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class MatchAttributeComposite extends Composite implements ILanguageChangedListener {

	private Ct2Attribute attribute;
	
	private Label typeLabel;
	private Label mapToLabel;
	
	private Ct2AttributeTypeLabelProvider typeLabelProvider;
	private SmartAttributeLabelProvider attrLabelProvider;
	
	private Ct2AttributeEAComposite extraAttrCmp;
	private ValueMapComposite valueMapCmp;
	
	private Connection connection;
	
	public MatchAttributeComposite(Composite parent, DataModelLookup lookup, Connection c) {
		super(parent, SWT.NONE);
		this.connection = c;
		typeLabelProvider = new Ct2AttributeTypeLabelProvider();
		attrLabelProvider = new SmartAttributeLabelProvider(lookup);

		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		this.setLayoutData(gridData);
		
		Group group = new Group(this, SWT.NONE);
		group.setText("Attribute details");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		group.setLayout(new GridLayout(1, false));

//		Composite labelContainer = new Composite(group, SWT.NONE);
//		GridLayout gd = new GridLayout(2, true);
//		gd.marginBottom = 0;
//		gd.marginHeight = 0;
//		gd.marginLeft = 0;
//		gd.marginRight = 0;
//		gd.marginTop = 0;
//		gd.marginWidth = 0;
//		labelContainer.setLayout(gd);
//		labelContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		typeLabel = new Label(group, SWT.NONE);
		mapToLabel = new Label(group, SWT.NONE);
		
		Button rawBtn = new Button(group, SWT.PUSH);
		rawBtn.setText("View Raw Values");
		rawBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				showRawValues();
			}
		});
		
		extraAttrCmp = new Ct2AttributeEAComposite(group, lookup);
		valueMapCmp = new ValueMapComposite(group, lookup);
		
	}

	protected void showRawValues() {
		try {
			ResultSet attrRs = connection.createStatement().executeQuery("select n, i, id from CT_TO_SMART.ATTRIBUTES where i = '" + attribute.getI() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			if (attrRs.next()) {
				String valuesSql = "select distinct a"+attrRs.getString(3)+" from CT_TO_SMART.CSV"; //$NON-NLS-1$ //$NON-NLS-2$
				ResultSet valRs = connection.createStatement().executeQuery(valuesSql);
				String values = "Raw values for \"" + attribute.getN() + "\":"; //$NON-NLS-1$ //$NON-NLS-2$
				while (valRs.next()) {
					values += "\n" + valRs.getString(1); //$NON-NLS-1$
				}
				MessageDialog.openInformation(getShell(), "Raw values", values);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		
		
	}

	public void setInput(Ct2Attribute attribute) {
		this.attribute = attribute;
		typeLabel.setText("Type: " + typeLabelProvider.getText(attribute.getType()));
		mapToLabel.setText("Smart Attribute: " + attrLabelProvider.getText(attribute.getMapTo()));
		
		if (Ct2AttributeTypeUtil.canMap(attribute.getType())) {
			mapToLabel.setText("Smart Attribute: " + attrLabelProvider.getText(attribute.getMapTo()));
			mapToLabel.setVisible(true);
			extraAttrCmp.setInput(attribute);
			extraAttrCmp.setVisible(true);
		} else {
			mapToLabel.setVisible(false);
			extraAttrCmp.setVisible(false);
		}

		if (Ct2AttributeType.REF.equals(attribute.getType()) || Ct2AttributeType.REF_BOOL.equals(attribute.getType())) {
			valueMapCmp.setInput(attribute);
			valueMapCmp.setVisible(true);
		} else {
			valueMapCmp.setVisible(false);
		}

		this.layout(true, true);
	}

	@Override
	public void languageChanged(String langCode) {
		attrLabelProvider.languageChanged(langCode);
		if (attribute != null) {
			mapToLabel.setText("Smart Attribute: " + attrLabelProvider.getText(attribute.getMapTo()));
		}
		extraAttrCmp.languageChanged(langCode);
		valueMapCmp.languageChanged(langCode);
		
	}
}
