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

import java.util.ArrayList;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.ui.querytable.column.QueryTableColumn;
import org.wcs.smart.query.ui.querytable.column.QueryTableViewerColumn;

/**
 * Dialog box for modifying waypoint query information include the
 * name and output columns.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryPropertiesDialog extends TitleAreaDialog {

	private WaypointQuery query;
	private CheckboxTableViewer columnViewer;
	private Text txtName;
	
	private QueryTableViewerColumn[] allColumns;
	/**
	 * @param parent
	 * @param title
	 */
	protected QueryPropertiesDialog(Shell parent, WaypointQuery query, QueryTableViewerColumn[] allCollumns) {
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

	public void setChangesMade(boolean changesMade){
		getButton(IDialogConstants.OK_ID).setEnabled(changesMade);
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		getShell().setText("Query Properties");
		setMessage("Select the query properties.");
		
		Composite main = new Composite(parent, SWT.NONE);
		
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lblName = new Label(main, SWT.NONE);
		lblName.setText("Query Name:");
		
		txtName = new Text(main, SWT.BORDER);
		txtName.setText(query.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
			}
		});
		
		Label lblTableColumns = new Label(main, SWT.NONE);
		lblTableColumns.setText("Output Columns:");
		lblTableColumns.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		createColumnTable(main);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd.heightHint = 200;
		columnViewer.getTable().setLayoutData(gd);
		
		
		return main;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	
	protected boolean performSave() {
		
		query.setName(txtName.getText());
		ArrayList<QueryTableColumn> columns = new ArrayList<QueryTableColumn> ();
		for (int i = 0; i < columnViewer.getCheckedElements().length; i ++){
			columns.add( ((QueryTableViewerColumn)columnViewer.getCheckedElements()[i]).getColumn()  );
		}
		query.setTableColumns(columns);
		setChangesMade(false);
		return true;
	}

	
	private void createColumnTable(Composite parent){
		columnViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		
		columnViewer.setContentProvider(ArrayContentProvider.getInstance());
		columnViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof QueryTableViewerColumn){
					return ((QueryTableViewerColumn)element).getColumn().getName();
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
		
		
		if (query.getTableColumns().size() == 0){
			columnViewer.setAllChecked(true);
		}else{
			for (int i = 0; i < allColumns.length; i ++){
				if (query.getTableColumns().contains(allColumns[i].getColumn())){
					columnViewer.setChecked(allColumns[i], true);
				}
			}
		}
		
		columnViewer.getTable().addKeyListener(new KeyListener(){

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == ' '){
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
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
