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
package org.wcs.smart.intelligence.query.ui.dropitem;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.ui.DefinitionPanel;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Drop item factory for ingelligence options.
 *  
 * 
 * @author Emily
 *
 */
public class IntelligenceDropItemFactory implements IDropItemFactory{
	
	public static IntelligenceDropItemFactory INSTANCE = new IntelligenceDropItemFactory();
	
	public DropItem createDropItem(IntelligenceFilterOption filter){
		if (filter == IntelligenceFilterOption.DESCRIPTION || 
				filter == IntelligenceFilterOption.NAME){
			return new TextFilterDropItem(filter);
		}else if (filter == IntelligenceFilterOption.INFORMANTID ||
				filter == IntelligenceFilterOption.SOURCE){
			return new ListFilterDropItem(filter);
		}else if (filter == IntelligenceFilterOption.PATROLID){
			return new TextListFilterDropItem(filter);
		}
		return null;
	}

	@Override
	public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
		if (source instanceof IntelligenceFilterOption){
			DropItem di = createDropItem((IntelligenceFilterOption) source);
			if (di != null){
				return new DropItem[]{di};
			}
		}else if (source instanceof Operator){
			return BasicDropItemFactory.INSTANCE.createOtherDropItem((Operator)source);
		}
		return null;
	}

	@Override
	public void generateDropItems(QueryProxy proxy, Session session) {
		if (proxy.getQuery() instanceof IntelligenceRecordQuery){
			IFilter queryFilter = ((IntelligenceRecordQuery)proxy.getQuery()).getFilter().getFilter();
			proxy.setDropItems(DefinitionPanel.ID, asDropItems(queryFilter, session));
		}
		
	}
	
	/*
	 * Converts a filter to a set of drop items
	 */
	private List<DropItem> asDropItems(IFilter filter, Session session){
		List<DropItem> items = new ArrayList<DropItem>();
		try{
			DropItem[] filterItems = filter.getDropItems(session);
			for(DropItem i : filterItems){
				items.add(i);
			}
			
		}catch (Exception ex){
			items.add(new ErrorDropItem(MessageFormat.format(Messages.IntelligenceDropItemFactory_ParseError, new Object[]{ex.getMessage()})));
		}
		return items;
	}
}
