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
package org.wcs.smart.query.ui;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.ui.querytable.QueryTableColumn;

/**
 * Dialog box for modifying query information.  This includes the query
 * name and the columns in the query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryPropertiesDialog extends TitleAreaDialog {

	private WaypointQuery query;
	private CheckboxTableViewer columnViewer;
	private Text txtName;
	
	private QueryTableColumn[] allColumns;
	

	/**
	 * @param parent the parent shell
	 * @param query the query to update
	 * @param allCollumns all columns available to the query
	 */
	protected QueryPropertiesDialog(Shell parent, WaypointQuery query,
			QueryTableColumn[] allCollumns) {
		super(parent);
		this.query = query;
		this.allColumns = allCollumns;
	}
	
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (performSave()){
				okPressed();
			}
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			cancelPressed();
		}
	}
	

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		btnOk.setEnabled(false);
	}

	/**
	 * enables or disables the save button based changesMade
	 * @param changesMade 
	 */
	private void setChangesMade(boolean changesMade){
		getButton(IDialogConstants.OK_ID).setEnabled(changesMade);
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		getShell().setText("Query Properties");
		setMessage("Select the query properties.");
		
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginTop = 10;
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lblName = new Label(main, SWT.NONE);
		lblName.setText("Query Name:");
		lblName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtName = new Text(main, SWT.BORDER);
		txtName.setText(query.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
			}
		});
		
		Label lblTableColumns = new Label(main, SWT.NONE);
		lblTableColumns.setText("Output Columns:");
		lblTableColumns.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		createColumnTable(main);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd.heightHint = 200;
		columnViewer.getTable().setLayoutData(gd);
		
		Composite hyperlinkComposite = new Composite(main, SWT.NONE);
		hyperlinkComposite.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1) );
		hyperlinkComposite.setLayout(new GridLayout(3, false));
		
		Link selectAll = new Link(hyperlinkComposite, SWT.NONE);
		selectAll.setText("<a>Select All</a>");
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				columnViewer.setAllChecked(true);
				setChangesMade(true);
			}
		});
		Label lbl = new Label(hyperlinkComposite, SWT.VERTICAL | SWT.SEPARATOR);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		lbl.setLayoutData(gd);
		Link deselectAll = new Link(hyperlinkComposite, SWT.NONE);
		deselectAll.setText("<a>De-Select All</a>");
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				columnViewer.setAllChecked(false);
				setChangesMade(true);
			}
		});
		
		
		return main;
	}

	/**
	 * Updates the query.
	 * 
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	protected boolean performSave() {
		
		if (!query.getName().equals(txtName.getText())){
			query.setName(txtName.getText());
		}
		
		HashSet<String> columns = new HashSet<String> ();
		for (int i = 0; i < columnViewer.getCheckedElements().length; i ++){
			columns.add( ((QueryTableColumn)columnViewer.getCheckedElements()[i]  ).getKey()  );
		}
		query.setVisibleColumns(columns);
		setChangesMade(false);
		return true;
	}

	/*
	 * Creates checkbox table viewer for selecting columns
	 */
	private void createColumnTable(Composite parent){
		columnViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		
		columnViewer.setContentProvider(ArrayContentProvider.getInstance());
		columnViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof QueryTableColumn){
					return ((QueryTableColumn)element).getName();
				}
				return super.getText(element);
			}
		});
		columnViewer.setInput(allColumns);
		columnViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setChangesMade(true);
			}
		});
		
		
		if (query.getVisibleColumns() == null){
			columnViewer.setAllChecked(true);
		}else{
			for (int i = 0; i < allColumns.length; i ++){
				if (query.getVisibleColumns().contains(allColumns[i].getKey())){
					columnViewer.setChecked(allColumns[i], true);
				}
			}
		}
		
		columnViewer.getTable().addKeyListener(new KeyListener(){
			@SuppressWarnings("rawtypes")
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.SPACE){
					boolean value = columnViewer.getChecked(   ((IStructuredSelection)columnViewer.getSelection()).getFirstElement() );
					for (Iterator iterator = ((IStructuredSelection)columnViewer.getSelection()).iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						columnViewer.setChecked(tp, !value);
					}
					e.doit = false;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
		
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @return <code>true</code>
	 */
	@Override
	public boolean isResizable(){
		return true;
	}
}
