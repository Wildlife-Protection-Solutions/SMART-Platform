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
package org.wcs.smart.er.query.importexport;

import org.hibernate.Session;
import org.wcs.smart.er.query.filter.summary.MissionIdGroupBy;
import org.wcs.smart.er.query.filter.summary.SamplingUnitGroupBy;
import org.wcs.smart.er.query.filter.summary.SurveyIdGroupBy;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.query.common.importexport.SummaryQueryDefinitionExporter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;
import org.wcs.smart.util.SmartUtils;

/**
 * Summary query definition exporter
 * @author egouge
 * @since 1.0.0
 */
public class SurveySummaryQueryDefinitionExporter extends SummaryQueryDefinitionExporter {

	/**
	 * @see org.wcs.smart.query.export.DefinitionQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query instanceof SurveySummaryQuery){
			return true;
		}
		return false;
	}

	@Override
	public void writeQuerySpecifics(Query query, QueryType xmlQuery) throws Exception {
		super.writeQuerySpecifics(query, xmlQuery);
		
		QueryPart defPart = new QueryPart();
		defPart.setKey("surveyDesignFilter"); //$NON-NLS-1$
		defPart.setValue( ((ISurveyQuery)query).getSurveyDesign() );
		xmlQuery.getQueryPart().add(defPart);
	}
	
	/*
	 * Exports the group by part information
	 */
	@Override
	protected void processGroupBy(GroupByPart values, QueryType qt, Session session) throws Exception{
		if (values == null) return;
		SurveyFilterProcessorVisitor vv = new SurveyFilterProcessorVisitor(session, null);
		for (IGroupBy item: values.getGroupBys()){
			if (item instanceof SamplingUnitGroupBy){
				SamplingUnitGroupBy gb = (SamplingUnitGroupBy)item;
				if (gb.getRawItems() != null){
					for (String uuid : gb.getRawItems()){
						UuidItemType uuidItem = vv.samplingUnitToUuidItem(SmartUtils.decodeHex(uuid));
						if (uuid != null){
							qt.getUuiditem().add(uuidItem);
						}
					}
				}
			}
		}
	}
	
	/*
	 * Exports the filter information
	 */
	protected void processFilter(IFilter f, QueryType qt, Session session) throws Exception{
		if(f == null) return;
		SurveyFilterProcessorVisitor visitor = new SurveyFilterProcessorVisitor(session,qt);
		f.accept(visitor);
	}

}
