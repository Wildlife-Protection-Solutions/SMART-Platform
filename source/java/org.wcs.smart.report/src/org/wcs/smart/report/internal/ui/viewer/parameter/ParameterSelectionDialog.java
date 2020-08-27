/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.report.internal.ui.viewer.parameter;

import java.net.URL;
import java.text.Collator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.birt.parameter.ISmartBirtParameter;
import org.wcs.smart.birt.parameter.ParameterManager;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog box for selecting a SMART list 
 * for the BIRT parameter
 * 
 * @author Emily
 *
 */
public class ParameterSelectionDialog extends SmartStyledTitleDialog {

	private static final String MODIFY_KEY = "MOD"; //$NON-NLS-1$
	
	private ISmartBirtParameter selection;
	private String name;
	private boolean isRequired;
	
	public ParameterSelectionDialog(Shell parent) {
		super(parent);
	}

	@Override
	public void okPressed() {
		super.okPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(part, SWT.NONE);
		l.setText(Messages.ParameterSelectionDialog_Name);
		
		Text txtName = new Text(part, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addListener(SWT.Modify, e->{
			name = txtName.getText();
			validate();
		});
		txtName.addListener(SWT.KeyDown, e->{
			txtName.setData(MODIFY_KEY, true);
		});
		txtName.setData(MODIFY_KEY, false);
		
		l = new Label(part, SWT.NONE);
		l.setText(Messages.ParameterSelectionDialog_IsRequired);
		
		Button btnRequired = new Button(part, SWT.CHECK);
		btnRequired.addListener(SWT.Selection, e->{
			isRequired = btnRequired.getSelection();
		});
		
		l = new Label(part, SWT.NONE);
		l.setText(Messages.ParameterSelectionDialog_List);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		Composite c = new Composite(part, SWT.NONE);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		c.setLayout(new TableColumnLayout());
		
		TableViewer lstParams = new TableViewer(c);
		lstParams.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn col1 = new TableViewerColumn(lstParams, SWT.NONE);
		col1.setLabelProvider(new ColumnLabelProvider() {
			private HashMap<Object,Image> images = new HashMap<>();
			
			@Override
			public void dispose() {
				images.values().forEach(e->e.dispose());
			}
			
			@Override
			public String getText(Object element) {
				return ((ISmartBirtParameter)element).getName(Locale.getDefault());
			}
			
			@Override
			public Image getImage(Object element) {
				Image i = images.get(element);
				if (i != null) return i;
				
				String im = ParameterManager.INSTANCE.getConfigElement(((ISmartBirtParameter)element)
						.getKey()).getAttribute(ParameterManager.IMAGE_ATTRIBUTE);
				if (im == null) return null;
				try {
					
					i = ImageDescriptor.createFromURL(new URL(im)).createImage();
					images.put(element,i);
					return i;
				}catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
		});
		((TableColumnLayout)c.getLayout()).setColumnData(col1.getColumn(), new ColumnWeightData(1));
		
		lstParams.addSelectionChangedListener(e->{
			if (lstParams.getStructuredSelection().getFirstElement() instanceof ISmartBirtParameter) {
				selection = (ISmartBirtParameter) lstParams.getStructuredSelection().getFirstElement();
				if (!(Boolean)txtName.getData(MODIFY_KEY)) txtName.setText(selection.getName(Locale.getDefault()));
			}
			validate();
		});
		List<ISmartBirtParameter> items = ParameterManager.INSTANCE.getParameters();
		items.sort((a,b)->Collator.getInstance().compare(a.getName(Locale.getDefault()), b.getName(Locale.getDefault())));
		lstParams.setInput(items);
		
		setTitle(Messages.ParameterSelectionDialog_Title);
		setMessage(Messages.ParameterSelectionDialog_Message);
		getShell().setText(Messages.ParameterSelectionDialog_Title);
		return parent;
	}
	
	public boolean isRequired() {
		return isRequired;
	}
	
	public String getName() {
		return name;
	}
	
	public ISmartBirtParameter getSelection(){
		return selection;
	}
	
	private void validate() {
		getButton(IDialogConstants.OK_ID).setEnabled(selection != null && name != null && !name.isEmpty());
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
}
