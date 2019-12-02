/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui.run;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;

/**
 * PAWS Editor results page
 * 
 * @author Emily
 *
 */
public class RunTableResultsPage extends EditorPart {
	
	private static final int LOAD_SIZE = 50;
	
	private TableViewer tblViewer;
	private Composite part;
	
	private List<String[]> data = new ArrayList<>(LOAD_SIZE);
	private int startIndex = -1;
	private int endIndex = -1;
	
	
	public RunTableResultsPage(RunEditor parent) {
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		Label temp = new Label(part, SWT.NONE);
		temp.setText("No Results");
	}

	
	public void refresh(final PawsResultManager results) {
		for (Control c : part.getChildren()) c.dispose();
		
		if (!Files.exists(results.getResultsFile())) {
			Label l = new Label(part, SWT.WRAP);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			if (results.getRun().getStatus() == PawsRun.Status.ERROR) {
				l.setText("Error occurred during run.");
			}else if (results.getRun().getStatus() == PawsRun.Status.COMPLETE) {
				l.setText("Results not found in local filestore.");
			}else {
				l.setText("Results not available until run complete.");
			}
		}else {		
			tblViewer = new TableViewer(part, SWT.VIRTUAL | SWT.FULL_SELECTION);
			tblViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			tblViewer.setContentProvider(new ILazyContentProvider() {			
				@Override
				public void updateElement(int index) {
					int dIndex = index - (index / LOAD_SIZE) * LOAD_SIZE;
					if (index >= startIndex && index <= endIndex) {
						if (!tblViewer.isBusy()) tblViewer.replace(data.get(dIndex), index);
					}else {
						data.clear();
						startIndex = (index / LOAD_SIZE) * LOAD_SIZE;
						endIndex = startIndex + LOAD_SIZE - 1;
						try {
							results.getData(startIndex+1, LOAD_SIZE, data);
						}catch (Exception ex) {
							PawsPlugIn.displayLog(ex.getMessage(), ex);
						}
						
						
						if (!tblViewer.isBusy()) tblViewer.replace(data.get(dIndex), index);
					}
				}
			});
			tblViewer.getTable().setHeaderVisible(true);
			tblViewer.getTable().setLinesVisible(true);
			
			String[] headers = results.getHeaders();
			for (int i = 0; i < headers.length; i ++) {
				String h = headers[i];
				TableViewerColumn tvc = new TableViewerColumn(tblViewer, SWT.NONE);
				tvc.getColumn().setText(h);
				tvc.getColumn().setWidth(200);
				final int colindex = i;
				tvc.setLabelProvider(new ColumnLabelProvider() {
					public String getText(Object element) {
						return ((String[])element)[colindex];
					}
				});
			}
			tblViewer.setItemCount(results.getNumRows());
			
		}
		
	}
	@Override
	public void setFocus() {
		part.setFocus();
	}

}
