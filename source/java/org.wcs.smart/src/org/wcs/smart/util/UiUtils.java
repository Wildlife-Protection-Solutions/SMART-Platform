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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class UiUtils {
	
	private static Color dialogColor ;
	
	static {
		dialogColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	}
	
	
	
	public static void colorDialog(Shell shell) {
		List<Composite> comps = new ArrayList<>();
		comps.add(shell);
		
		shell.setBackground(dialogColor);
		
		while(!comps.isEmpty()) {
			Composite item = comps.remove(0);
			item.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			for (Control kid : item.getChildren()) {
				
				if (WidgetElement.getCSSClass(kid) != null) {
					//this has a specific style; skip it
					continue;
				}
				if (kid instanceof Composite) comps.add((Composite)kid);
				if (kid instanceof Label) kid.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				if (kid instanceof Text) kid.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				if (kid instanceof Link) kid.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				if (kid instanceof Hyperlink) kid.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				if (kid instanceof Button) {
					if ((kid.getStyle() & SWT.CHECK) == SWT.CHECK || (kid.getStyle() & SWT.RADIO) == SWT.RADIO) kid.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}
			}
			
			if (item instanceof TabFolder) {
				for (TabItem titem : ((TabFolder) item).getItems()) {
					if (titem.getControl() instanceof Composite) comps.add((Composite)titem.getControl());
					titem.getControl().setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					titem.getControl().getParent().setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));			
				}
			}
		}
		
	}
}
