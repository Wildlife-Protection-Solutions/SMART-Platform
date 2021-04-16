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
package org.wcs.smart.query;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.IDataModelItemListener;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.IFilterVisitor;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.summary.AttributeGroupBy;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * DataModel listener for updating query filters
 * when attributes are converted from single to multi-list type
 * 
 * @author Emily
 *
 */
public enum QueryDataModelItemListener implements IDataModelItemListener {
	
	INSTANCE;

	@Override
	public void deleteItem(Session currentSession, Object itemToDelete) throws Exception { }

	@Override
	public void addItem(Session currentSession, Object itemToAdd) {	}

	@Override
	public void itemEnabledStateChanged(Session currentSession, Object itemToAdd) { }
	
	@Override
	public void singleToMulti(Session session, Attribute attribute) throws SQLException{
		
		for (IQueryType type : QueryTypeManager.INSTANCE.getAllQueryTypes()) {
			
			
			List<? extends Query> queries = QueryFactory.buildQuery(session, type.getHibernateClass(), 
					new Object[] {"conservationArea", attribute.getConservationArea()}) //$NON-NLS-1$
					.list();
			
			for (Query q : queries) {
				if (q instanceof SimpleQuery) {
					processSimpleQuery((SimpleQuery)q, attribute);
				}else if (q instanceof SummaryQuery) {
					processSummaryQuery((SummaryQuery)q, attribute);
				}else if (q instanceof GriddedQuery) {
					processGridQuery((GriddedQuery) q,attribute);
				}
				session.update(q);
			}
		}
		
	}
	
	private void processSimpleQuery(SimpleQuery q, Attribute attribute)  {
		try {
			QueryFilter filter = q.getFilter();
			String current = filter.asString();
			
			processFilter(filter.getFilter(), attribute);
			
			String newfilter = filter.asString();
			
			if (!current.equals(newfilter)) {
				q.setQueryFilter(newfilter);
			}
			
		}catch (Exception ex) {
			QueryPlugIn.log(MessageFormat.format(Messages.QueryDataModelItemListener_ConvertError,  attribute.getKeyId(), ex.getMessage()), ex);
		}
	}
	
	private void processSummaryQuery(SummaryQuery q, Attribute attribute) {
		try {
			SumQueryDefinition def = q.getQueryDefinition();
			
			
			boolean modified = false;
			
			for (QueryFilter filter : new QueryFilter[] {def.getRateFilter(), def.getValueFilter()}) {
				if (filter == null) continue;
				
				String currentfilter = filter.asString();
				processFilter(filter.getFilter(), attribute);
				String newfilter = filter.asString();
				if (!currentfilter.equals(newfilter)) modified = true;
				
			}
			
			for (IGroupBy gb : def.getRowGroupByPart().getGroupBys()) {
				if (processGroupBy(gb, attribute)) modified = true;
			}
			for (IGroupBy gb : def.getColumnGroupByPart().getGroupBys()) {
				if (processGroupBy(gb, attribute)) modified = true;
			}
			
			for (IValueItem v : def.getValuePart().getValueItems()) {
				if (processValueItem(v, attribute)) modified = true;
			}
			
			if (modified) {				
				q.setQuery(def.asQuery());
			}
			
		}catch (Exception ex) {
			QueryPlugIn.log(MessageFormat.format(Messages.QueryDataModelItemListener_ConvertError,  attribute.getKeyId(), ex.getMessage()), ex);
		}
	}

	private void processGridQuery(GriddedQuery q, Attribute attribute) {
		try {
			GridQueryDefinition def = q.getQueryDefinition();
			boolean modified = false;
			
			for (QueryFilter filter : def.getAllFilters()) {
				if (filter == null) continue;
				
				String currentfilter = filter.asString();
				processFilter(filter.getFilter(), attribute);
				String newfilter = filter.asString();
				if (!currentfilter.equals(newfilter)) modified = true;
				
			}

			IValueItem v = def.getValuePart();
			if (processValueItem(v, attribute)) modified = true;
			
			if (modified) {				
				q.setQuery(def.asQuery());
			}
			
		}catch (Exception ex) {
			QueryPlugIn.log(MessageFormat.format(Messages.QueryDataModelItemListener_ConvertError,  attribute.getKeyId(), ex.getMessage()), ex);
		}
	}
	
	private boolean processValueItem(IValueItem valueItem, Attribute attribute) {
		final boolean[] mm = new boolean[]{false};
		IValueVisitor visitor = new IValueVisitor() {
			@Override
			public void visit(IValueItem valueItem) {
				if (valueItem instanceof AttributeValueItem) {
					AttributeValueItem avi = (AttributeValueItem)valueItem;
					if (avi.getAttributeKey().equalsIgnoreCase(attribute.getKeyId())) {
						avi.updateValues(attribute.getKeyId(), attribute.getType());
						mm[0] = true;
					}
				}
			}
		};
		valueItem.accept(visitor);
		return mm[0];
	}
	private boolean processGroupBy(IGroupBy gb, Attribute attribute) {
		if (!(gb instanceof AttributeGroupBy)) return false;
		if (!((AttributeGroupBy)gb).getAttributeKey().equalsIgnoreCase(attribute.getKeyId())) return false;
		AttributeGroupBy agb = (AttributeGroupBy)gb;
		agb.updateValues(agb.getCategoryHkey(), attribute.getKeyId(), attribute.getType(), agb.getFilterKeys(), agb.getTreeLevel());					
		return true;
	}
	
	private void processFilter(IFilter filter, Attribute attribute) {
		IFilterVisitor visitor = new IFilterVisitor() {
			@Override
			public void visit(IFilter filter) {
				if (filter instanceof AttributeFilter) {
					AttributeFilter afilter = (AttributeFilter) filter;
					
					String attributeKey = afilter.getAttributeKey();
					if (attribute.getKeyId().equalsIgnoreCase(attributeKey)) {
						//change to multi-select
						String value = afilter.getValue().toString();
						if (afilter.getValue().toString().equalsIgnoreCase(AttributeFilter.ANY_OPTION_KEY)) {
							value = attribute.getAttributeList().stream().map(e->e.getKeyId()).collect(Collectors.joining(AttributeFilter.MLIST_SEPERATOR));
						}
						afilter.updateValues(afilter.getAttributeKey(), 
								Attribute.AttributeType.MLIST, 
								Operator.OR, value, null);
					}
				}
			}
		};
		filter.accept(visitor);
	}
}
