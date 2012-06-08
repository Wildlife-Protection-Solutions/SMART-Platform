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
package org.wcs.smart.query.ui.patrol;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.parser.internal.filter.DateFilter;

/**
 * This file contains the table results section
 * of a patrol query editor.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolQueryTableResultsPage extends EditorPart  {

	
	
	private PatrolQueryEditor parentEditor;

	
	/**
	 * Creates new editor page
	 * @param parent
	 */
	public PatrolQueryTableResultsPage(PatrolQueryEditor parent) {
		this.parentEditor = parent;
	}

	
	/**
	 * Does nothing.
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void doSaveAs() {
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
		
		
	}

	public void setQuery(){
		//TODO: init the table/header information
		//To get the query being set use parentEditor.getQuery()
	}
	
	
	/**
	 * @return <code>false</code>
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		return false;
	}

	/** 
	 * @return <code>false</code>
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	
	/**
	 * @return the date filter associated with the query
	 */
	public DateFilter getDateFilter(){
		//TODO: return the date from the QueryDateFilterComposite you will add to this page
		return null;
	}
	
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		//TODO: fill me in using the QueryEditorTableContent as an example
		Label lblDoMe = new Label(parent, SWT.NONE);
		lblDoMe.setText("Copy the code in the QueryResultTablePage and QueryEditorTableContent");
		
		
	}
	
	/**
	 * Updates the table with the new results and ensure
	 * it is displayed.
	 * 
	 * @param results
	 */
	public void updateAndShowTable(List<QueryResultItem> results, 
			IProgressMonitor monitor){
		//TODO:  update the results table and show it
	}
	
	/**
	 * Displays the progress bar
	 */
	public void showProgressArea(){
		//TODO: show the progress area
	}
	
	/**
	 * @return a progress monitor that updates the progress area
	 */
	public IProgressMonitor createProgressMonitor(){
		//TODO: create a progress monitor that writes data to the 
		//progress area.  See QueryEditorTableContent
		return new NullProgressMonitor();
	}
	
	@Override
	public void setFocus() {
	}
}

