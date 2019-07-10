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
package org.wcs.smart.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class UiUtils {
	
	private static Color dialogColor ;
	
	static {
		dialogColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	}
	
	/**
	 * Assigns a transparent background to the given control
	 * and all of it's children 
	 * @param c
	 */
	public static void makeTransparent(Composite c) {
		List<Composite> comps = new ArrayList<>();
		comps.add(c);
		Color transparent = c.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT);
		while(!comps.isEmpty()) {
			Composite item = comps.remove(0);
			item.setBackground(transparent);
			
			for (Control kid : item.getChildren()) {
				
				if (WidgetElement.getCSSClass(kid) != null) {
					//this has a specific style; skip it
					continue;
				}
				if (kid instanceof Table) continue;	//skip tables
				if (kid instanceof Composite) comps.add((Composite)kid);
				if (kid instanceof Label) kid.setBackground(transparent);
				if (kid instanceof Text) kid.setBackground(transparent);
				if (kid instanceof Link) kid.setBackground(transparent);
				if (kid instanceof Hyperlink) kid.setBackground(transparent);
				if (kid instanceof Button) {
					if ((kid.getStyle() & SWT.CHECK) == SWT.CHECK || (kid.getStyle() & SWT.RADIO) == SWT.RADIO) kid.setBackground(transparent);
				}
			}
			
			if (item instanceof TabFolder) {
				for (TabItem titem : ((TabFolder) item).getItems()) {
					if (titem.getControl() instanceof Composite) comps.add((Composite)titem.getControl());
					titem.getControl().setBackground(transparent);
					titem.getControl().getParent().setBackground(transparent);			
				}
			}
		}
	}
	public static void colorDialog(Shell shell) {
		shell.setBackground(dialogColor);
		for (Control kid : shell.getChildren()) {
			if (kid instanceof Composite) makeTransparent((Composite)kid);
		}
	}
}
