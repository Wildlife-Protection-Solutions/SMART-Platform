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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.ui.support.Ct2AttributeTypeLabelProvider;
import org.wcs.smart.ct2smart.ui.support.SmartAttributeLabelProvider;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class MatchAttributeComposite extends Composite {

	private Label mapToLabel;
	private ComboViewer typeComboViewer;
	private ComboViewer mapToComboViewer;
	
	private ExtraAttributeComposite extraAttrCmp;
	
	public MatchAttributeComposite(Composite parent, DataModelLookup lookup) {
		super(parent, SWT.NONE);

		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		this.setLayoutData(gridData);

		Label typeLabel = new Label(this, SWT.NONE);
		typeLabel.setText("Type");

		typeComboViewer =  new ComboViewer(this, SWT.READ_ONLY);
		typeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		typeComboViewer.setLabelProvider(new Ct2AttributeTypeLabelProvider());
		typeComboViewer.setInput(Ct2AttributeType.values());

		mapToLabel = new Label(this, SWT.NONE);
		mapToLabel.setText("Smart Attribute");
		
		mapToComboViewer =  new ComboViewer(this, SWT.READ_ONLY);
		mapToComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		mapToComboViewer.setLabelProvider(new SmartAttributeLabelProvider(lookup));
		
		extraAttrCmp = new ExtraAttributeComposite(this, lookup);
		
	}

	public void setInput(Ct2Attribute attribute) {
		extraAttrCmp.setInput(attribute);
		
	}
}
