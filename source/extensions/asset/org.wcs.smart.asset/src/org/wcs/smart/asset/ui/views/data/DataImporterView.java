/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.data;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;

/**
 * Asset data importer view
 * 
 * @author Emily
 *
 */
public class DataImporterView extends EditorPart{

	public static final String ID = "org.wcs.smart.asset.ui.views.data.importer"; //$NON-NLS-1$
	
	private boolean isDirty;
	private IEclipseContext context;
	
	private FormToolkit toolkit;
	
	private DataImportPage importPage;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	
	
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		context = (IEclipseContext) getSite().getService(IEclipseContext.class);		
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	public void setDirty(boolean isDirty) {
		boolean fire = isDirty != this.isDirty;
		this.isDirty = isDirty;
		if (fire) firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private void zeroMargins(Composite c) {
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form mainform = toolkit.createForm(parent);
		mainform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainform.getBody().setLayout(new GridLayout());
		zeroMargins(mainform.getBody());
		
		Composite header = toolkit.createComposite(mainform.getBody());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		header.setLayout(new GridLayout());
		zeroMargins(header);
		
		Composite topPanel = toolkit.createComposite(header);
		topPanel.setLayout(new GridLayout(3, false));
//		zeroMargins(topPanel);
		
		FontData fd = mainform.getFont().getFontData()[0];
		fd.setStyle(SWT.NORMAL);
		
		Font normalFont = new Font(topPanel.getDisplay(), fd);
		
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(topPanel.getDisplay(), fd);
		
		topPanel.addListener(SWT.Dispose, e->{
			normalFont.dispose();
			boldFont.dispose();
		});
		
		Hyperlink lnkImport = toolkit.createHyperlink(topPanel, "Import Data Files", SWT.NONE);
		lnkImport.setFont(normalFont);
		
		Hyperlink lnkReview = toolkit.createHyperlink(topPanel, "Review Imported Files", SWT.NONE);
		lnkReview.setFont(normalFont);
		
		Composite body = toolkit.createComposite(header);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		body.setLayout(new GridLayout());
		//zeroMargins(body);
		
		importPage = new DataImportPage(this, toolkit);
		
		lnkImport.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				for (Control c : body.getChildren()) c.dispose();
				importPage.createPage(body);
				body.layout(true, true);
				
				lnkImport.setFont(boldFont);
				lnkReview.setFont(normalFont);
				lnkImport.getParent().layout();
			}
		});
		
		lnkReview.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				for (Control c : body.getChildren()) c.dispose();
				createReview(body);
				body.layout(true, true);
				
				lnkImport.setFont(normalFont);
				lnkReview.setFont(boldFont);
				lnkImport.getParent().layout();
			}
		});
		
		createReview(body);
		lnkImport.setFont(normalFont);
		lnkReview.setFont(boldFont);
	}
	
	private void createReview(Composite body) {
		Composite c = toolkit.createComposite(body);
		c.setLayout(new GridLayout());
		toolkit.createLabel(c, "REVIEW SECTION");
	}
	
	
	
	
	@Override
	public void setFocus() {
	}

	
	public IEclipseContext getContext() {
		return this.context;
	}
	
	
	
}
