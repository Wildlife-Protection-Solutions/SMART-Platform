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
package org.wcs.smart.er.ui.surveydesign.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Survey Design Summary Editor Page
 * @author elitvin
 * @since 3.0.0
 */
public class SurveyDesignSummaryEditorPage extends EditorPart {

	private Form form;
	private Text txtName;
	private Text txtStartDate;
	private Text txtEndDate;
	private Text txtStatus;
	private Text txtKey;
	private Text txtDescription;
	
	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	
	private SurveyDesignEditor parentEditor;
	
	public SurveyDesignSummaryEditorPage(SurveyDesignEditor parent) {
		this.parentEditor = parent;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		toolkit.setBorderStyle(SWT.BORDER);
		Composite container = toolkit.createComposite(parent, SWT.NONE);

		toolkit.paintBordersFor(container);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		form = toolkit.createForm(container);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		form.getBody().setLayout(new GridLayout(1, true));

		ScrolledForm main = toolkit.createScrolledForm(form.getBody());
		main.getBody().setLayout(new GridLayout(1, true));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite content = toolkit.createComposite(main.getBody(), SWT.NONE);
		GridLayout leftLayout = new GridLayout(3, false);
		leftLayout.verticalSpacing = 10;
		content.setLayout(leftLayout);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)content.getLayout()).marginRight = 10;
	
		toolkit.createLabel(content, "Name:");
		txtName = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtName.setEditable(false);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content); 
		
		toolkit.createLabel(content, "Start Date:");
		txtStartDate = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtStartDate.setEditable(false);
		txtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content); 

		toolkit.createLabel(content, "End Date:");
		txtEndDate = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtEndDate.setEditable(false);
		txtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content); 

		toolkit.createLabel(content, "Status:");
		txtStatus = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtStatus.setEditable(false);
		txtStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content); 

		toolkit.createLabel(content, "Key:");
		txtKey = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtKey.setEditable(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(content);
		
		toolkit.createLabel(content, "Description:");
		txtDescription = toolkit.createText(content, "", SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		txtDescription.setEditable(false);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true,true);
		gd.heightHint = 40;
		gd.widthHint = 100;
		txtDescription.setLayoutData(gd);
		Hyperlink lnk = createEditLink(content);
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
	}

	private Hyperlink createEditLink(Composite parent) {
		Hyperlink editLink = toolkit.createHyperlink(parent, DialogConstants.EDIT_LINK_TEXT, SWT.WRAP);
		
//		if (!this.parentEditor.canEdit()) {
//			editLink.setEnabled(false);
//			editLink.setVisible(false);
//		}
//		
//		if (panelType != null){
//			editLink.addHyperlinkListener(new HyperlinkAdapter() {
//				@Override
//				public void linkActivated(HyperlinkEvent e) {
//					showEditDialog(panelType);
//				}
//			});
//		}
		return editLink;
	}

	@Override
	public void setFocus() {
		txtName.setFocus();
	}

	@Override
	public void dispose(){
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void doSaveAs() {
		// not allowed
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
