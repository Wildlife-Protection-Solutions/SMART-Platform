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
package org.wcs.smart.query.ui;

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.ui.SourceProvider.QueryPartPanelType;
import org.wcs.smart.query.ui.definition.GriddedQueryDefinitionComposite;
import org.wcs.smart.query.ui.definition.ObservationQueryDefinitionComposite;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.definition.QueryDefinitionComposite;
import org.wcs.smart.query.ui.definition.SummaryQueryDefinitionComposite;
import org.wcs.smart.query.ui.queryfilter.AbstractQueryItemPanel;
import org.wcs.smart.query.ui.queryfilter.GriddedFilterPanel;
import org.wcs.smart.query.ui.queryfilter.QueryFilterPanel;
import org.wcs.smart.query.ui.queryfilter.SummaryFilterPanel;

/**
 * Class for managing query ui elements.
 * 
 * @author Emily
 *
 */
public class QueryLayoutManager {

	private static QueryLayoutManager instance = null;
	
	/*
	 * This is a set of panels that appear in the query filter view on 
	 * the right-hand side.
	 */
	private AbstractQueryItemPanel[] panels = new AbstractQueryItemPanel[]{
			new QueryFilterPanel(),
			new SummaryFilterPanel(),
			new GriddedFilterPanel()
	};
	
	/*
	 * Creates a new instance
	 */
	private QueryLayoutManager(){	
		// need to refresh query item panels when data model changes
		DataModelManager.getInstance().addChangeListener(new IDataModelListener() {
			@Override
			public void modified() {
				QueryDataModelManager.getInstance().clearDataModel();
				for (AbstractQueryItemPanel pnl : panels){
					pnl.refreshPanel();
				}
			}
		});
	}
	
	/**
	 * 
	 * @return the default instance
	 */
	public static QueryLayoutManager getInstance(){
		if (instance == null){
			instance = new QueryLayoutManager();
		}
		return instance;
	}
	
	/**
	 * 
	 * @param panelType the type of panel to get
	 * @param parent the parent element
	 * @return the query item panel to display based on the panel type
	 */
	public AbstractQueryItemPanel getFilterPanel(QueryPartPanelType panelType){
		for (AbstractQueryItemPanel panel : panels){
			if (panel.getValidType() == panelType){
				return panel;
			}
		}
		
		return null;
	}
	
	/**
	 * Creates the query definition composite for a given query type.
	 * 
	 * @param queryType the query type
	 * @param parent the query definition
	 * @param view parent querydefview
	 * @return
	 */
	public QueryDefinitionComposite createComposite(QueryType queryType, Composite parent, QueryDefView view){
		if (queryType == QueryType.OBSERVATION || queryType == QueryType.PATROL){
			return new ObservationQueryDefinitionComposite(parent, view);
		}else if (queryType == QueryType.GRIDDED){
			return new GriddedQueryDefinitionComposite(parent, view);
		}else if (queryType == QueryType.SUMMARY){
			return new SummaryQueryDefinitionComposite(parent, view);
		}
		return null;
	}
	
}
