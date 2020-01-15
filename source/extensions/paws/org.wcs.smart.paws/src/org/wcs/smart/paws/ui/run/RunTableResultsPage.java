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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsResultFile;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;

/**
 * PAWS Editor results page
 * 
 * @author Emily
 *
 */
public class RunTableResultsPage extends EditorPart {
	
	private static final int LOAD_SIZE = 50;
	
	private TableViewer tblViewer;
	private ComboViewer fileViewer;
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
		
		if (results == null || results.getResults().isEmpty()) {
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
			Composite temp = new Composite(part, SWT.NONE);
			temp.setLayout(new GridLayout(1, false));
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			fileViewer = new ComboViewer(temp,  SWT.DROP_DOWN | SWT.READ_ONLY);
			fileViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			fileViewer.getControl().setBackground(temp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			fileViewer.setContentProvider(ArrayContentProvider.getInstance());
			fileViewer.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					if (element instanceof PawsResultFile) return ((PawsResultFile)element).getResultsFile().getFileName().toString();
					return super.getText(element);
				}
			});
			
			fileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					// TODO Auto-generated method stub
					Object item = fileViewer.getStructuredSelection().getFirstElement();
					tblViewer.setInput(null);
					tblViewer.setItemCount(0);
					data.clear();
					startIndex = -1;
					endIndex = -1;

					if (item instanceof PawsResultFile) {
						try {
							PawsResultFile data = (PawsResultFile)item;

							for (TableColumn tc : tblViewer.getTable().getColumns()) tc.dispose();
							String[] headers = data.getHeaders();
							for (int i = 0; i < headers.length; i ++) {
								String h = headers[i];
								TableViewerColumn tvc = new TableViewerColumn(tblViewer, SWT.NONE);
								tvc.getColumn().setText(h);
								tvc.getColumn().setWidth(200);
								final int colindex = i;
								tvc.setLabelProvider(new ColumnLabelProvider() {
									public String getText(Object element) {
										if (element == null) return "NULL";
										return ((String[])element)[colindex];
									}
								});
							}
							tblViewer.setItemCount(data.getNumRows());
							tblViewer.setInput(data);
						}catch (Exception ex) {
							PawsPlugIn.displayLog(ex.getMessage(), ex);
						}
					}
				}
			});

			tblViewer = new TableViewer(part, SWT.VIRTUAL | SWT.FULL_SELECTION );
			tblViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			tblViewer.setContentProvider(new ILazyContentProvider() {	
				PawsResultFile input = null;
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					if (newInput instanceof PawsResultFile) {
						input = (PawsResultFile)newInput;
					}else {
						input = null;
					}
					
				}
				@Override
				public void updateElement(int index) {
					if (input == null) {
						tblViewer.replace(null, index);
						return;
					}
					
					int dIndex = index - (index / LOAD_SIZE) * LOAD_SIZE;
					if (index >= startIndex && index <= endIndex) {
						if (!tblViewer.isBusy()) tblViewer.replace(data.get(dIndex), index);
					}else {
						data.clear();
						startIndex = (index / LOAD_SIZE) * LOAD_SIZE;
						endIndex = startIndex + LOAD_SIZE - 1;
						try {
							input.getData(startIndex+1, LOAD_SIZE, data);
						}catch (Exception ex) {
							PawsPlugIn.displayLog(ex.getMessage(), ex);
						}
						
						
						if (!tblViewer.isBusy()) tblViewer.replace(data.get(dIndex), index);
					}
				}
			});
			tblViewer.getTable().setHeaderVisible(true);
			tblViewer.getTable().setLinesVisible(true);
			
			fileViewer.setInput(results.getResults());
			fileViewer.setSelection(new StructuredSelection(results.getResults().get(0)));
		}
		
	}
	@Override
	public void setFocus() {
		part.setFocus();
	}

}
