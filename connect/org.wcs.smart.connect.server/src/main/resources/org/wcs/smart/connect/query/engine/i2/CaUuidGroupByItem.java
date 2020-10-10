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
package org.wcs.smart.connect.query.engine.i2;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.i2.query.observation.filter.GroupByPart;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;
import org.wcs.smart.util.UuidUtils;

/**
 * Wrapper around group by item used to convert
 * CA id/name to  uuid when includeuuids is provided
 * as an option in the query parameters.
 * 
 * @author Emily
 *
 */
public class CaUuidGroupByItem extends GroupByItem{

	public CaUuidGroupByItem(GroupByItem item) {
		super(item.getGroupByType(), item.getFilterOptions());
	}
	
	@Override
	public List<ListItem> getAllOptions(Session session, Set<UUID> profiles, IQueryItemProvider itemProvider, LocalDate[] dateRange, Locale locale) {
		if (super.getGroupByType() == GroupByType.CA) {
			List<ListItem> items = new ArrayList<>();
			for (ConservationArea ca : itemProvider.getConservationAreas()) {
				items.add(new ListItem(UuidUtils.uuidToString(ca.getUuid()), UuidUtils.uuidToString(ca.getUuid()),UuidUtils.uuidToString(ca.getUuid())));									}
			return items;
		}
		return super.getAllOptions(session, profiles, itemProvider, dateRange, locale);
	}
	
	
	/**
	 * Update summary definition to use the cauuidgroup by in place of
	 * ca group bys.
	 * 
	 * @param def
	 */
	public static final void replaceCaGroupBy(SumQueryDefinition def) {
		GroupByPart p2 = def.getRowGroupByPart();
		List<GroupByItem> r1 = p2.getItems();
		for (int i =0; i< r1.size(); i ++) {
			GroupByItem g = r1.get(i);
			if (g.getGroupByType() == GroupByItem.GroupByType.CA) {
				r1.set(i, new CaUuidGroupByItem(g));
			}
		}
		
		p2 = def.getColumnGroupByPart();
		r1 = p2.getItems();
		for (int i =0; i< r1.size(); i ++) {
			GroupByItem g = r1.get(i);
			if (g.getGroupByType() == GroupByItem.GroupByType.CA) {
				r1.set(i, new CaUuidGroupByItem(g));
			}
		}
		
	}
}
