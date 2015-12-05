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
package org.wcs.smart.query.ui.importexport;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.QueryFolderTreeComposite;

/**
 * Query wizard page to select the query import folder location.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportQueryFolderPage extends WizardPage {

	public static final String PAGENAME = "ImportLocation"; //$NON-NLS-1$
	
	private QueryFolderTreeComposite folderTree;

	/**
	 * Creates a new query wizard page.
	 */
	protected ImportQueryFolderPage() {
		super(PAGENAME);
	}

	/**
	 * Initializes the values in the query wizard
	 */
	public void initValues(){
		setPageComplete(false);
		
	}
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ImportQueryFolderPage_FolderLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		folderTree= new QueryFolderTreeComposite(main,QueryHibernateManager.getInstance().canModifyCaQueries());
		folderTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		((GridData)folderTree.getLayoutData()).heightHint = 200;
		folderTree.addSelectionListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setPageComplete(!event.getSelection().isEmpty());
			}
		});
		setTitle(Messages.ImportQueryFolderPage_PageTitle);
		setMessage(Messages.ImportQueryFolderPage_PageMessage);
		setPageComplete(false);
		setControl(main);
	}

	/**
	 * @return the selected query folder
	 */
	public QueryFolder getFolder(){
		return (QueryFolder)((IStructuredSelection)folderTree.getSelection()).getFirstElement();
	}
	
	@Override
	public IWizardPage getNextPage() {
		return null;
	}

}
