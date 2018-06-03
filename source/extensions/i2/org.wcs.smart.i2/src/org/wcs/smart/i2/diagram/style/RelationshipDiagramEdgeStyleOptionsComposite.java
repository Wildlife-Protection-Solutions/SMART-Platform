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
package org.wcs.smart.i2.diagram.style;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.common.control.ColorSelector;
import org.wcs.smart.common.control.ColorSelector.IColorSelectionChangeListener;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions;

/**
 * Composite containing controls to manage edge style options for relationship diagram.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramEdgeStyleOptionsComposite extends Composite {
	
	private RelationshipDiagramEdgeStyleOptions options;

	private ColorSelector csEdgeColor;
	
	public RelationshipDiagramEdgeStyleOptionsComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}
	
	public void setSourceOptions(RelationshipDiagramEdgeStyleOptions options) {
		this.options = options;
		//TODO: ZZZZZZZZZZZZ update controls
	}

	private void createContent(Composite parent) {
		Label lblEdgeColor = new Label(parent, SWT.NONE);
		lblEdgeColor.setText("Color:");
		
		csEdgeColor = new ColorSelector(parent);
		csEdgeColor.addColorSelectionChangeListener(new IColorSelectionChangeListener() {
			@Override
			public void colorSelectionChanged(Color color) {
				//TODO: ZZZZZZZZ implement
			}
		});
	}
}
