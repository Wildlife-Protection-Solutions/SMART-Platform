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
package org.wcs.smart.intelligence.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.intelligence.query.IIntelligenceQueryColumnProvider;
import org.wcs.smart.intelligence.query.parser.Parser;
import org.wcs.smart.query.common.model.IQueryColumnProvider;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;

/**
 * Class for representing intelligence record queries.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.intel_record_query")
public class IntelligenceRecordQuery extends SimpleQuery implements IPagedQuery{
	
	public static final String KEY = "intelligencerecord"; //$NON-NLS-1$
	
	@Transient
	@Override
	public String getTypeKey() {
		return KEY;
	}

	@Override
	protected QueryFilter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return new QueryFilter(EmptyFilter.INSTANCE);
		}
		if(queryFilter != null){
			return queryFilter;
		}
		
		try(InputStream is = new ByteArrayInputStream(getQueryFilter().getBytes())){
			Parser parser = new Parser(is);
			queryFilter = parser.IntelligenceFilter();
			return queryFilter;
		}catch (Exception ex){
			throw ex;
		}
	}

	@Override
	public Query clone(Employee newOwner) {
		IntelligenceRecordQuery q = new IntelligenceRecordQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(newOwner);
		q.setQueryFilter(getQueryFilter());
		q.setVisibleColumns(getVisibleColumns());
		q.setStyle(getStyle());
		return q;
	}

	@Override
	@Transient
	protected Class<? extends IQueryColumnProvider> getColumnProviderClass() {
		return IIntelligenceQueryColumnProvider.class;
	}
}
