/*
 * Copyright (C) 2018 Wildlife Conservation Society
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
package org.wcs.smart.imageprocessor.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.imageprocessor.ImageResizeProcessor;
import org.wcs.smart.imageprocessor.internal.Messages;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog for selecting which images to resize and the output 
 * options.
 * 
 * @author Emily
 *
 */
public class ResizeAttachmentDialog extends SmartStyledTitleDialog {

	private enum Size{
		S640 ("640x480", 640, 480), //$NON-NLS-1$
		S1280 ("1280x960", 1280, 960), //$NON-NLS-1$
		S1600 ("1600x1200", 1600, 1200), //$NON-NLS-1$
		S2048 ("2048x1536", 2048, 1536), //$NON-NLS-1$
		S2560 ("2560x1920", 2560, 1920), //$NON-NLS-1$
		S2816 ("2816x2112", 2816, 2112), //$NON-NLS-1$
		S3264 ("3264x2468", 3264, 2468), //$NON-NLS-1$
		S4200 ("4200x2800", 4200, 2800), //$NON-NLS-1$
		CUSTOM (Messages.ResizeAttachmentDialog_CustomLabel, 0, 0);
		
		public String gui;
		public int width;
		public int height;
		Size(String gui, int width, int height){
			this.gui = gui;
			this.width = width;
			this.height = height;
		}
	}
	
	private CheckboxTableViewer lstTypes;
	private Text txtMaxSize;
	private ComboViewer cmbNewSize;
	private Text txtWidth;
	private Text txtHeight;
	
