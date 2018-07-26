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
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.internal.Messages;

/**
 * Composite that provides controls for font selection.
 * 
 * @author elitvin
 * @since 6.1.0
 */
public class FontSelector extends Composite {

	private FontData fontData;
	private Label fontLabel;
	
	private List<IFontSelectionChangeListener> fontChangeListers = new ArrayList<>();

	public FontSelector(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		fontLabel = new Label(this, SWT.NONE);

		Button button = new Button(this, SWT.PUSH);
		button.setText(Messages.FontSelector_Button_Select);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				FontDialog dlg = new FontDialog(getShell());
				dlg.setEffectsVisible(false);
				if (fontData != null) {
					dlg.setFontList(new FontData[] {fontData});
				}
				
				if (dlg.open() != null) {
					FontData fd = dlg.getFontList()[0];
					setFontData(fd);
					fireFontSelectionChanged();
				}
			}
		});
	}

	public FontData getFontData() {
		return fontData;
	}
	
	public void setFontData(FontData fontData) {
		this.fontData = fontData;
		String fontText = (fontData != null) ? fontData.getName() + ", " + fontData.getHeight() : ""; //$NON-NLS-1$ //$NON-NLS-2$
		fontLabel.setText(fontText);
		layout(true, true);
	}
	
	public void addFontSelectionChangeListener(IFontSelectionChangeListener listener) {
		fontChangeListers.add(listener);
	}

	protected void fireFontSelectionChanged() {
		for (IFontSelectionChangeListener l : fontChangeListers) {
			l.fontSelectionChanged(fontData);
		}
	}
	
	/**
	 * Listener for font selection change events.
	 * 
	 * @author elitvin
	 * @since 6.1.0
	 *
	 */
	public static interface IFontSelectionChangeListener {
		public void fontSelectionChanged(FontData fontData);
	}
}
