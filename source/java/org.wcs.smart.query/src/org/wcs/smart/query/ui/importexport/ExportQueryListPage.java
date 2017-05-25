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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Wizard page to display a list of queries to export. For
 * exporting query definitions
 * 
 * @author Emily
 *
 */
public class ExportQueryListPage extends WizardPage {

	public static final String PAGE_NAME = "QueryListPage"; //$NON-NLS-1$
	
	private TableViewer queryList;
	private List<Object> queries;
	
	/**
	 * Creates a new query wizard page.
	 */
	protected ExportQueryListPage() {
		super(PAGE_NAME);
		this.queries = new ArrayList<Object>();
	}

	/**
	 * Initializes the values in the query wizard
	 */
	public void initValues(List<QueryEditorInput> initqueries){
		if ( ((ExportQueryWizard)getWizard()).getQuery() != null){
			if (!queries.contains(((ExportQueryWizard)getWizard()).getQuery())){
				queries.add(((ExportQueryWizard)getWizard()).getQuery());
			}
		}
		if (initqueries != null){
			queries.addAll(initqueries);
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
				add();
			}
		});
		
		Button btnRemove = new Button(buttons, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				remove();
			}
			
		});
		
		Menu menu = new Menu(queryList.getControl());
		queryList.getControl().setMenu(menu);
		MenuItem add = new MenuItem(menu,SWT.DEFAULT);
		add.setText(DialogConstants.ADD_BUTTON_TEXT);
		add.addListener(SWT.Selection, e->add());
		
		MenuItem remove = new MenuItem(menu,SWT.DEFAULT);
		remove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		remove.addListener(SWT.Selection, e->remove());
		menu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				remove.setEnabled(!queryList.getSelection().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		
		setTitle(Messages.ExportQueryListPage_PageTitle);
		setMessage(Messages.ExportQueryListPage_PageMessage);
		setPageComplete(false);
		setControl(main);
	}
	
	private void add(){
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
	private void remove(){
		IStructuredSelection sel = (IStructuredSelection)queryList.getSelection();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			queries.remove(object);
		}
		queryList.refresh();
		validate();
	}
	
	private void validate(){
		String error = null;
		if (queries.size() == 0){
			error = Messages.ExportQueryListPage_QueryRequired;
		}
		setErrorMessage(error);
		setPageComplete(error == null);
	}
	
	/**
	 * @return the selected file
	 */
	public List<Object> getQueries(){
		return this.queries;
	}
	
}
