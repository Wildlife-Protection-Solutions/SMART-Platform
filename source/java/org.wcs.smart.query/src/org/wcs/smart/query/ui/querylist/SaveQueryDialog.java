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

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.QueryFolderTreeComposite;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for saving queries.  Asks for the folder
 * location and optionally the name.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SaveQueryDialog  extends SmartStyledTitleDialog {

	private Text txtName;
	private boolean includeName = false;
	
	private QueryFolder selectedQuery = null;
	private String queryName = null;
	private QueryFolderTreeComposite treeComp;

	/**
	 * @param parent the parent shell
	 * @param query the query to update
	 * @param includeName if the dialog should include a location
	 * for users to modify the query name
	 */
	public SaveQueryDialog(Shell parent, Query query, boolean includeName) {
		super(parent);
		this.includeName = includeName;
		this.queryName = query.getName();
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,
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
		parent = (Composite) super.createDialogArea(parent);
		
		getShell().setText(Messages.SaveQueryDialog_DialogTitle);
		setTitle(Messages.SaveQueryDialog_PageTitle);
		if (this.includeName){
			setMessage(Messages.SaveQueryDialog_NewDialogMessage);
		}else{
			setMessage(MessageFormat.format(Messages.SaveQueryDialog_DialogMessage, new Object[]{ queryName }));
		}
		
		Composite main = new Composite(parent, SWT.NONE);
		
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (this.includeName){
			 Label lbl = new Label(main, SWT.NONE);
			 lbl.setText(Messages.SaveQueryDialog_NameLabel );
			 
			 txtName = new Text(main, SWT.BORDER);
			 txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			 txtName.setText(queryName);
			 txtName.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					queryName = txtName.getText();
					validate();
				}
			});
			 
			 lbl = new Label(main, SWT.NONE);
			 lbl.setText(Messages.SaveQueryDialog_LocationLabel );
			 lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		}
		
		treeComp = new QueryFolderTreeComposite(main,QueryHibernateManager.getInstance().canModifyCaQueries());
		treeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		treeComp.addSelectionListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				if (!event.getSelection().isEmpty()){
					selectedQuery = (QueryFolder) ((IStructuredSelection)event.getSelection()).getFirstElement();					
				}else{
					selectedQuery = null;
				}
				validate();
			}
		});
		
		return main;
	}

	private void validate(){
		boolean ok = true;
		setErrorMessage(null);
		if (queryName.trim().length() == 0){
			setErrorMessage(Messages.SaveQueryDialog_InvalidName);
			ok = false;
		}
		if(treeComp.getSelection().isEmpty()){
			ok = false;
		}
		SaveQueryDialog.this.getButton(IDialogConstants.OK_ID).setEnabled(ok);
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
