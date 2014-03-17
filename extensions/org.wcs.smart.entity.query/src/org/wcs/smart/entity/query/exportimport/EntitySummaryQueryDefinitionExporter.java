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
package org.wcs.smart.entity.query.exportimport;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.entity.query.model.EntitySummaryQuery;
import org.wcs.smart.query.common.importexport.SummaryQueryDefinitionExporter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;
import org.wcs.smart.util.SmartUtils;

/**
 * Summary query definition exporter
 * @author egouge
 * @since 1.0.0
 */
public class EntitySummaryQueryDefinitionExporter extends SummaryQueryDefinitionExporter {

	/**
	 * @see org.wcs.smart.query.export.DefinitionQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query instanceof EntitySummaryQuery){
			return true;
		}
		return false;
	}

	/*
	 * Exports the group by part information
	 */
	@Override
	protected void processGroupBy(GroupByPart values, QueryType qt, Session session) throws Exception{
		if (values == null) return;
		for (IGroupBy item: values.getGroupBys()){
			if (item instanceof DateGroupBy) continue;
			List<ListItem> bits = item.getItems(session);
			for (ListItem it : bits){
				if (it.getUuid() != null){
					UuidItemType uuiditem = new UuidItemType();
					uuiditem.setUuid( SmartUtils.encodeHex( it.getUuid() ) );
					uuiditem.getValue().add(it.getName());
					qt.getUuiditem().add(uuiditem);
				}
			}
		}
	}
	
	/**
	 * Nothing to do
	 * 
	 */
	@Override
	protected void processFilter(IFilter f, QueryType qt, Session session) throws Exception{
	}

}
