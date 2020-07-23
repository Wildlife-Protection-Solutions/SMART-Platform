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
package org.wcs.smart.query.common.ui.image;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.query.common.engine.IDesktopPagedImageResultSet;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.ui.SmartShellDialog;

/**
 * Shell for displaying attachment tooltip 
 * @author Emily
 *
 */
public class AttachmentTooltipShell extends SmartShellDialog{

	private IDesktopPagedImageResultSet results;
	private IAttachmentResultItem data;
	
	public AttachmentTooltipShell(Shell parentShell, IDesktopPagedImageResultSet results, IAttachmentResultItem data) {
		super(parentShell, SWT.RESIZE);
		this.results = results;
		this.data = data;
	}
	
	@Override
	public void createContents(Composite parent) {
		parent.setLayout(new GridLayout());
		
		//configure background color
		Color bgColor = new Color(parent.getDisplay(), 255, 255, 225);
		parent.addListener(SWT.Dispose, e->bgColor.dispose());
				
		List<Control> items = new ArrayList<Control>();
		items.add(parent);
		while(!items.isEmpty()){
			Control c = items.remove(0);
			c.setBackground(bgColor);
			if (c instanceof Composite){
				for (Control kid : ((Composite) c).getChildren()){
					items.add(kid);
				}
			}
		}	
		results.createTooltip(data, parent);
		
		parent.layout(true, true);
//		int height = parent.computeSize(400, SWT.DEFAULT).y;
//		if (height > 500) height = 500;
		shell.setSize(400, 250);
	}

}
