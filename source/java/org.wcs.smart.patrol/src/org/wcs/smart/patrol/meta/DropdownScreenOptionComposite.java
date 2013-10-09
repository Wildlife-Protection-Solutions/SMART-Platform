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
package org.wcs.smart.patrol.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.ScreenOption;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.patrol.internal.Messages;

/**
 * Screen option with dropdown as default value selector.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class DropdownScreenOptionComposite extends Composite {
	
	public static final NamedItem EMPTY_DROP_OPTION = new NamedItem() {
		public byte[] getUuid() {
			return null;
		}
		public String getName() {
			return Messages.ScreenOptionComposite_Dropdown_NoValue;
		}
	};

	List<NamedItem> ddInput;
	
	private ScreenOption model;
	
	private Button btnDisplayPage;
	private ComboViewer viewer;

	public DropdownScreenOptionComposite(Composite parent, ScreenOption model, List<? extends NamedItem> cInput) {
		super(parent, SWT.NONE);

		this.model = model;

		ddInput = new ArrayList<NamedItem>();
		ddInput.add(EMPTY_DROP_OPTION);
		ddInput.addAll(cInput);
		
		createControls();
	}

	protected void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group group = new Group(this,  SWT.NONE);
		group.setText(model.getType().getGuiLabel());
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		group.setLayout(new GridLayout(2, false));

		Label label = new Label(group, SWT.NONE);
		label.setText(Messages.ScreenOptionComposite_DisplayPage);
		btnDisplayPage = new Button(group, SWT.CHECK);
		btnDisplayPage.setSelection(model.isVisible());
		btnDisplayPage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.getControl().setEnabled(!btnDisplayPage.getSelection());
			}
		});
		
		label = new Label(group, SWT.NONE);
		label.setText(Messages.ScreenOptionComposite_DefaultValue);
		
		viewer = new ComboViewer(group, SWT.READ_ONLY);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		viewer.getControl().setEnabled(!model.isVisible());
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new NamedItemLabelProvider());
 		viewer.setInput(ddInput);
 		byte[] uuid = model.getUuidValue();
		for (NamedItem item : ddInput) {
			if (uuid == item.getUuid() || Arrays.equals(item.getUuid(), uuid)) {
				viewer.setSelection(new StructuredSelection(item));
				break;
			}
		}
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
 			@Override
 			public void selectionChanged(SelectionChangedEvent event) {
 				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
 				Object obj = selection.getFirstElement();
 				if (obj instanceof UuidItem) {
 					UuidItem i = (UuidItem) obj;
 					model.setUuidValue(i.getUuid());
 	 				//setChangesMade(true);
 				}
 			}
 		});
	}
	
}
