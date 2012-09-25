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
package org.wcs.smart.patrol.xml.in;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for selecting file to import
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportPatrolDialog  extends TitleAreaDialog {

	private List<String> selectedFiles;
	
	private Button btnOpSingle;
	private Button btnOpMuliple;
	
	//single file widgets
	private Text txtFile;
	private Button btnFileBrowse;

	//multi file widgets
	private ListViewer lstFiles;
	private Button btnAdd;
	private Button btnRemove;
	
	private WritableList files = new WritableList();
	
	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 */
	public ImportPatrolDialog(Shell parentShell) {
		super(parentShell);

	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@SuppressWarnings("unchecked")
	protected void buttonPressed(int buttonId) {
		selectedFiles = new ArrayList<String>();
		if (btnOpMuliple.getSelection()){
			selectedFiles.addAll(files);
		}else{
			selectedFiles.add(txtFile.getText());
			
		}
		super.buttonPressed(buttonId);
	}

	/**
	 * @return the filename selected by user or 
	 * a list of files
	 */
	public List<String> getFileNames() {
		return selectedFiles;
	}


	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button b = createButton(parent, IDialogConstants.OK_ID, "Import", true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);

		b.setEnabled(false);
	}

	private void setEnabledState(){
		Control[] compMulti = { lstFiles.getList(), btnAdd, btnRemove };
		Control[] compSingle = { txtFile,  btnFileBrowse};
		for (int i = 0 ; i < compSingle.length; i ++){
			compSingle[i].setEnabled(btnOpSingle.getSelection());
		}
		for (int i = 0 ; i < compMulti.length; i ++){
			compMulti[i].setEnabled(!btnOpSingle.getSelection());
		}
	}
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		btnOpSingle = new Button(main, SWT.RADIO);
		btnOpSingle.setText("Import Single Patrol");
		btnOpSingle.addListener(SWT.Selection, new Listener() {			
			@Override
			public void handleEvent(Event event) {
				if (btnOpSingle.getSelection()){
					if (txtFile.getText().length() > 0) {
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}else{
						getButton(IDialogConstants.OK_ID).setEnabled(false);
					}
				}
				setEnabledState();

			}
		});
		Composite single = new Composite(main, SWT.NONE);
		single.setLayout(new GridLayout(3, false));
		single.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)single.getLayout()).marginLeft = 15;
		
		Label lbl = new Label(single, SWT.NONE);
		lbl.setText("Source File:");
		txtFile = new Text(single, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (txtFile.getText().length() > 0) {
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		btnFileBrowse = new Button(single, SWT.NONE);
		btnFileBrowse.setText("Browse...");
		btnFileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(ImportPatrolDialog.this
						.getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[] { "*.zip", "*.xml", "*.*" });
				fd.setFilterNames(new String[] { "zip (*.zip)", "xml (*.xml)", "All Files (*.*)" });
				
				fd.setFilterPath(txtFile.getText());
				fd.setFileName(txtFile.getText());
				String f = fd.open();
				
				if (f != null) {
					txtFile.setText(f);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
				
			}
		});
		
		btnOpMuliple = new Button(main, SWT.RADIO);
		btnOpMuliple.setText("Import Multiple Patrols");
		btnOpMuliple.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (btnOpMuliple.getSelection()){
					getButton(IDialogConstants.OK_ID).setEnabled(files.size() > 0);
					setEnabledState();
				}
			}
		});
		
		Composite multi = new Composite(main, SWT.NONE);

		multi.setLayout(new GridLayout(3, false));
		multi.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)multi.getLayout()).marginLeft = 15;
		lbl = new Label(multi, SWT.NONE);
		lbl.setText("Files:");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true));
		
		lstFiles = new ListViewer(multi, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		lstFiles.setContentProvider(ArrayContentProvider.getInstance());
		lstFiles.setLabelProvider(new LabelProvider());
		lstFiles.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstFiles.setInput(files);
		
		Composite buttons = new Composite(multi, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginTop = gl.marginBottom = gl.marginHeight = 0;
		buttons.setLayout(gl);		
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, true));
		
		btnRemove = new Button(buttons, SWT.PUSH);
		btnRemove.setText("Remove Files");
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = ((IStructuredSelection)lstFiles.getSelection());
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();
					files.remove(x);
				}
				lstFiles.refresh();
				getButton(IDialogConstants.OK_ID).setEnabled(files.size() > 0);
			}	
		});
		btnAdd = new Button(buttons, SWT.PUSH);
		btnAdd.setText("Add Files...");
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.MULTI | SWT.OPEN );
				
				fd.setFilterExtensions(new String[] { "*.zip;*.xml", "*.zip", "*.xml", "*.*" });
				fd.setFilterNames(new String[] { "Patrol Files (*.zip, *.xml)", "zip (*.zip)", "xml (*.xml)", "All Files (*.*)" });
				
				String x = fd.open();
				if (x != null){
					for (int i = 0; i < fd.getFileNames().length; i ++){
						files.add(  (new File(fd.getFilterPath(),fd.getFileNames()[i])).toString() );
					}
					lstFiles.refresh();
				}
				getButton(IDialogConstants.OK_ID).setEnabled(files.size() > 0);
			}		
		});
		

		txtFile.setEnabled(btnOpSingle.getSelection());
		btnFileBrowse.setEnabled(btnOpSingle.getSelection());
		
		setMessage("Select the patrol data location.");
		setTitle("Import Patrol Data");
		getShell().setText("Import Patrols");
		return composite;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

}
