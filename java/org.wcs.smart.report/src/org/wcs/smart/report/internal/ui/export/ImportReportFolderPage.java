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
package org.wcs.smart.report.internal.ui.export;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.report.ui.LazyReportContentProvider;
import org.wcs.smart.report.ui.LazyReportContentProvider.RootType;
import org.wcs.smart.report.ui.ReportLabelProvider;

/**
 * Query wizard page to select the query import folder location.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportReportFolderPage extends WizardPage {

	public static final String PAGENAME = "ImportLocation"; //$NON-NLS-1$
	
	private TreeViewer reportList;

	/**
	 * Creates a new query wizard page.
	 */
	protected ImportReportFolderPage() {
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
		lbl.setText(Messages.ImportReportFolderPage_FolderLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		reportList = new TreeViewer(main, SWT.SINGLE | SWT.BORDER);
		reportList.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		reportList.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		reportList.setContentProvider(new LazyReportContentProvider(
				(SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER || SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN) ? RootType.ALL : RootType.USER_ONLY));
		reportList.setLabelProvider(new ReportLabelProvider());
		reportList.setInput(Messages.CreateReportDialog_LoadingLabel);
		((GridData)reportList.getTree().getLayoutData()).heightHint = 300;

		reportList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				if (!sel.isEmpty() && (sel.getFirstElement() instanceof ReportFolder || sel.getFirstElement() instanceof RootReportFolder)){
					setPageComplete(true);
					return;
				}
				setPageComplete(false);
			}
		});
		
		setTitle(Messages.ImportReportFolderPage_Title);
		setMessage(Messages.ImportReportFolderPage_Message);
		setPageComplete(false);
		setControl(main);
	}

	/**
	 * @return the selected query folder
	 */
	public Object getFolder(){
		return ((IStructuredSelection)reportList.getSelection()).getFirstElement();
	}
	
	@Override
	public IWizardPage getNextPage() {
		return null;
	}

}
