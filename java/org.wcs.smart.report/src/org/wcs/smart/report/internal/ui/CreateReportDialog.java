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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.query.ui.querylist.MultiFocusCellOwnerDrawHighlighter;
import org.wcs.smart.report.IReportListener;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportEventManager.EventType;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.manger.ReportManager;
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
	private Object initItem = null;
	private String reportName = null;
	private TreeViewer reportList;
	private boolean includeName;
	private Button btnNewFolder;
	private IReportListener folderListener = new IReportListener() {
		
		@Override
		public void reportEvent(final Object o, EventType eventType) {
			if (eventType == ReportEventManager.EventType.FOLDER_ADDED){
				reportFolderAdded((ReportFolder)o);
			}else if (eventType == ReportEventManager.EventType.FOLDER_UPDATED){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						ReportFolder rf = (ReportFolder)o;
						reportList.refresh(rf.getParentFolder());
					}
				});
				
			}
		}
	};
	
	/**
	 * @param parent
	 *            the parent shell
	 * @param rootFolder the default selected folder
	 * @param defaultName the initial name of the report; can be null
	 * @param includeName <code>true</code> if name should be included in dialog box
	 */
	public CreateReportDialog(Shell parent, 
			Object rootFolder,
			String defaultName, boolean includeName) {
		
		super(parent);
		this.initItem = rootFolder;
		this.includeName = includeName;
		if (defaultName != null){
			this.reportName = defaultName;
		}else{
			this.reportName = Messages.CreateReportDialog_DefaultReportName;
		}
		
		ReportEventManager.getInstance().addReportListener(folderListener);
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
	
	@Override
	public boolean close(){
		ReportEventManager.getInstance().removeReportListener(folderListener);
		return super.close();
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
		reportList.setContentProvider(new LazyReportContentProvider(ReportManager.canModifyCaReports() ? RootType.ALL : RootType.USER_ONLY));
		reportList.setLabelProvider(new ReportLabelProvider());		
		reportList.setInput(Messages.CreateReportDialog_LoadingLabel);
		reportList.setCellEditors(new CellEditor[] { new TextCellEditor(reportList.getTree()) });
		reportList.setColumnProperties(new String[] { "col1" }); //$NON-NLS-1$
		reportList.setCellModifier(new ReportItemNameCellEditor(true));
		
		new TreeViewerFocusCellManager
				(reportList, new MultiFocusCellOwnerDrawHighlighter(reportList));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(
				reportList) {
			
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
						return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION ||
								event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		TreeViewerEditor.create(reportList, actSupport, ColumnViewerEditor.DEFAULT);
		
		btnNewFolder = new Button(main, SWT.PUSH);
		btnNewFolder.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 2, 1));
		btnNewFolder.setText(Messages.CreateReportDialog_NewFolderButton);
		btnNewFolder.setEnabled(false);
		btnNewFolder.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				(new NewFolderHandler()).execute(new StructuredSelection(selectedItem));
			}
		});
		
		reportList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedItem = ((IStructuredSelection) reportList
						.getSelection()).getFirstElement();

				validate();	
			}
		});
		
		((GridData)reportList.getTree().getLayoutData()).heightHint = 300;

		if (initItem instanceof RootReportFolder){
			reportList.setSelection(new StructuredSelection(initItem));
			reportList.expandToLevel(2);
		}else if (initItem instanceof ReportFolder){
			List<Object> folders = new ArrayList<Object>();
			ReportFolder tmp = (ReportFolder) initItem;

			tmp = tmp.getParentFolder();
			while(tmp != null){
				folders.add(tmp);
				tmp = tmp.getParentFolder();
			}
			if (((ReportFolder) initItem).getEmployee() != null){
				folders.add(RootReportFolder.USER_ROOT_FOLDER);
			}else{
				folders.add(RootReportFolder.CA_ROOT_FOLDER);
			}
			Collections.reverse(folders);
			TreePath p = new TreePath(folders.toArray());
			((LazyReportContentProvider)reportList.getContentProvider()).setInitialExpandedPath(new TreePath[]{p}, new StructuredSelection(initItem));
			
			reportList.expandToLevel(2);
			reportList.refresh();
			
			
//			reportList.setInput(Messages.CreateReportDialog_LoadingLabel);
//			reportList.refresh(folders.get(0));
		}
		
		return main;
	}

	private void reportFolderAdded(final ReportFolder rf){
		getShell().getDisplay().syncExec(new Runnable(){

			@Override
			public void run() {
				if (rf.getParentFolder() != null){
					reportList.refresh(rf.getParentFolder());
				}else if (rf.getDeletedParent() !=null){
					reportList.refresh(rf.getDeletedParent());
				}else if (rf.getEmployee() == null){
					reportList.refresh(RootReportFolder.CA_ROOT_FOLDER);
				}else{
					reportList.refresh(RootReportFolder.USER_ROOT_FOLDER);
				}
				
				reportList.expandToLevel(rf, 0);
				
				IJobChangeListener listener = new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						((LazyReportContentProvider)reportList.getContentProvider()).removeUpdateCompleteListener(this);
						reportList.editElement(rf, 0);
					}
				};
				((LazyReportContentProvider)reportList.getContentProvider()).addUpdateCompleteListener(listener);
			}});		
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
			btnNewFolder.setEnabled(false);
		}else{
			if (selectedItem.getClass() != ReportFolder.class && 
					selectedItem.getClass() != RootReportFolder.class){
				ok = false;
				btnNewFolder.setEnabled(false);
			}else{
				btnNewFolder.setEnabled(true);
			}
		}
		if (getButton(IDialogConstants.OK_ID) != null){
			getButton(IDialogConstants.OK_ID).setEnabled(ok);
		}
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
