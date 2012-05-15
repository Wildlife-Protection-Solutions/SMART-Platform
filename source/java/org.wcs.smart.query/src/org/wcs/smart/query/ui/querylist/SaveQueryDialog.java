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
package org.wcs.smart.query.ui.querylist;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TextCellEditor;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.QueryFolderTreeComposite;

/**
 * Dialog for saving queries.  Asks for the folder
 * location and optionally the name.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SaveQueryDialog  extends TitleAreaDialog {

	private Text txtName;
	private boolean includeName = false;
	
	private QueryFolder selectedQuery = null;
	private String queryName = null;

	/**
	 * @param parent the parent shell
	 * @param query the query to update
	 * @param includeName if the dialog should include a location
	 * for users to modify the query name
	 */
	public SaveQueryDialog(Shell parent, Query query, boolean includeName) {
		super(parent);
		this.includeName = includeName;
		this.queryName = "Copy of " + query.getName();
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, "Save",
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		btnOk.setEnabled(false);
	}

	
	/**
	 * @return the selected query folder
	 */
	public QueryFolder getQueryFolder(){
		return selectedQuery;
	}
	/**
	 * @return the selected query name
	 */
	public String getQueryName(){
		return this.queryName;
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		getShell().setText("Save Query");
		if (this.includeName){
			setMessage("Select the new query name and location to save query.");
		}else{
			setMessage("Select location to save the query:");
		}
		
		Composite main = new Composite(parent, SWT.NONE);
		
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (this.includeName){
			 Label lbl = new Label(main, SWT.NONE);
			 lbl.setText("Query Name:" );
			 
			 txtName = new Text(main, SWT.BORDER);
			 txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			 txtName.setText(queryName);
			 txtName.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent e) {
					queryName = txtName.getText();
				}
			});
			 
			 lbl = new Label(main, SWT.NONE);
			 lbl.setText("Save Location:" );
			 lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		}
		
		QueryFolderTreeComposite treeComp = new QueryFolderTreeComposite(main);
		treeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		treeComp.addSelectionListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				if (!event.getSelection().isEmpty()){
					SaveQueryDialog.this.getButton(IDialogConstants.OK_ID).setEnabled(true);
					selectedQuery = (QueryFolder) ((IStructuredSelection)event.getSelection()).getFirstElement();					
				}else{
					SaveQueryDialog.this.getButton(IDialogConstants.OK_ID).setEnabled(false);
					selectedQuery = null;
				}
			}
		});
		
		return main;
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
