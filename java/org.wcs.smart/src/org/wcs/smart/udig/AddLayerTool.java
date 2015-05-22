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
package org.wcs.smart.udig;

import org.eclipse.swt.widgets.Display;
import org.locationtech.udig.project.ui.internal.wizard.MapImport;
import org.locationtech.udig.project.ui.tool.AbstractActionTool;

/**
 * Add layer tool for map.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AddLayerTool extends AbstractActionTool {
	
	public static final String ID = "org.wcs.smart.udig.AddLayer"; //$NON-NLS-1$
	
	public AddLayerTool() {
	}

	@Override
	public void run() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				MapImport mapImport = new MapImport();
				mapImport.setLayerPosition(0);
				mapImport.getDialog().open();
			}
		});

	}

	@Override
	public void dispose() {

	}

}
