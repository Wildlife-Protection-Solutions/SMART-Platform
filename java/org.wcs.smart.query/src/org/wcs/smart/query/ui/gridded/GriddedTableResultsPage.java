package org.wcs.smart.query.ui.gridded;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.ui.querytable.QueryResultsTable;

public class GriddedTableResultsPage  extends EditorPart  {

	
	private GriddedEditor parentEditor;
	private GriddedTableContent content;
	
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
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		parent.setLayout(layout);
		content = new GriddedTableContent(parent, parentEditor);
	}
	
	/**
	 * Updates the table with the new results and ensure
	 * it is displayed.
	 * 
	 * @param results
	 */
	public void updateAndShowTable(List<GridResultItem> results, 
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
	}
}
