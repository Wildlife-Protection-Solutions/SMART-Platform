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
package org.wcs.smart.i2.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.i2.internal.Messages;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Dialog for editing the WKT of a geometry
 * @author Emily
 *
 */
public class WKTGeometryDialog extends TitleAreaDialog {

	private Text txtGeometry;
	
	private Geometry geometry;
	private Geometry newGeometry;
	private WKTReader parser = new WKTReader();
	
	public WKTGeometryDialog(Shell parentShell, Geometry  geometry) {
		super(parentShell);
		this.geometry = geometry;
	}

	@Override
	protected org.eclipse.swt.graphics.Point getInitialSize() {
		org.eclipse.swt.graphics.Point p = super.getInitialSize();
		return new org.eclipse.swt.graphics.Point(Math.min(p.x, 600), Math.min(400,  p.y));
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		txtGeometry = new Text(parent, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		String text = geometry.toText();
		text = text.replaceAll(", ", ", \n "); //$NON-NLS-1$ //$NON-NLS-2$
		txtGeometry.setText(text);
		
		txtGeometry.addListener(SWT.Modify, e-> validate());
		txtGeometry.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		setTitle(Messages.WKTGeometryDialog_Title);
		setMessage(Messages.WKTGeometryDialog_Message);
		getShell().setText(Messages.WKTGeometryDialog_ShellTitle);
		return parent;
	}
	
	private void validate(){
		String message = null;
		try{
			newGeometry = null;
			newGeometry = parser.read(txtGeometry.getText());
			if (!(newGeometry instanceof Point || newGeometry instanceof Polygon)){
				message = Messages.WKTGeometryDialog_InvalidGeometryType;
			}
		}catch (Exception ex){
			message = Messages.WKTGeometryDialog_InvalidGeometry;
		}
		setErrorMessage(message);
		getButton(IDialogConstants.OK_ID).setEnabled(message == null);
	}
	
	public Geometry getNewGeometry(){
		return this.newGeometry;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}