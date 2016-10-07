package org.wcs.smart.query.common.ui;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.query.common.model.QueryGridResultItem;
import org.wcs.smart.query.model.filter.DateFilter;

public class GriddedTableResultsPage  extends EditorPart  {

	
	private GriddedEditor parentEditor;
	private GriddedTableContent content;
	private FormToolkit toolkit;
	
	/**
	 * Creates new editor page
	 * @param parent
	 */
	public GriddedTableResultsPage(GriddedEditor parent) {
		this.parentEditor = parent;
	}

	/**
	 * @return the query results table
	 */
	public QueryResultsTable getQueryResultsTable(){
		return content.getQueryResultsTable();
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

	@Override
	public void dispose(){
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
	}
	
	public void validate(){
		content.validate();
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
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		parent.setLayout(layout);
		content = new GriddedTableContent(parent, parentEditor, toolkit);
	}
	
	/**
	 * Updates the table with the new results and ensure
	 * it is displayed.
	 * 
	 * @param results
	 */
	public void updateAndShowTable(Collection<QueryGridResultItem> results, 
			IProgressMonitor monitor){
		content.setTableData(results, monitor);
	}
	
	
	
	public GriddedTableContent getContent() {
		return content;
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
	
	public void updateQueryName(){
		content.setQueryName(parentEditor.getQueryInternal());
	}
}
