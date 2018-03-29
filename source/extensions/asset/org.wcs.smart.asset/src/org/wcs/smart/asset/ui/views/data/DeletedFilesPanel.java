/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.internal.Messages;

/**
 * Displays a table for files names
 * 
 * @author Emily
 *
 */
public class DeletedFilesPanel {

	private DataImportPage view;
	
	private TableViewer tblResults;
	private Composite item;
	
	public DeletedFilesPanel(Composite parent, DataImportPage view, FormToolkit toolkit) {
		this.view = view;
		createComposite(parent,  toolkit);
	}
	
	public Control getControl() {
		return item;
	}
	
	public void addSelectionChangedListener(ISelectionChangedListener l) {
		tblResults.addSelectionChangedListener(l);
	}
	
	public IStructuredSelection getSelection() {
		return tblResults.getStructuredSelection();
	}
	
	private void createComposite(Composite parent, FormToolkit toolkit) {
		item = toolkit.createComposite(parent);
		item.setLayout(new GridLayout());
		
		tblResults = new TableViewer(item, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblResults.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblResults.getTable().setHeaderVisible(false);
		tblResults.getTable().setLinesVisible(false);
		tblResults.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof FileProxy) return ((FileProxy) element).getFile().getFileName().toString();
				return super.getText(element);
			}
		});
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		tblResults.setInput(view.getDeletedItems());
		tblResults.refresh();
		
		Menu mnu = new Menu(tblResults.getControl());
		MenuItem restore = new MenuItem(mnu, SWT.PUSH);
		restore.setText(Messages.DeletedFilesPanel_RestoreMenuOption);
		restore.addListener(SWT.Selection, e->{
			List<FileProxy> filesToAdd = new ArrayList<>();
			for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
				FileProxy type = (FileProxy) iterator.next();
				filesToAdd.add(type);
			}
			
			view.restoreFiles(filesToAdd);
		});
		tblResults.getControl().setMenu(mnu);
	}
	
	public void refresh() {
		tblResults.refresh();
	}
		
}
