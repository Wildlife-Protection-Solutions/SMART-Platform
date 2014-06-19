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
package org.wcs.smart.ct2smart.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class XmlFileComposite extends Composite {
	
	private Text fileName;
	
	public XmlFileComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		this.setLayoutData(gridData);

		Label label = new Label(this, SWT.NONE);
		label.setText("file");
		
	    fileName = new Text(this, SWT.BORDER);
	    fileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	    fileName.setToolTipText("The location of a file to load.");

	    Button open = new Button(this, SWT.PUSH);
		open.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false));
		((GridData)open.getLayoutData()).heightHint = 10;
	    open.setText("...");
	    open.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
	    		dlg.setFilterNames(new String[] {"XML file"});
	    		dlg.setFilterExtensions(new String[] {"*.xml"});
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			fileName.setText(fn);
	    		}
	    	}
	    });
	    
	}
	
}
