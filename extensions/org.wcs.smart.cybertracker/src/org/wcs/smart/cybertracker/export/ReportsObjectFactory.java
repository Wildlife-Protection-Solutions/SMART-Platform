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
package org.wcs.smart.cybertracker.export;

import java.util.Collection;

import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.model.reports.Items;
import org.wcs.smart.cybertracker.model.reports.Items.Item;
import org.wcs.smart.cybertracker.model.reports.Reports;
import org.wcs.smart.cybertracker.model.reports.Table;

/**
 * Factory for manipulations with {@link Reports} related objects
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ReportsObjectFactory {
	
	public static final Integer TAG_0_OUTPUT_MODE = 2;

	public static Items.Item createColumnItem(String sourceGuid, String columnName) {
		return createColumnItem(sourceGuid, columnName, null);
	}
	
	public static Items.Item createColumnItem(String sourceGuid, String columnName, Integer outputMode) {
		CyberTrackerId id = new CyberTrackerId(); //just some id to be used for this item
		Items.Item item = new Items.Item();
		item.setId(id.getNodeId());
		item.setName(columnName);
		item.setSourceGuid(sourceGuid);
		item.setChecked("True"); //$NON-NLS-1$
		item.setOutputMode(outputMode);
		return item;
	}
	
	public static Reports createReports(Collection<Items.Item> columnItems) {
		Reports reports = new Reports();
		Reports.List list = new Reports.List();
		reports.setList(list);
		Items items = new Items();
		list.setItems(items);
		Item reportItem = createReportItem(columnItems);
		items.getItem().add(reportItem);
		reports.setActiveId(reportItem.getId());
		return reports;
	}
	
	private static Items.Item createReportItem(Collection<Items.Item> columnItems) {
		CyberTrackerId id = new CyberTrackerId(); //just some id to be used for report and query (node - report; item = query)
		Items.Item item = new Items.Item();
		item.setId(id.getNodeId());
		item.setName("SMART Report"); //$NON-NLS-1$
		
		Items.Item.Queries queries = new Items.Item.Queries();
		item.setQueries(queries);
		Items items = new Items();
		queries.setItems(items);
		
		//creating query item
		Items.Item qItem = new Items.Item();
		qItem.setId(id.getItemId());
		qItem.setName("SMART Query"); //$NON-NLS-1$
		qItem.setChecked("True"); //$NON-NLS-1$
		qItem.setDateMode(1);
		qItem.setDateFrom("1/1/1980"); //$NON-NLS-1$
		qItem.setDateTo("2/2/2222"); //$NON-NLS-1$
		qItem.setTable(createReportsTable(columnItems));
		Items.Item.Cursor cursor = new Items.Item.Cursor();
		cursor.setIndex(-1);
		qItem.setCursor(cursor);
		items.getItem().add(qItem);
		
		return item;
	}
	
	private static Table createReportsTable(Collection<Items.Item> columnItems) {
		Table table = new Table();
		table.setName("SMART Table"); //$NON-NLS-1$
		if (columnItems != null && !columnItems.isEmpty()) {
			Table.Columns columns = new Table.Columns();
			Items items = new Items();
			columns.setItems(items);
			items.getItem().addAll(columnItems);
			table.setColumns(columns);
		}
		table.setGrouping("False"); //$NON-NLS-1$
		return table;
	}

}
