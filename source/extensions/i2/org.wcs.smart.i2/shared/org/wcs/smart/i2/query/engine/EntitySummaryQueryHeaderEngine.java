/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query.engine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.i2.query.SummaryHeader;
import org.wcs.smart.i2.query.SummaryQueryResult;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem.GroupByType;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;
import org.wcs.smart.i2.query.observation.filter.ValuePart;

/**
 * Engine for computing summary query headers
 * 
 * @author Emily
 *
 */
public enum EntitySummaryQueryHeaderEngine {
	
	INSTANCE;
	
	/**
	 * Computes the header information for a given
	 * query.
	 * 
	 * @param query the summary query
	 * @param results the summary query results to update
	 * @param session hibernate session
	 */
	public void getHeaderInfo(SumQueryDefinition queryDefinition, SummaryQueryResult results, LocalDate[] dateRange, Set<UUID> profiles, IQueryItemProvider itemProvider, Locale l, Session session) throws Exception{
		
		// value headers
		ValuePart vp = queryDefinition.getValuePart();
		String name = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(vp.getValueOption(), l);
		SummaryHeader header = new SummaryHeader(name, name, vp.getValueOption().getKey(), true);
		results.addValueHeader(header);
		

		for (GroupByItem item : queryDefinition.getRowGroupByPart().getItems()) {
			List<ListItem> allItems = item.getAllOptions(session, profiles, itemProvider, dateRange, l);
			String colkey = computeColumnKey(item);
			List<SummaryHeader> rowHeaders = new ArrayList<>();
			
			for (int i = 0; i < allItems.size(); i ++){
				ListItem it = allItems.get(i);
				if (item.getFilterOptions() == null || item.getFilterOptions().isEmpty()) {
					rowHeaders.add( new SummaryHeader( it.getName(), it.getFullName(), colkey, it.getKeyId(), false) );
				}else {
					for (String key : item.getFilterOptions()) {
						if (key.equals(it.getKeyId())) {
							rowHeaders.add( new SummaryHeader( it.getName(), it.getFullName(), colkey, it.getKeyId(), false) );
							break;
						}
					}
				}
					
				
			}
			results.addRowHeader(rowHeaders.toArray(new SummaryHeader[rowHeaders.size()]));
		}
		
		for (GroupByItem item : queryDefinition.getColumnGroupByPart().getItems()) {
			List<ListItem> allItems = item.getAllOptions(session, profiles, itemProvider, dateRange, l);
			String colkey = computeColumnKey(item);
			
			List<SummaryHeader> rowHeaders = new ArrayList<>();
			
			for (int i = 0; i < allItems.size(); i ++){
				ListItem it = allItems.get(i);
				if (item.getFilterOptions() == null || item.getFilterOptions().isEmpty()) {
					rowHeaders.add( new SummaryHeader( it.getName(), it.getFullName(), colkey, it.getKeyId(), false) );
				}else {
					for (String key : item.getFilterOptions()) {
						if (key.equals(it.getKeyId())) {
							rowHeaders.add( new SummaryHeader( it.getName(), it.getFullName(), colkey, it.getKeyId(), false) );
							break;
						}
					}
				}
					
				
			}
			results.addColumnHeader(rowHeaders.toArray(new SummaryHeader[rowHeaders.size()]));
		}
		
	}
	
	public String computeColumnKey(GroupByItem item) {
		String colkey = null;
		if (item.getGroupByType() == GroupByType.ENTITYTYPE) {
			colkey = "ET"; //$NON-NLS-1$
		}else if (item.getGroupByType() == GroupByType.RECORDSOURCE) {
				colkey = "RSRC"; //$NON-NLS-1$
		}else if (item.getGroupByType() == GroupByType.RECORDSTATUS) {
			colkey = "RSTAT"; //$NON-NLS-1$
		}else if (item.getGroupByType() == GroupByType.CA) {
			colkey = "CA"; //$NON-NLS-1$
		}else {
			colkey = item.getAttributeKey();
			if (item.getOtherKey() != null) {
				colkey += "_" + item.getOtherKey(); //$NON-NLS-1$
			}
		}
		return colkey;
	}
}
