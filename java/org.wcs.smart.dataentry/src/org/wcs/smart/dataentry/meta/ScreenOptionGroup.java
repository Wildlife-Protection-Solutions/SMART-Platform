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
package org.wcs.smart.dataentry.meta;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ScreenOption;

/**
 * Container for group common logic.
 * Contains 2 labels ("Display page" and "Default value") and one check box.
 * Control for "Default value" label must be provided by descendants.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public abstract class ScreenOptionGroup extends Composite {

	private ScreenOption model;

	private Button btnDisplayPage;
	private Label label1;
	private Label label2;
	private Group group;
	
	/**
	 * @param parent
	 * @param style
	 */
	public ScreenOptionGroup(Composite parent, ScreenOption option, String title) {
		super(parent, SWT.NONE);
		this.model = option;

		GridLayout gd = new GridLayout(1, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		this.setLayout(gd);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		group = new Group(this,  SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		group.setLayout(new GridLayout(2, false));
		group.setText(title);

		label1 = new Label(group, SWT.NONE);
		label1.setText(Messages.ScreenOptionGroup_PageVisible);
		
		btnDisplayPage = new Button(group, SWT.CHECK);
		btnDisplayPage.setSelection(model.isVisible());
		btnDisplayPage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onBtnDisplayPageClick();
			}
		});
		
		label2 = new Label(group, SWT.NONE);
		label2.setText(Messages.ScreenOptionGroup_DefaultValue);
		
		createDefaultControl(group);
	}
	
	public void setDefaultEnabled(boolean enabled){
		label2.setEnabled(enabled);
	}
	
	public void setVisibleEnabled(boolean enabled){
		label1.setEnabled(enabled);
		btnDisplayPage.setEnabled(enabled);
	}
	
	public void setEnabled(boolean enabled){
//		group.setEnabled(enabled);
		label1.setEnabled(enabled);
		label2.setEnabled(enabled);
		btnDisplayPage.setEnabled(enabled);
	}

	protected abstract void createDefaultControl(Group group);
	
	protected abstract void onBtnDisplayPageClick();
	

	public ScreenOption getModel() {
		return model;
	}
	
	public Button getBtnDisplayPage() {
		return btnDisplayPage;
	}
}
