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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Query wizard page to select query files to import.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportQueryFilePage extends WizardPage {
	
	public static final String PAGENAME = "QueryFiles"; //$NON-NLS-1$
	
	private List<File> files = null;
	
	/**
	 * Creates a new query wizard page.
	 */
	protected ImportQueryFilePage() {
		super(PAGENAME);
		files = new ArrayList<File>();
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ImportQueryFilePage_FileLabel1);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		final ListViewer lstFiles = new ListViewer(main, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		lstFiles.setContentProvider(ArrayContentProvider.getInstance());
		lstFiles.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof File){
					return ((File) element).getAbsolutePath();
				}
				return super.getText(element);
			}
		});
		lstFiles.setInput(files);
		lstFiles.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite buttons = new Composite(main, SWT.NONE);
		buttons.setLayout(new GridLayout());
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnAdd = new Button(buttons, SWT.NONE);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(ImportQueryFilePage.this.getShell(), 
						SWT.OPEN | SWT.MULTI);
				
				String[] extensions = new String[]{"*.xml", "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$
				String[] names = new String[]{Messages.ImportQueryFilePage_xmlFilterName, Messages.ImportQueryFilePage_AllFilesFilterName};
				
				fd.setFilterExtensions(extensions);
				fd.setFilterNames(names);
				
				String f = fd.open();
				
				if (f != null) {
					for (String f2 : fd.getFileNames()){
						File newF = new File(fd.getFilterPath(), f2);
						if (!files.contains(newF)){
							files.add(newF);
							lstFiles.refresh();	
						}	
					}	
				}
				setPageComplete(files.size() > 0);
			}
		});
		
		Button btnRemove = new Button(buttons, SWT.NONE);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) lstFiles.getSelection();
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					Object file = (Object) iterator.next();
					if (file instanceof File){
						files.remove(file);
					}
				}
				lstFiles.refresh();
			}
		});
		
		
		setTitle(Messages.ImportQueryFilePage_WizardPageTitle1);
		setMessage(Messages.ImportQueryFilePage_PageMessage1);
		setPageComplete(false);
		setControl(main);
	}

	/**
	 * @return the selected file
	 */
	public List<File> getFiles(){
		return files;
	}
	
	@Override
	public IWizardPage getNextPage() {
		return getWizard().getPage(ImportQueryFolderPage.PAGENAME);
	}
}
