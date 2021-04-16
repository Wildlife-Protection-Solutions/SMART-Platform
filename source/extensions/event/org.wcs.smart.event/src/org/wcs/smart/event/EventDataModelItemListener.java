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
package org.wcs.smart.event;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.IDataModelItemListener;
import org.wcs.smart.event.filter.ParsedFilter;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.BooleanFilter;
import org.wcs.smart.filter.BracketFilter;
import org.wcs.smart.filter.CategoryAttributeFilter;
import org.wcs.smart.filter.CategoryFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.NotFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * DataModel listener for updating event filters
 * when attributes are converted from single to multi list type
 * 
 * @author Emily
 *
 */
public enum EventDataModelItemListener implements IDataModelItemListener {
	
	INSTANCE;

	@Override
	public void deleteItem(Session currentSession, Object itemToDelete) throws Exception { }

	@Override
	public void addItem(Session currentSession, Object itemToAdd) {	}

	@Override
	public void itemEnabledStateChanged(Session currentSession, Object itemToAdd) { }
	
	@Override
	public void singleToMulti(Session session, Attribute attribute) throws SQLException{
		//update observations
		
		List<EFilter> filters = QueryFactory.buildQuery(session, EFilter.class, 
				new Object[] {"conservationArea", attribute.getConservationArea()}).list(); //$NON-NLS-1$
		
		for (EFilter f : filters) {
			try {
				ParsedFilter pp = f.getParsedFilter();
				if(pp.getFilter() != null) {
					processFilter(pp.getFilter(), attribute);
					f.setFilterString( pp.asString() );
					session.update(f);
				}
			} catch (Exception e) {
				throw new SQLException(e);
			}
		}
		
	}
	private void processFilter(IFilter filter, Attribute attribute) throws Exception {
		if (filter instanceof BooleanFilter) {
			processFilter(((BooleanFilter) filter).getFilter1(), attribute);
			processFilter(((BooleanFilter) filter).getFilter2(), attribute);			
		}else if (filter instanceof NotFilter) {
			processFilter(((NotFilter) filter).getFilter(), attribute);
		}else if (filter instanceof BracketFilter) {
			processFilter(((BracketFilter) filter).getFilter(), attribute);			
		}else if (filter instanceof CategoryAttributeFilter) {
			processFilter(((CategoryAttributeFilter) filter).getCategoryFilter(), attribute);
			processFilter(((CategoryAttributeFilter) filter).getAttributeFilter(), attribute);
		}else if (filter instanceof CategoryFilter) {

		}else if (filter instanceof AttributeFilter) {
			AttributeFilter afilter = (AttributeFilter)filter;
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
}
