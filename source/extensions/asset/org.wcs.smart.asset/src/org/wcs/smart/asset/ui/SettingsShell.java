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
package org.wcs.smart.asset.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Shell that allows users to select display settings
 * for the asset plugin.  This includes the icon size and
 * page size.
 * 
 * @author Emily
 *
 */
public class SettingsShell  {

	private Shell shell;
	private DataDisplaySettings settings;
	
	public SettingsShell(Display parent) {
		shell = new Shell(parent, SWT.NO_TRIM | SWT.ON_TOP | SWT.BORDER);
	
		settings = DataDisplaySettings.getSettings();
		createContents();
	}
	
	public DataDisplaySettings getSettings() {
		return this.settings;
	}
	
	public Shell getShell() {
		return this.shell;
	}
	
	private void createContents() {
		shell.setLayout(new GridLayout());
		((GridLayout)shell.getLayout()).marginWidth = 0;
		((GridLayout)shell.getLayout()).marginHeight = 0;
		
		Composite outer = new Composite(shell, SWT.BORDER);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite c = new Composite(outer, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label ll = new Label(c, SWT.NONE);
		ll.setText("Icon Size");
		
		int indent = 10;
		
		for (DataDisplaySettings.IconSize iconSize : DataDisplaySettings.IconSize.values()) {
			Button opSmall = new Button(c, SWT.RADIO);
			opSmall.setText(iconSize.getOptionName());
			opSmall.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData)opSmall.getLayoutData()).horizontalIndent = indent;
			opSmall.addListener(SWT.MouseUp, e->{
				settings.setIconSize(iconSize);
				close();
			});
			opSmall.setSelection(settings.getIconSize() == iconSize);
		}

		c = new Composite(outer, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ll = new Label(c, SWT.NONE);
		ll.setText("Page Size");
		
		for (DataDisplaySettings.PageSize pageSize : DataDisplaySettings.PageSize.values()) {
			Button opSmall = new Button(c, SWT.RADIO);
			opSmall.setText(pageSize.getOptionName());
			opSmall.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData)opSmall.getLayoutData()).horizontalIndent = indent;
			opSmall.addListener(SWT.MouseUp, e->{
				settings.setPageSize(pageSize);
				close();
			});
			opSmall.setSelection(settings.getPageSize() == pageSize);
		}
		
		Color backgroundColor = shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		List<Control> items = new ArrayList<>();
		items.add(shell);
		while(!items.isEmpty()) {
			Control kid = items.remove(0);
			kid.setBackground(backgroundColor);
			if (kid instanceof Composite) {
				for (Control kk : ((Composite)kid).getChildren()) {
					items.add(kk);
				}
			}
		}
				
		shell.pack();
		shell.layout(true);
	}
	
	public void show(Control relative) {
		Point p2 = relative.getParent().toDisplay(relative.getLocation());
		shell.setLocation(p2.x - shell.getSize().x + relative.getBounds().width, p2.y + relative.getSize().y);
		shell.open();
		shell.addListener(SWT.Deactivate, evt->{shell.dispose();});
	}
	
	private void close() {
		shell.dispose();
	}
}
