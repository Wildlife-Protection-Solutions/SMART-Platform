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
package org.wcs.smart.patrol.query.ui.editor;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.common.ui.QueryResultsTable;
import org.wcs.smart.query.model.filter.DateFilter;


/**
 * This file contains the table results section
 * of a patrol query editor.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolQueryTableResultsPage extends EditorPart  {

	private PatrolQueryResultsEditor parentEditor;
	private PatrolQueryEditorTableContent content ;
	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	
	/**
	 * Creates new editor page
	 * @param parent
	 */
	public PatrolQueryTableResultsPage(PatrolQueryResultsEditor parent) {
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
	
	@Override
	public void dispose(){
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
	}
	
	public void setQuery(){
		content.initValues(parentEditor.getQueryInternal());
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
		return this.content.getDateFilter();
	}
	
	/**
	 * validates date filter
	 */
	public void validate(){
		this.content.validate();
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		parent.setLayout(layout);
		content = new PatrolQueryEditorTableContent(parent, parentEditor, toolkit);
	}
		
	
	
	/**
	 * Updates the table with the new results and ensure
	 * it is displayed.
	 * 
	 * @param results
	 */
	public void updateAndShowTable(Collection<PatrolQueryResultItem> results){
		content.setTableData(results);
	}
	
	/**
	 * Displays the progress bar
	 */
	public void showProgressArea(){
		content.showProgressArea();
	}
	
	/**
	 * @return a progress monitor that updates the progress area
	 */
	public IProgressMonitor createProgressMonitor(){
		return content.createProgressMonitor();
	}
	
	@Override
	public void setFocus() {
		content.setFocus();
	}


	public QueryResultsTable getQueryResultsTable() {
		return content.getQueryResultsTable();
	}
	
	public void updateQueryName(){
		content.updateName(parentEditor.getQueryInternal());
	}
}

