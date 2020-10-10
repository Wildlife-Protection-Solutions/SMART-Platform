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
package org.wcs.smart.udig.style;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.geotools.styling.Style;
import org.locationtech.udig.style.advanced.common.styleattributeclasses.PointSymbolizerWrapper;
import org.locationtech.udig.style.advanced.common.styleattributeclasses.RuleWrapper;
import org.locationtech.udig.style.advanced.points.PointPropertiesEditor;
import org.locationtech.udig.style.advanced.points.widgets.IPointSymbolizerComposite;
import org.wcs.smart.internal.Messages;

/**
 * Point symbolizer that generates theme rules based on dataset column
 * and data model element using the icon associated with the data model element.
 * 
 * @author Emily
 *
 */
public class SmartGenerateIconSymbolizer implements IPointSymbolizerComposite {

	private PointPropertiesEditor editor;
	private Composite composite = null;
	
	public SmartGenerateIconSymbolizer() {
	}

	@Override
	public String getName() {
		return Messages.SmartGenerateIconSymbolizer_Name;
	}

	@Override
	public Composite getComposite() {
		return composite;
	}

	@Override
	public void createComposite(Composite parent, PointPropertiesEditor editor,
			RuleWrapper ruleWrapper,
			String[] numericAttributesArrays) {
		
		this.editor = editor;
		
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		Label l = new Label(composite, SWT.NONE);
		l.setText(Messages.SmartGenerateIconSymbolizer_CCAANotSupported);

	}

	@Override
	public void update(RuleWrapper ruleWrapper) {
		generateIconStyles();
	}

	private void generateIconStyles() {
		GenerateSmartThemeDialog dialog = new GenerateSmartThemeDialog(getComposite().getShell(), editor.getLayer());
		dialog.open();
		Style style = dialog.getStyle();
		if (style != null) {
			editor.updateStyle(style);
		}
	}
	
	@Override
	public boolean canStyle(PointSymbolizerWrapper pointWrapper) {
		return false;
	}

}
