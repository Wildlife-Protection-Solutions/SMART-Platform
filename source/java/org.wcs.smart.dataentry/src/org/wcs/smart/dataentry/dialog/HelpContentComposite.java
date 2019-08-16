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
package org.wcs.smart.dataentry.dialog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.dataentry.dialog.composite.AbstractInfoComposite.IModelChangedListener;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttribute.HelpImageLocation;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for collecting help info for configurable model
 * attributes.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class HelpContentComposite extends Composite{

	private Text txtText;
	private ComboViewer cmbImageLoc;
	private Canvas lblImg;
	
	private CmAttribute attribute;
	
	private Image imgCache;
	private Path imgPath;
	
	private IModelChangedListener listener;
	
	public HelpContentComposite(Composite parent, IModelChangedListener listener) {
		super(parent, SWT.NONE);
		this.listener = listener;
		createContent();
		this.attribute = null;
	}
	
	private void createContent() {
		setLayout(new GridLayout(1, false));
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		SmartUiUtils.createHeaderLabel(this, Messages.HelpContentComposite_ImageLblSection);
		
		Composite temp = new Composite(this, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lblImg = new Canvas(temp, SWT.BORDER);
		lblImg.setLayoutData(new GridData(SWT.TOP, SWT.FILL, false, false));
		((GridData)lblImg.getLayoutData()).widthHint = 150;
		((GridData)lblImg.getLayoutData()).heightHint = 150;
		
		lblImg.addListener(SWT.Dispose, e->{
			if (imgCache != null) {
				imgCache.dispose();
				imgCache = null;
			}
		});
		lblImg.addListener(SWT.Paint, e->{
			if (imgPath == null) {
				String text = Messages.HelpContentComposite_NoImageText;
				Point size = e.gc.textExtent(text);
				e.gc.drawText(text, (lblImg.getBounds().width - size.x) / 2, (lblImg.getBounds().height / 2)- size.y);
				return;
			}
			if (imgCache != null) {
				e.gc.drawImage(imgCache, 0, 0);
				return;
			}
			String text = Messages.HelpContentComposite_FormatNotSupportedText;
			Point size = e.gc.textExtent(text);
			e.gc.drawText(text, (lblImg.getBounds().width - size.x) / 2, (lblImg.getBounds().height / 2)- size.y);
		});
		
		Composite btnComp = new Composite(temp, SWT.NONE);
		btnComp.setLayout(new GridLayout());
		((GridLayout)btnComp.getLayout()).marginWidth = 0;
		((GridLayout)btnComp.getLayout()).marginHeight = 0;
		btnComp.setLayoutData(new GridData(SWT.TOP, SWT.FILL, false, false));
		
		cmbImageLoc = new ComboViewer(btnComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbImageLoc.setContentProvider(ArrayContentProvider.getInstance());
		cmbImageLoc.setInput(CmAttribute.HelpImageLocation.values());
		cmbImageLoc.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				switch(((CmAttribute.HelpImageLocation)element)) {
				case BEFORE: return Messages.HelpContentComposite_DisplayBeforeTextOp;
				case AFTER: return Messages.HelpContentComposite_DisplayAfterOp;
				}
				return super.getText(element);
			}
		});
		cmbImageLoc.setSelection(new StructuredSelection(CmAttribute.HelpImageLocation.BEFORE));
		cmbImageLoc.addSelectionChangedListener(e->{
			if (attribute == null) return;
			attribute.setHelpImageLocation((HelpImageLocation) cmbImageLoc.getStructuredSelection().getFirstElement());
			listener.modelChanged();
		});
		
		Button btnSelect = new Button(btnComp, SWT.NONE);
		btnSelect.setText(Messages.HelpContentComposite_SelectImageBtn);
		btnSelect.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnSelect.addListener(SWT.Selection, e->selectImageFile());
		btnSelect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnClear = new Button(btnComp, SWT.NONE);
		btnClear.setText(Messages.HelpContentComposite_ClearImageBtn);
		btnClear.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnClear.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnClear.addListener(SWT.Selection, e->{
			if (attribute != null) {
				attribute.setImportHelpFile(null);
				attribute.setHelpFormat(null);
			}
			refreshImage();
			listener.modelChanged();
		});
		
		SmartUiUtils.createHeaderLabel(this, Messages.HelpContentComposite_TextSectionHeader);

		txtText = new Text(this, SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		txtText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		txtText.addListener(SWT.Modify, e->{
			if (attribute == null) return;
			attribute.setHelpText(txtText.getText());
			listener.modelChanged();
		});
		
	}

	public void setAttribute(CmAttribute attribute) {
		this.attribute = attribute;
		if (imgCache != null) {
			imgCache.dispose();
			imgCache = null;
		}
		
		if (attribute != null) {
			txtText.setText(attribute.getHelpText() == null ? "" : attribute.getHelpText()); //$NON-NLS-1$
			if (attribute.getHelpImageLocation() != null) {
				cmbImageLoc.setSelection(new StructuredSelection(attribute.getHelpImageLocation()));
			}
			
		}else {
			txtText.setText(""); //$NON-NLS-1$
			cmbImageLoc.setSelection(new StructuredSelection(CmAttribute.HelpImageLocation.BEFORE));
		}
		refreshImage();
		
	}
	
	private void refreshImage() {
		if (imgCache != null) {
			imgCache.dispose();
			imgCache = null;
		}
		imgPath = null;
		if (attribute != null) {
			if (attribute.getImportHelpFile() != null) {
				imgPath = attribute.getImportHelpFile();
			}else {
				imgPath = attribute.getHelpImage();
			}
			if (imgPath != null) {
				imgCache = SmartUtils.getImage(imgPath, 150);	
			}
		}
		
		getDisplay().asyncExec(()->lblImg.redraw());
	}
	private void selectImageFile() {
		FileDialog fd = new FileDialog(getShell());
		String imageextensions = "*.png;*.jpeg;*.jpg;*.svg"; //$NON-NLS-1$
		fd.setFilterExtensions(new String[] {imageextensions, "*.*"}); //$NON-NLS-1$
		fd.setFilterNames(new String[] {MessageFormat.format(Messages.HelpContentComposite_ImageFilesOp, imageextensions), Messages.HelpContentComposite_AllFilesOp});
		String filename = fd.open();
		if (filename == null) return;
		
		Path p = Paths.get(filename);
		attribute.setImportHelpFile(p);
		refreshImage();
		listener.modelChanged();
	}
}
