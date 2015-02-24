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
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.query.model.IQueryType;

/**
 * A source provider that provides the following sources:
 * <ul>
 * <li> Query Valid - null if query valid, String if query in error.</li>
 * <li> Query Date Valid - null if date filter valid, String if query in error.</li>
 * <li> Selected Definition Items - current selected query definition item</li>
 * 
 * </ul>
 *  
 * @author Emily
 * @since 1.0.0
 */
public class QuerySourceProvider extends AbstractSourceProvider {

	public final static String QUERY_VALID = "org.wcs.smart.query.ui.queryvalid"; //$NON-NLS-1$
	public final static String QUERY_DATE_VALID = "org.wcs.smart.query.ui.querydatevalid"; //$NON-NLS-1$	
	public final static String DEFINITION_ITEMS = "org.wcs.smart.query.ui.definitionitem"; //$NON-NLS-1$
	public final static String DEFINITION_ITEMS_SRC = "org.wcs.smart.query.ui.definitionitem.source"; //$NON-NLS-1$
	
	public final static String DEFINITION_PANEL_ID = "org.wcs.smart.query.ui.definitionPanelId"; //$NON-NLS-1$
	public final static String QUERY_TYPE = "org.wcs.smart.query.ui.queryType"; //$NON-NLS-1$

	private HashMap<String, Object> data = new HashMap<String, Object>();
	
	public QuerySourceProvider(){
		data.put(DEFINITION_ITEMS, IEvaluationContext.UNDEFINED_VARIABLE);
		data.put(QUERY_VALID, IEvaluationContext.UNDEFINED_VARIABLE);
		data.put(QUERY_DATE_VALID, IEvaluationContext.UNDEFINED_VARIABLE);
		
		//add this to the context so we can get this from the context elsewhere
		//should be addon if we convert to pure e4
		IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		ContextInjectionFactory.inject(this, parentContext);
		parentContext.set(QuerySourceProvider.class, this);
	}
	/**
	 * @see org.eclipse.ui.ISourceProvider#dispose()
	 */
	@Override
	public void dispose() {
		data = null;
	}
	
	public void setQueryDefinitionPanelId(String id, IQueryType queryType){
		data.put(DEFINITION_PANEL_ID, id);
		data.put(QUERY_TYPE, queryType);
		fireSourceChanged(ISources.ACTIVE_PART_ID, DEFINITION_PANEL_ID, id);
	}
	/**
	 * Sets the filter selection.
	 * @param selection a selection of items to add to the current query 
	 */
	public void setQueryDefinitionSelection(IStructuredSelection selection, String queryItemPanelId){
		data.put(DEFINITION_ITEMS, selection);		
		data.put(DEFINITION_ITEMS_SRC, queryItemPanelId);
		fireSourceChanged(ISources.ACTIVE_PART_ID, DEFINITION_ITEMS, selection);
	}
	
	/**
	 * Sets the state of the current query.
	 * 
	 * @param isValid <code>true</code> or <code>false</code> depending on the state of the
	 * @param errorMessage string error message description or null if query is valid
	 * current query.
	 */
	public void setQueryValid(Boolean isValid, String errorMessage){
		if (isValid){
			data.put(QUERY_VALID, null);
		}else{
			data.put(QUERY_VALID, errorMessage);
		}
		fireSourceChanged(ISources.ACTIVE_PART_ID, QUERY_VALID, isValid);
	}
	
	/**
	 * Sets the state of the current query date range.
	 * @param isValid <code>true</code> if valid date range, <code>false</code> otherwise
	 * @param errorMessage error message associated with date range.  Cannot
	 * be null if isValid = true;
	 */
	public void setQueryDateValid(Boolean isValid, String errorMessage){
		if (isValid){
			data.put(QUERY_DATE_VALID, null);
		}else{
			data.put(QUERY_DATE_VALID, errorMessage);
		}
		fireSourceChanged(ISources.ACTIVE_PART_ID, QUERY_DATE_VALID, isValid);
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
		return new String[]{DEFINITION_ITEMS, QUERY_VALID, QUERY_DATE_VALID, DEFINITION_PANEL_ID, QUERY_TYPE};
	}

	/**
	 * This is a helper function for e3 to e4 bridge that finds the provider in
	 * the root eclise context using platformui
	 * 
	 * @return
	 */
	public static QuerySourceProvider getSourceProviderFromContext(){
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		QuerySourceProvider provider = (QuerySourceProvider) (ctx.get(QuerySourceProvider.class));
		return provider;
	}
}
