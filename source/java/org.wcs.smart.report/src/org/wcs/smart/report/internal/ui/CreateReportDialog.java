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
package org.wcs.smart.report.internal.ui;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.report.ui.LazyReportContentProvider;
import org.wcs.smart.report.ui.LazyReportContentProvider.RootType;
import org.wcs.smart.report.ui.ReportLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for creating a new report. Prompts
 * the user for the report name, and save location.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CreateReportDialog extends TitleAreaDialog {

	private Text txtName = null;
	private Object selectedItem = null;
	private String reportName = null;
	private TreeViewer reportList;
	private boolean includeName;
	
	/**
	 * @param parent
	 *            the parent shell
	 * @param rootFolder the default selected folder
	 * @param defaultName the initial name of the report; can be null
	 * @param includeName <code>true</code> if name should be included in dialog box
	 */
	//TODO: rootFolder is not selected correctly
	public CreateReportDialog(Shell parent, 
			Object rootFolder,
			String defaultName, boolean includeName) {
		
		super(parent);
		this.selectedItem = rootFolder;
		this.includeName = includeName;
		if (defaultName != null){
			this.reportName = defaultName;
		}else{
			this.reportName = Messages.CreateReportDialog_DefaultReportName;
		}
	}

	/**
	 * Creates a new report dialog.
	 * @param parent
	 * @param rootFolder
	 */
	public CreateReportDialog(Shell parent, 
			Object rootFolder) {
		this(parent, rootFolder, null, true);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		validate();
	}

	/**
	 * @return the selected query folder
	 */
	public Object getReportFolder() {
		return selectedItem;
	}

	/**
	 * @return the selected query name
	 */
	public String getReportName() {
		return this.reportName;
	}

	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		getShell().setText(Messages.CreateReportDialog_Dialog_Title);

		setMessage(Messages.CreateReportDialog_Dialog_Message);
		setTitle(Messages.CreateReportDialog_PageTitle);

		Composite main = new Composite(parent, SWT.NONE);

		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		if (includeName){
			Label lbl = new Label(main, SWT.NONE);
			lbl.setText(Messages.CreateReportDialog_ReportNameLabel);

			txtName = new Text(main, SWT.BORDER);
			txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			txtName.setText(reportName);
			txtName.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					reportName = txtName.getText();
					validate();
				}
			});
		}

		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.CreateReportDialog_SaveLocationLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

		reportList = new TreeViewer(main, SWT.SINGLE | SWT.BORDER);
		reportList.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		reportList.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		reportList.setContentProvider(new LazyReportContentProvider(
				(SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER || SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN) ? RootType.ALL : RootType.USER_ONLY));
		reportList.setLabelProvider(new ReportLabelProvider());
		reportList.setInput(Messages.CreateReportDialog_LoadingLabel);
		reportList.getTree().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedItem = ((IStructuredSelection) reportList
						.getSelection()).getFirstElement();
				validate();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		((GridData)reportList.getTree().getLayoutData()).heightHint = 300;

		if (selectedItem instanceof RootReportFolder){
			reportList.setSelection(new StructuredSelection(selectedItem));
		}else if (selectedItem instanceof ReportFolder){
			//TODO: fix this code somehow
//			ReportFolder folder = ((ReportFolder) selectedItem);
//			Stack<ReportFolder> folders = new Stack<ReportFolder>();
////			Report r = (Report)selectedItem;
//			while(folder != null){
//				folders.push(folder);
//				folder = folder.getParentFolder();
//			}
//			Object[] path = new Object[folders.size()+1];
//			int index = 0;
//			path[index++] = ((ReportFolder)selectedItem).getEmployee() == null ? RootReportFolder.CA_ROOT_FOLDER : RootReportFolder.USER_ROOT_FOLDER; 
//			while(!folders.isEmpty()){
//				Object x = folders.pop();
//				reportList.setExpandedState(x, true);
//			}
////			TreePath p = new TreePath(path);
////			reportList.expandToLevel(p, 1);
//			reportList.setSelection(new StructuredSelection(selectedItem));
		}
		return main;
	}

	/*
	 * Validate the user input
	 */
	private void validate() {
		boolean ok = true;
		setErrorMessage(null);
		if (includeName){
			if (reportName.trim().length() == 0) {
				setErrorMessage(Messages.CreateReportDialog_Error_ReportNameBlank);
				ok = false;
			}
			if (!SmartUtils.isSimpleString(reportName, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Report.MAX_NAME_LENGTH)){
				setErrorMessage(
						MessageFormat.format(
						Messages.CreateReportDialog_Error_InvalidReportName, 
						new Object[]{Report.MAX_NAME_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc} ));
						
				ok = false;
			}
		}
		if (selectedItem == null) {
			ok = false;
		}else{
			if (selectedItem.getClass() != ReportFolder.class && 
					selectedItem.getClass() != RootReportFolder.class){
				ok = false;
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(ok);
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @return <code>true</code>
	 */
	@Override
	public boolean isResizable() {
		return true;
	}
}
