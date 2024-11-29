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
package org.wcs.smart.patrol.query.exportimport;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolQueryFactory;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptions;
import org.wcs.smart.patrol.query.model.PatrolQueryValidator;
import org.wcs.smart.patrol.query.model.PatrolSummaryQuery;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.common.importexport.SummaryQueryDefinitionImporter;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Query importer for importing summary query definitions
 * @author egouge
 * @since 1.0.0
 */
public class PatrolSummaryQueryDefinitionImporter extends SummaryQueryDefinitionImporter{

	@Override
	public boolean canImport(IQueryType qt) {
		return qt.getKey().equals(PatrolSummaryQuery.KEY);
	}

	@Override
	protected void validateQuery(ConservationArea caImport, SumQueryDefinition sumDef, String langCode,
			HashMap<String, UuidItemType> uuidLookup, Session session)
			throws Exception {
		PatrolQueryValidator validator = new PatrolQueryValidator(langCode, uuidLookup, session, QueryDataModelManager.getManager(caImport), caImport);
		if (sumDef.getValueFilter() != null ){
			warnings.addAll(validator.validate(sumDef.getValueFilter().getFilter()));
		}
		if (sumDef.getRateFilter() != null){
			warnings.addAll(validator.validate(sumDef.getRateFilter().getFilter()));
		}
		//process value items
		for (IValueItem item: sumDef.getValuePart().getValueItems()){
			warnings.addAll(validator.validate(item));
		}
		
		//process group by 
		for (IGroupBy gbpart: sumDef.getColumnGroupByPart().getGroupBys()){
			warnings.addAll(validator.validate(gbpart));
		}		
		for (IGroupBy gbpart: sumDef.getRowGroupByPart().getGroupBys()){
			warnings.addAll(validator.validate(gbpart));
		}		
	
		
		if (caImport.getIsCcaa()) {
			List<IGroupBy> groupBys = new ArrayList<>(sumDef.getColumnGroupByPart().getGroupBys());
			groupBys.addAll(sumDef.getRowGroupByPart().getGroupBys());
			
			for (IGroupBy gp : groupBys) {
				if (gp instanceof PatrolGroupBy) {
					PatrolGroupBy pgb = (PatrolGroupBy) gp;
					PatrolQueryOption po = pgb.getOption();
					
					if (po == PatrolQueryOption.TEAM) {
						//update to teamkey
						po = PatrolQueryOption.TEAM_KEY;
						pgb.setOption(po);
					}else if (po == PatrolQueryOption.MANDATE) {
						//update to mandatekey
						po = PatrolQueryOption.MANDATE_KEY;
						pgb.setOption(po);
					}else if (po == PatrolQueryOption.PATROL_TRANSPORT_TYPE) {
						//update to transport type key
						po = PatrolQueryOption.PATROL_TRANSPORT_TYPE_KEY;
						pgb.setOption(po);
					}
					
					boolean found = false;
					for (PatrolQueryOption p : PatrolQueryOptions.SHARED_PATROL_GROUBY_OPTIONS) {
						if (p == po) {
							found = true;
							break;
						}
					}
					if (!found) warnings.add(MessageFormat.format(Messages.PatrolSummaryQueryDefinitionImporter_GroupByNotSupported2, po.getGuiName(Locale.getDefault())));
					
				}
			}
			
			
		}
	}

	@Override
	public SummaryQuery createQuery(String queryType) {
		return PatrolQueryFactory.createSummaryQuery();
	}
	
}
