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
package org.wcs.smart.common.control;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.internal.Messages;

/**
 * Composite that provides controls for color selection.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class ColorSelector extends Composite {

	private Color color;
	private Label colorLabel;
	
	private List<IColorSelectionChangeListener> colorChangeListers = new ArrayList<>();

	public ColorSelector(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// Use a label full of spaces to show the color
		colorLabel = new Label(this, SWT.NONE);
		colorLabel.setText("               "); //$NON-NLS-1$

		Button button = new Button(this, SWT.PUSH);
		button.setText(Messages.ColorSelector_Button_Color);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				// Create the color-change dialog
				ColorDialog dlg = new ColorDialog(getShell());
				if (color != null) {
					dlg.setRGB(color.getRGB());
				}
				dlg.setText(Messages.ColorSelector_Dialog_Title);
				RGB rgb = dlg.open();
				if (rgb != null) {
					if (color != null) {
						color.dispose();
					}
					color = new Color(Display.getCurrent(), rgb);
					colorLabel.setBackground(color);
					fireColorSelectionChanged();
				}
			}
		});
	}

	public Color getColor() {
		return color;
	}
	
	public void setColor(Color color) {
		this.color = color;
		colorLabel.setBackground(this.color);
	}
	
	public void addColorSelectionChangeListener(IColorSelectionChangeListener listener) {
		colorChangeListers.add(listener);
	}

	protected void fireColorSelectionChanged() {
		for (IColorSelectionChangeListener l : colorChangeListers) {
			l.colorSelectionChanged(color);
		}
	}
	
	/**
	 * Listener for color selection change events.
	 * 
	 * @author elitvin
	 * @since 6.0.0
	 *
	 */
	public static interface IColorSelectionChangeListener {
		public void colorSelectionChanged(Color color);
	}
}
