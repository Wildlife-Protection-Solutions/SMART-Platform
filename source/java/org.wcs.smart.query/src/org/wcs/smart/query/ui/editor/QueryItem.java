package org.wcs.smart.query.ui.editor;

import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;

public class QueryItem {

	
	private Query query;
	private ProgressMonitorWrapper wrapper;
	private DateFilter dfilter;
	
	private ProgressBar pbar;
	private Label totalWidget;
	

	private Integer totalCnt = null;
	private IQueryType type;
	
	public QueryItem(Query query, IQueryType type){
		this.query = query;
		this.type = type;
	}
	
	public IQueryType getQueryType(){
		return this.type;
	}
	public Query getQuery(){
		return this.query;
	}
	public DateFilter getDateFilter(){
		return this.dfilter;
	}
	public void setDateFilter(DateFilter dfilter){
		this.dfilter = dfilter;
	}
	
	public String getQueryName(){
		return query.getName() + " [" + query.getId() + "]";
	}
	
	public void setProgressBar(ProgressBar pbar){
		this.pbar = pbar;
	}
	private TableEditor tableEditor;
	public void setTableEditor(TableEditor tableEditor){
		this.tableEditor = tableEditor;
	}
	public TableEditor getTableEditor(){
		return this.tableEditor;
	}
	public ProgressBar getProgressBar(){
		return this.pbar;
	}
	
	public void setTotalCnt(int totalCnt){
		this.totalCnt = totalCnt;
	}
	
	public Integer gettotalCnt(){
		return this.totalCnt;
	}
	public Label getTotalWidget() {
		return totalWidget;
	}

	public void setTotalWidget(Label totalWidget) {
		this.totalWidget = totalWidget;
	}
}
