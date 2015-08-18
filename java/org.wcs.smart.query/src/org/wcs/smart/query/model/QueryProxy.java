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
package org.wcs.smart.query.model;

import java.util.Collection;
import java.util.HashMap;

import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.ui.definition.QueryDefPanel;
import org.wcs.smart.query.ui.model.DropItem;
/**
 * A query proxy used for GUI interactions.
 * 
 * @author Emily
 *
 */
public class QueryProxy {

	private Query query;
	private IQueryType qType;
	
	private HashMap<String, Collection<DropItem>> dropItems;
	private QueryDefPanel definitionPanel;
	private String isValid;
	
	/**
	 * Creates a new query proxy for the given query.
	 * 
	 * @param query
	 */
	public QueryProxy (Query query){
		this.query = query;
		this.qType = QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey());
		dropItems = new HashMap<String, Collection<DropItem>>();
	}
	
	/**
	 * Disposes of existing drop items; then updates
	 * to new drop items
	 * @param panelId the drop panel id
	 * @param dropItems collection of drop items associated with the drop panel
	 */
	public void setDropItems(String panelId, Collection<DropItem> dropItems ){
		disposeDropItems(panelId);
		this.dropItems.put(panelId, dropItems);
	}
	
	/**
	 * 
	 * @param panelId
	 * @return the drop item associated with the given panel
	 */
	public  Collection<DropItem> getDropItems(String panelId){
		return this.dropItems.get(panelId);
	}
	
	/**
	 * 
	 * @return the query associated with the proxy
	 */
	public Query getQuery(){
		return this.query;
	}
	
	public IQueryType getQueryType(){
		return this.qType;
	}
	
	/**
	 * Disposes of all drop items
	 */
	public void dispose(){
		for(Collection<DropItem> dis : dropItems.values()){
			if (dis != null){
				for (DropItem di : dis){
					di.dispose();
				}
			}
		}
		dropItems.clear();
	}
	/**
	 * Dispose of all drop items in query proxy
	 * 
	 * @param panelId
	 */
	private void disposeDropItems(String panelId){
		Collection<DropItem> di = dropItems.get(panelId);
		if (di != null){
			for (DropItem d : di){
				if (d != null) d.dispose();
			}
			dropItems.remove(panelId);
		}
	}

	/**
	 * 
	 * @return the query def panel associated with the query proxy
	 */
	public QueryDefPanel getQueryDefinitionPanel(){
		return this.definitionPanel;
	}
	/**
	 * 
	 * @param panel sets the query def panel associated with the query proxy
	 */
	public void setQueryDefinitionPanel(QueryDefPanel panel){
		this.definitionPanel = panel;
	}
	
	/**
	 * 
	 * @return if the current query definition is valid
	 */
	public boolean isValid(){
		return this.isValid == null;
	}
	/**
	 * 
	 * @return current query definition error or null if query is valid
	 */
	public String getErrorMessage(){
		return this.isValid;
	}
	/**
	 * Sets the current query definition error.
	 * @param valid query error or null if query is valid
	 */
	public void setValid(String valid){
		this.isValid = valid;
	}
}