	public ResizeAttachmentDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
				
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));

		SmartUiUtils.createHeaderLabel(main, Messages.ResizeAttachmentDialog_typesLabel);
		Composite c = new Composite(main, SWT.NONE);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		c.setLayout(new GridLayout());
		
		Label l = new Label(c, SWT.WRAP);
		l.setText(Messages.ResizeAttachmentDialog_typesmessage);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lstTypes = CheckboxTableViewer.newCheckList(c, SWT.MULTI | SWT.BORDER);
		lstTypes.setContentProvider(ArrayContentProvider.getInstance());
		lstTypes.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IWaypointSource)element).getName(Locale.getDefault());
			}
		});
		lstTypes.setInput(WaypointSourceEngine.INSTANCE.getSupportedSources());
		lstTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lstTypes.addCheckStateListener(e->validate());
		lstTypes.setAllChecked(true);
		
		SmartUiUtils.createHeaderLabel(main, Messages.ResizeAttachmentDialog_MaxSize);
		c = new Composite(main, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(c, SWT.WRAP);
		l.setText(Messages.ResizeAttachmentDialog_MaxSizeMessage);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite temp = new Composite(c, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		
		txtMaxSize = new Text(temp, SWT.BORDER);
		txtMaxSize.setText("0"); //$NON-NLS-1$
		txtMaxSize.addModifyListener(e->validate());
		txtMaxSize.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		((GridData)txtMaxSize.getLayoutData()).widthHint = 75;
		
		l = new Label(temp, SWT.NONE);
		l.setText("MB"); //$NON-NLS-1$
		
		SmartUiUtils.createHeaderLabel(main, Messages.ResizeAttachmentDialog_NewSize);
		c = new Composite(main, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(c, SWT.WRAP);
		l.setText(Messages.ResizeAttachmentDialog_NewSizeMessage);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		cmbNewSize = new ComboViewer(c, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbNewSize.setContentProvider(ArrayContentProvider.getInstance());
		cmbNewSize.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 6, 1));
		cmbNewSize.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return  ((Size)element).gui;
			}
		});
		cmbNewSize.setInput(Size.values());
		cmbNewSize.setSelection(new StructuredSelection(Size.S640));
		
		temp = new Composite(c, SWT.NONE);
		temp.setLayout(new GridLayout(6, false));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		
		List<Control> cts = new ArrayList<>();
		
		l = new Label(temp, SWT.NONE);
		l.setText(Messages.ResizeAttachmentDialog_Width);
		cts.add(l);
		
		txtWidth = new Text(temp, SWT.BORDER);
		txtWidth.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWidth.addModifyListener(e->validate());
		cts.add(txtWidth);
		
		l = new Label(temp, SWT.NONE);
		l.setText("px"); //$NON-NLS-1$
		cts.add(l);
		
		l = new Label(temp, SWT.NONE);
		l.setText(Messages.ResizeAttachmentDialog_Height);
		cts.add(l);
		
		txtHeight = new Text(temp, SWT.BORDER);
		txtHeight.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtHeight.addModifyListener(e->validate());
		cts.add(txtHeight);
		
		l = new Label(temp, SWT.NONE);
		l.setText("px"); //$NON-NLS-1$
		cts.add(l);
		
		
		
		
		cts.forEach(e->e.setEnabled(false));
		
		cmbNewSize.addSelectionChangedListener(e->{
			Object x = cmbNewSize.getStructuredSelection().getFirstElement();
			boolean enabled=false;
			if (x == Size.CUSTOM) {
				enabled = true;
			}
			final boolean value = enabled;
			cts.forEach(ob->ob.setEnabled(value));	
			validate();
		});
		
		
		setTitle(Messages.ResizeAttachmentDialog_Title);
		getShell().setText(Messages.ResizeAttachmentDialog_Title);
		setMessage(Messages.ResizeAttachmentDialog_Message);
		return composite;
	}
	
	@Override
	public void okPressed() {
		List<IWaypointSource> srcs = new ArrayList<>();
		for (Object o :	lstTypes.getCheckedElements()) {
			srcs.add((IWaypointSource)o);
		}
		
		double maxSize = Double.parseDouble( txtMaxSize.getText() );
		int width = 0;
		int height = 0;
		Size ss = (Size) cmbNewSize.getStructuredSelection().getFirstElement();
		if (ss == Size.CUSTOM) {
			width = Integer.parseInt( txtWidth.getText() );
			height = Integer.parseInt( txtHeight.getText() );
		}else {
			width = ss.width;
			height = ss.height;
		}
	
		ImageResizeProcessor pp = new ImageResizeProcessor(srcs, maxSize, width, height);
		ProcessingStatusDialog dialog = new ProcessingStatusDialog(getShell(), pp);
		dialog.open();
		
		super.close();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private void validate() {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (ok != null) ok.setEnabled(false);
		
		if (lstTypes.getCheckedElements().length == 0) {
			setErrorMessage(Messages.ResizeAttachmentDialog_OneTypeRequired);
			return;
		}
		
		if (!validatePositiveDbl(txtMaxSize.getText())) {
			setErrorMessage(Messages.ResizeAttachmentDialog_InvalidMaxSize);
			return;
		}
		
		if (cmbNewSize.getStructuredSelection().getFirstElement() == Size.CUSTOM) {
			if (!validatePositiveInt(txtWidth.getText())) {
				setErrorMessage(Messages.ResizeAttachmentDialog_InvalidWidth);
				return;
			}
			if (!validatePositiveInt(txtHeight.getText())) {
				setErrorMessage(Messages.ResizeAttachmentDialog_InvalidHeight);
				return;
			}
		}
		setErrorMessage(null);
		if (ok != null) ok.setEnabled(true);
	}
	
	private boolean validatePositiveInt(String text) {
		if (text.trim().isEmpty()) return false;
		try {
			Integer ii = Integer.parseInt(text);
			if (ii < 0) {
				return false;
			}
			return true;
		}catch (Exception ex) {
			return false;
		}
	}
	
	private boolean validatePositiveDbl(String text) {
		if (text.trim().isEmpty()) return false;
		try {
			Double ii = Double.parseDouble(text);
			if (ii < 0) {
				return false;
			}
			return true;
		}catch (Exception ex) {
			return false;
		}
	}
}
	