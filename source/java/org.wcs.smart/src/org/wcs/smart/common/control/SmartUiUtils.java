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
import java.util.Locale;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

/**
 * UI Utilities for SMART
 * 
 * @author Emily
 *
 */
public class SmartUiUtils {

	private static Color dialogColor ;
	
	static {
		dialogColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	}
	
	public static final String HEADER_CLASS = "SMARTSection";  //$NON-NLS-1$
	public static final String SUB_HEADER_CLASS = "SMARTSubSection";  //$NON-NLS-1$
	
	public static void configure(TableComboViewer viewer) {
		//the height for these does not configure nicely on mac so for mac only we adjust the height
		//assumption is viewer layout data is griddata if not we don't do anything
		if (!(viewer.getControl().getLayoutData() instanceof GridData)) return;		
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) { //$NON-NLS-1$ //$NON-NLS-2$
			GridData gd = (GridData) viewer.getControl().getLayoutData();
			gd.heightHint = viewer.getControl().computeSize(SWT.DEFAULT,SWT.DEFAULT).y + 6;
		}
	}
	
	/**
	 * Creates a SMART section header label.
	 * Assumes parent has a grid layout
	 * @param parent
	 * @param text
	 * @return
	 */
	public static Composite createHeaderLabel(Composite parent, String text) {
		return createHeaderLabelInternal(parent, text, HEADER_CLASS);
	}
	/**
	 * Creates a sub header label.  Assumes parent has a grid layout
	 * 
	 * @param parent
	 * @param text
	 * @return
	 */
	public static Composite createSubHeaderLabel(Composite parent, String text) {
		return createHeaderLabelInternal(parent, text, SUB_HEADER_CLASS);
	}
	
	private static Composite createHeaderLabelInternal(Composite parent, String text, String style) {
		Composite sectionheader = new Composite(parent, SWT.NONE);
		sectionheader.setLayout(new GridLayout());
		sectionheader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		WidgetElement.setCSSClass(sectionheader, style);
		Label headerLabel = new Label(sectionheader, SWT.NONE);
		headerLabel.setText(text);
		
		return sectionheader;
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
					if ((kid.getStyle() & SWT.CHECK) == SWT.CHECK || (kid.getStyle() & SWT.RADIO) == SWT.RADIO) {
						if (!SystemUtils.IS_OS_MAC) kid.setBackground(transparent);
					}
				}
			}
			
			if (item instanceof TabFolder) {
				for (TabItem titem : ((TabFolder) item).getItems()) {
					if (titem.getControl() instanceof Composite) comps.add((Composite)titem.getControl());
					if (titem.getControl() != null) {
						titem.getControl().setBackground(transparent);
						titem.getControl().getParent().setBackground(transparent);
					}
				}
			}
		}
	}
	
	/**
	 * Color the shell with the SMART dialog color (white).
	 * @param shell
	 */
	public static void colorDialog(Shell shell) {
		shell.setBackground(dialogColor);
		for (Control kid : shell.getChildren()) {
			if (kid instanceof Composite) makeTransparent((Composite)kid);
		}
	}
}
