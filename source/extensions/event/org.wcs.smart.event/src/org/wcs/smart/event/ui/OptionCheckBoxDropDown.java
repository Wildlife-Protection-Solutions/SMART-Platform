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
package org.wcs.smart.event.ui;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ui.CheckBoxDropDown;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;

/**
 * Drop down check box viewer for selecting multiple items from a list
 * 
 * @author Emily
 *
 */
public class OptionCheckBoxDropDown extends CheckBoxDropDown{

	private Object optionValue;
	private Button btnOptionCh;

	
	public OptionCheckBoxDropDown(Composite parent) {
		super(parent);
	}
	
	public void setOptionValue(Object optionValue) {
		this.optionValue = optionValue;
	}
	
	/**
	 * If overwritten Must populate the table value
	 * @return
	 */
	@Override
	protected Shell createPopup(){
		// create shell and table
		Shell popup = new Shell(getShell(),  SWT.NO_TRIM | SWT.ON_TOP);
		popup.setLayout(new GridLayout());
		((GridLayout)popup.getLayout()).marginWidth = 1;
		((GridLayout)popup.getLayout()).marginHeight = 1;
		popup.addListener(SWT.Traverse, e-> {
	    	if (e.detail == SWT.TRAVERSE_ESCAPE) {
	    		e.doit = false;
	    	}
		});
		popup.addListener(SWT.Deactivate, (event)->dropDown(false));
		popup.addListener(SWT.Paint, event->{
			Rectangle bounds = popup.getBounds();
            event.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
            event.gc.setLineWidth(1);
            event.gc.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
		});
		if (optionValue != null) {
			popup.setBackground(popup.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			
			Composite spacer = new Composite(popup, SWT.NONE);
			spacer.setLayout(new GridLayout());
			spacer.setBackground(popup.getBackground());
			
			btnOptionCh = new Button(spacer, SWT.CHECK);
			btnOptionCh.setText(labelProvider.getText(optionValue));
			btnOptionCh.setBackground(popup.getBackground());
			btnOptionCh.addListener(SWT.Selection, e->{
				table.getControl().setEnabled(!btnOptionCh.getSelection());
				checkChanged = true;
			});
			btnOptionCh.setSelection(true);
		}
		
		// create table
		table = CheckboxTableViewer.newCheckList(popup, SWT.V_SCROLL | SWT.MULTI);
		table.getControl().setEnabled(false);
		table.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((CheckboxTableViewer)table).addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				checkChanged = true;				
			}
		});
		table.getControl().addKeyListener(new CheckboxSelectorKeyAdapter((CheckboxTableViewer)table));
		if (labelProvider != null) table.setLabelProvider(labelProvider);
		if (contentProvider != null) table.setContentProvider(contentProvider);
		if (input != null) table.setInput(input);
		popup.pack();
		
		return popup;
	}
	
	@Override
	protected Object[] getCheckedElements(){
		if (btnOptionCh.getSelection()) {
			return new Object[] {optionValue};
		}
		return ((CheckboxTableViewer)table).getCheckedElements();
	}
	@Override
	protected void setCheckedElements(Object[] elements){
		if (optionValue != null) {
			for (Object o : elements) {
				if (o.equals(optionValue)) {
					btnOptionCh.setSelection(true);
					return;		
				}
			}
					
		}
		btnOptionCh.setSelection(false);
		((CheckboxTableViewer)table).setCheckedElements(elements);
	}
	
}
