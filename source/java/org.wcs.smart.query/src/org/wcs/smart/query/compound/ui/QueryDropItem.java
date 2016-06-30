package org.wcs.smart.query.compound.ui;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.ui.QueryDateFilterComposite;
import org.wcs.smart.query.ui.editor.ProgressMonitorWrapper;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.util.UuidUtils;

public class QueryDropItem extends DropItem {

	private UUID query;
	private IQueryType queryType;
	private String label;
	
	private QueryDateFilterComposite dateFilter;
	
	public QueryDropItem(Query query){
		this.query = query.getUuid();
		this.queryType = QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey());
		this.label = query.getName() + " [" + query.getId() + "]";
	}
	
	public QueryDropItem(UUID query, IQueryType queryType, String label){
		this.query = query;
		this.queryType = queryType;
		this.label = label;
	}
	
	public String getLabel(){
		return this.label;
	}
	
	public UUID getQueryUuid(){
		return this.query;
	}
	public DateFilter getDateField(){
		return dateFilter.getDateFilter();
	}
	
	@Override
	public String getText() {
		return queryType.getKey() + ":" + UuidUtils.uuidToString(query);
	}

	@Override
	public String asQueryPart() {
		return getText();
	}

	/**
	 * Accepts a query as the data object
	 * or an array where the first element is
	 * a string representing the query type, the second
	 * is the query uuid, and the third the query label
	 * 
	 * 
	 */
	@Override
	public void initializeData(Object data) {
		if (data instanceof Query){
			this.query = ((Query)data).getUuid();
			this.queryType = QueryTypeManager.INSTANCE.findQueryType(((Query)data).getTypeKey());
			this.label = ((Query)data).getName() + " [" + ((Query)data).getId() + "]";
		}else if (data instanceof Object[]){
			Object[] parts = (Object[])data;
			if (parts[0] instanceof String && parts[1] instanceof UUID){
				this.queryType = QueryTypeManager.INSTANCE.findQueryType((String)parts[0]);
				this.query = (UUID)parts[1];	
				this.label = (String)parts[2];
			}
		}
	}

	@Override
	protected void createComposite(Composite parent) {
		// TODO Auto-generated method stub
		Composite main = new Composite(parent, SWT.NONE);

		main.setLayout(new GridLayout(2, false));
		Label lblName = new Label(main, SWT.NONE);
		lblName.setText(label);
		
		dateFilter = new QueryDateFilterComposite(main, queryType.getDateFilterOptions(),
				IDateFilter.DATE_FILTERS, false);

//		Label lblName = new Label(main, SWT.NONE);
//		lblName.setText(label);
		
		initDrag(dateFilter);
		initDrag(lblName);
		initDrag(main);
	}

	private ProgressMonitorWrapper monitor;
	
	public ProgressMonitorWrapper getMonitor(){
		return this.monitor;
	}
	public void setProgressMonitor(ProgressMonitorWrapper monitor){
		this.monitor = monitor;
	}
}
