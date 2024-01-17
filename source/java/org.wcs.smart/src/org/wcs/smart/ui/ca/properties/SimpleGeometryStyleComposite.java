/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.ui.ca.properties;

import java.awt.Color;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeGeometryStyle;

/**
 * Composite for displaying simple style options (line color, width and
 * fill color)
 * 
 */
public class SimpleGeometryStyleComposite extends Composite{

	
	public SimpleGeometryStyleComposite(Composite parent, int style, boolean canEdit) {
		super(parent, style);
		this.type = Attribute.AttributeType.LINE;
		createContents(canEdit);
	}
	
	
	private ColorSelector btnLineColor;
	private Spinner spnLineAlpaha;
	private ColorSelector btnFillColor;
	private Spinner spnFillAlpaha;
	private Text txtLineSize;
	
	private Listener[] btnLineColorl;
	private Listener[] btnFillColorl;
	
	private Label lfill;
	
	private ControlDecoration cdLineSize;
	
	private Attribute.AttributeType type;

	public void setEditable(boolean isEditable) {
		txtLineSize.setEditable(isEditable);
		spnFillAlpaha.setEnabled(isEditable);
		spnLineAlpaha.setEnabled(isEditable);
		if (isEditable) {
			for(Listener l : btnLineColorl) {
				btnLineColor.getButton().addListener(SWT.Selection, l);
			}
			for(Listener l : btnFillColorl) {
				btnFillColor.getButton().addListener(SWT.Selection, l);
			}
		}else {
			for(Listener l : btnLineColorl) {
				btnLineColor.getButton().removeListener(SWT.Selection, l);
			}
			for(Listener l : btnFillColorl) {
				btnFillColor.getButton().removeListener(SWT.Selection, l);
			}
		}	
	}
	
	public void setType(Attribute.AttributeType type) {
		this.type = type;
		lfill.setVisible(type == Attribute.AttributeType.POLYGON);
		btnFillColor.getButton().setVisible(type == Attribute.AttributeType.POLYGON);
		spnFillAlpaha.setVisible(type == Attribute.AttributeType.POLYGON);
	}
	
	public boolean validate() {
		cdLineSize.hide();
		try {
			Integer.parseInt(txtLineSize.getText());
		}catch (Exception ex) {
			
			cdLineSize.show();
			return true;
		}
		
		return false;
	}
	
	private void createContents(boolean canEdit) {
		
		setLayout(new GridLayout(3, false));
		
		Label l = new Label(this, SWT.NONE);
		l.setText("Line Color:");
		
		btnLineColor = new ColorSelector(this);
		btnLineColor.addListener(e->fireModified(null));
		
		spnLineAlpaha= new Spinner(this, SWT.BORDER);
		spnLineAlpaha.setMinimum(0);
		spnLineAlpaha.setMaximum(100);
		spnLineAlpaha.setIncrement(10);
		spnLineAlpaha.setToolTipText("Transparency value between 0 (transparent) and 100 (opaque)");
		
		l = new Label(this, SWT.NONE);
		l.setText("Line Size:");
		
		txtLineSize = new Text(this, SWT.BORDER);
		txtLineSize.addListener(SWT.Modify, e->fireModified(e));
		txtLineSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		if (canEdit) {
			cdLineSize = new ControlDecoration(txtLineSize, SWT.LEFT | SWT.TOP);
			cdLineSize.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			cdLineSize.setShowHover(true);
			cdLineSize.setDescriptionText("Invalid value for line size. Value must be an integer.");
			cdLineSize.hide();
		}
		
		lfill = new Label(this, SWT.NONE);
		lfill.setText("Fill Color:");
			
		btnFillColor = new ColorSelector(this);
		btnFillColor.addListener(e->fireModified(null));
		
		spnFillAlpaha= new Spinner(this, SWT.BORDER);
		spnFillAlpaha.setMinimum(0);
		spnFillAlpaha.setMaximum(100);
		spnFillAlpaha.setIncrement(10);
		spnFillAlpaha.setToolTipText("Transparency value between 0 (transparent) and 100 (opaque)");
		
		//get listeners for disabling 
		btnLineColorl = btnLineColor.getButton().getListeners(SWT.Selection);
		btnFillColorl = btnFillColor.getButton().getListeners(SWT.Selection);
		
	}
	
	private void fireModified(Event e) {
		for (Listener l : this.getListeners(SWT.Modify)) l.handleEvent(e);
	}
	
	/**
	 * initialize composite values
	 * @param style
	 */
	public void initValues(AttributeGeometryStyle style) {
		btnLineColor.setColorValue( toRGB(style.getLineColor() )) ;
		spnLineAlpaha.setSelection( style.getLineAlpha());
		txtLineSize.setText(String.valueOf(style.getLineSize()));
		if (type == AttributeType.POLYGON) {
			btnFillColor.setColorValue( toRGB(style.getFillColor()));
			spnFillAlpaha.setSelection(style.getFillAlpha());
		}
	}
	
	private RGB toRGB(Color c) {
		return new RGB(c.getRed(), c.getGreen(), c.getBlue());
	}
	/**
	 * Returns the selected geometry style
	 */
	public AttributeGeometryStyle getValue() {
		Attribute.AttributeType type = Attribute.AttributeType.LINE;
		if (btnFillColor.getButton().isVisible()) {
			type = Attribute.AttributeType.POLYGON;
		}
		AttributeGeometryStyle newstyle = new AttributeGeometryStyle(type);
		newstyle.setLineSize(Integer.valueOf(txtLineSize.getText()));
		
		RGB rgb = btnLineColor.getColorValue();
		newstyle.setLineColor(new Color(rgb.red, rgb.green, rgb.blue));
		newstyle.setLineAlpha(spnLineAlpaha.getSelection());
		if (type == Attribute.AttributeType.POLYGON) {
			rgb = btnFillColor.getColorValue();
			newstyle.setFillColor(new Color(rgb.red, rgb.green, rgb.blue));
			newstyle.setFillAlpha(spnFillAlpaha.getSelection());
		}
		return newstyle;
	}
	
	
}
