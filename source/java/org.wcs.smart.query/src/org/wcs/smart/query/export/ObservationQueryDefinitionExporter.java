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
package org.wcs.smart.query.export;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.ObservationQuery;
import org.wcs.smart.query.model.PatrolQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.internal.filter.PatrolFilter;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Exports a observation query definition
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ObservationQueryDefinitionExporter extends DefinitionQueryExporter {

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.DefinitionQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query instanceof ObservationQuery || query instanceof PatrolQuery ){
			return true;
		}
		return false;
	}

	/**
	 * @see org.wcs.smart.query.export.DefinitionQueryExporter#writeQuerySpecifics(org.wcs.smart.query.model.Query, org.wcs.smart.query.xml.model.QueryType)
	 */
	@Override
	public void writeQuerySpecifics(Query query, QueryType xmlQuery) throws Exception {
		QueryPart defPart = new QueryPart();
		defPart.setKey("definition"); //$NON-NLS-1$
		defPart.setValue( ((SimpleQuery)query).getQueryFilter() );
		xmlQuery.getQueryPart().add(defPart);
		
		
		defPart = new QueryPart();
		defPart.setKey("columns"); //$NON-NLS-1$
		defPart.setValue( ((SimpleQuery)query).getVisibleColumns() );
		xmlQuery.getQueryPart().add(defPart);
				
		IFilter queryFilter = ((SimpleQuery)query).getFilter();
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			processFilter(queryFilter, xmlQuery, s);
		}finally{
			s.getTransaction().rollback();
			s.close();
		}
	}

	/*
	 * Process the filter
	 */
	private void processFilter(IFilter f, QueryType qt, Session session) throws Exception{
		if (f instanceof PatrolFilter){
			PatrolFilter pf = (PatrolFilter)f;
			UuidItemType item = super.processPatrolOption(pf.getPatrolOption(), pf.getValue(), session);
			if (item != null){
				qt.getUuiditem().add(item);
			}
		}
		
		List<IFilter> kids = f.getChildren();
		if (kids != null){
			for (IFilter kid : kids){
				processFilter(kid, qt, session);
			}
		}
	}

}
