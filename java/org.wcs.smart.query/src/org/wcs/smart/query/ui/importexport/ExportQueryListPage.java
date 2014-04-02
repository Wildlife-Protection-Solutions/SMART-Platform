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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Wizard page to display a list of queries to export.
 * 
 * @author Emily
 *
 */
public class ExportQueryListPage extends WizardPage {

	public static final String PAGE_NAME = "QueryListPage"; //$NON-NLS-1$
	private static final String LAST_DIR_KEY = "LAST_EXPORT_DIR"; //$NON-NLS-1$
	
	private Text txtFile = null;
	private TableViewer queryList;
	private File selectedDirectory;
	private List<QueryEditorInput> queries;
	
	/**
	 * Creates a new query wizard page.
	 */
	protected ExportQueryListPage() {
		super(PAGE_NAME);
		this.queries = new ArrayList<QueryEditorInput>();
	}

	/**
	 * Initializes the values in the query wizard
	 */
	public void initValues(){
		String location = getWizard().getDialogSettings() != null ? getWizard().getDialogSettings().get(LAST_DIR_KEY) : null;
		if (location == null){
			location = System.getProperty("user.home"); //$NON-NLS-1$
		}
		txtFile.setText( location );
		
		if ( ((ExportQueryWizard)getWizard()).getQuery() != null){
			QueryEditorInput in = new QueryEditorInput(((ExportQueryWizard)getWizard()).getQuery());
			if (!queries.contains(in)){
				queries.add(in);
			}
		}
		queryList.refresh();
		
		validate();
		
	}
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ExportQueryListPage_OutputFolderLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		txtFile = new Text(main, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		txtFile.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});		
		
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText(Messages.ExportQueryLocationPage_BrowseButton);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
				dd.setText(Messages.ExportQueryListPage_DirDialogText);
				dd.setMessage(Messages.ExportQueryListPage_DirDialogMessage);
				if (txtFile.getText().length() > 0) {
					dd.setFilterPath(txtFile.getText());
				}
				String f = dd.open();
				if (f != null) {
					txtFile.setText(f);	
					validate();
				}
			}
		});
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ExportQueryListPage_QueriesLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		queryList = new TableViewer(main, SWT.MULTI | SWT.BORDER);
		queryList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		queryList.setContentProvider(ArrayContentProvider.getInstance());
		queryList.setLabelProvider(new QueryListLabelProvider());
		queryList.setInput(queries);
		
		Composite buttons = new Composite(main, SWT.NONE);
		buttons.setLayout(new GridLayout());
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAdd = new Button(buttons, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				QueryListDialog dialog = new QueryListDialog(getShell());
				if (dialog.open() == QueryListDialog.OK){
					for (QueryEditorInput in : dialog.getSelectedQueries()){
						if (!queries.contains(in)){
							queries.add(in);
						}
					}
					queryList.refresh();
					validate();
				}
			}
		});
		
		Button btnRemove = new Button(buttons, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection)queryList.getSelection();
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					queries.remove(object);
				}
				queryList.refresh();
				validate();
			}
			
		});
		
		setTitle(Messages.ExportQueryListPage_PageTitle);
		setMessage(Messages.ExportQueryListPage_PageMessage);
		setPageComplete(false);
		setControl(main);
	}

	private void validate(){
		String error = null;
		if (queries.size() == 0){
			error = Messages.ExportQueryListPage_QueryRequired;
		}else if (! new File(txtFile.getText()).exists()){
			error = Messages.ExportQueryListPage_ValidOutputDirRequired;
		}
		
		setErrorMessage(error);
		setPageComplete(error == null);
		
	}
	/**
	 * @return the selected file
	 */
	public File getDirectory(){
		return selectedDirectory;
	}
	
	/**
	 * @return the selected file
	 */
	public List<QueryEditorInput> getQueries(){
		return this.queries;
	}
	
	public void performFinish(){
		try{
			selectedDirectory = new File(txtFile.getText());
			getWizard().getDialogSettings().put(LAST_DIR_KEY, selectedDirectory.toString());
		}catch (Exception ex){
			//eatme
		}
		
	}
	
}
