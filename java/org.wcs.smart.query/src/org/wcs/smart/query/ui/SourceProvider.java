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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

/**
 * A source provider that provides two difference sources:
 * <ul>
 * <li> Selected Filters - These are the filters selected (in the QueryFilterView)
 * to add to the current query.</li>
 * <li> Query Valid - true or false if the current query is valid.</li>
 * <li> Query Definition - the current query definition in the querydefview</li>
 * 
 * </ul>
 *  
 * @author Emily
 * @since 1.0.0
 */
public class SourceProvider extends AbstractSourceProvider {

	public static enum QueryPartPanelType{
		FILTER_ITEM, SUMMARY_ITEM, GRIDDED_ITEM
	}
	
	public final static String SELECTED_FILTERS = "org.wcs.smart.query.ui.filteradd"; //$NON-NLS-1$
	public final static String QUERY_VALID = "org.wcs.smart.query.ui.queryvalid"; //$NON-NLS-1$
	public final static String QUERY_ERROR_MESSAGE = "org.wcs.smart.query.ui.queryerrormessage"; //$NON-NLS-1$
	public final static String QUERY_DROP_TYPE = "org.wcs.smart.query.ui.querybuildertype"; //$NON-NLS-1$

	HashMap<String, Object> data = new HashMap<String, Object>();
	
	public SourceProvider(){
		data.put(SELECTED_FILTERS, IEvaluationContext.UNDEFINED_VARIABLE);
		data.put(QUERY_VALID, IEvaluationContext.UNDEFINED_VARIABLE);
		data.put(QUERY_DROP_TYPE, IEvaluationContext.UNDEFINED_VARIABLE);
	}
	/**
	 * @see org.eclipse.ui.ISourceProvider#dispose()
	 */
	@Override
	public void dispose() {
		data = null;
	}
	
	/**
	 * Sets the filter selection.
	 * @param selection a selection of items to add to the current query 
	 */
	public void setFilterSelection(IStructuredSelection selection){
		data.put(SELECTED_FILTERS, selection);		
		fireSourceChanged(ISources.ACTIVE_PART_ID, SELECTED_FILTERS, selection);
	}
	
	public void setQueryDefinitionType(QueryPartPanelType type){
		data.put(QUERY_DROP_TYPE, type);
		fireSourceChanged(ISources.ACTIVE_PART_ID, QUERY_DROP_TYPE, type);
	}
	
	/**
	 * Sets the state of the current query.
	 * 
	 * @param isValid <code>true</code> or <code>false</code> depending on the state of the
	 * @param errorMessage string error message description or null if query is valid
	 * current query.
	 */
	public void setQueryValue(Boolean isValid, String errorMessage){
		data.put(QUERY_VALID, isValid);
		data.put(QUERY_ERROR_MESSAGE, errorMessage);
		fireSourceChanged(ISources.ACTIVE_PART_ID, QUERY_VALID, isValid);
	}
		
	/**
	 * @see org.eclipse.ui.ISourceProvider#getCurrentState()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Map getCurrentState() {
		return data;
	}

	/**
	 * @see org.eclipse.ui.ISourceProvider#getProvidedSourceNames()
	 */
	@Override
	public String[] getProvidedSourceNames() {
		return new String[]{SELECTED_FILTERS, QUERY_VALID, QUERY_DROP_TYPE};
	}

}
