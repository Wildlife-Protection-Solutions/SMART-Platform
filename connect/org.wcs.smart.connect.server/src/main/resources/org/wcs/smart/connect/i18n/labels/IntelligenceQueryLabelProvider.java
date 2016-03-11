/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.intelligence.query.IIntelligenceQueryLabelProvider;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.intelligence.query.model.ReceivedDateFilter;

/**
 * Implementation of query label provider.
 * 
 * @author Emily
 *
 */
public class IntelligenceQueryLabelProvider implements
		IIntelligenceQueryLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == FixedQueryColumn.FixedColumns.CA_ID) return Messages.getString("IntelligenceQueryLabelProvider.IdLabel", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.FixedColumns.CA_NAME) return Messages.getString("IntelligenceQueryLabelProvider.CaName", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.FixedColumns.INTEL_NAME) return Messages.getString("IntelligenceQueryLabelProvider.IntelName", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_RECIEVED) return Messages.getString("IntelligenceQueryLabelProvider.RecievedName", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_FROM) return Messages.getString("IntelligenceQueryLabelProvider.FromName", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_TO) return Messages.getString("IntelligenceQueryLabelProvider.ToName", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.FixedColumns.INTEL_SOURCE) return Messages.getString("IntelligenceQueryLabelProvider.SourceName", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.FixedColumns.INTEL_PATROL_SOURCE) return Messages.getString("IntelligenceQueryLabelProvider.PatrolName", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.FixedColumns.INTEL_INFORMANT_ID) return Messages.getString("IntelligenceQueryLabelProvider.InformantId", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.FixedColumns.INTEL_DESCRIPTION) return Messages.getString("IntelligenceQueryLabelProvider.DescriptionLabel", l); //$NON-NLS-1$
		
		if (item == IntelligenceFilterOption.NAME) return Messages.getString("IntelligenceQueryLabelProvider.NameLabel", l); //$NON-NLS-1$
		if (item == IntelligenceFilterOption.SOURCE) return Messages.getString("IntelligenceQueryLabelProvider.SourceLabel", l); //$NON-NLS-1$
		if (item == IntelligenceFilterOption.PATROLID) return Messages.getString("IntelligenceQueryLabelProvider.PIDLabel", l); //$NON-NLS-1$
		if (item == IntelligenceFilterOption.DESCRIPTION) return Messages.getString("IntelligenceQueryLabelProvider.DescriptionLabel", l); //$NON-NLS-1$
		if (item == IntelligenceFilterOption.INFORMANTID) return Messages.getString("IntelligenceQueryLabelProvider.IDLabel", l); //$NON-NLS-1$
		
		if (item instanceof ReceivedDateFilter) return Messages.getString("IntelligenceQueryLabelProvider.ReceievedDateFilterLabel", l); //$NON-NLS-1$
		

		if (item.equals(NUM_RECORD_SHORTNAME)) return Messages.getString("PsqlSummaryIntelligenceQueryEngine.NumberRecordsHeaderLabel", l);
		if (item.equals(NUM_RECORD_LONGNAME)) return Messages.getString("PsqlSummaryIntelligenceQueryEngine.NumberRecordsHeaderLabel", l);
		if (item.equals(FOLLOWUP_SHORTNAME)) return Messages.getString("PsqlSummaryIntelligenceQueryEngine.FollwedUpHeaderLabel", l);
		if (item.equals(FOLLOWUP_LONGNAME)) return Messages.getString("PsqlSummaryIntelligenceQueryEngine.FollwedUpHeaderLabel", l);
		if (item.equals(NOFOLLOWUP_LONGNAME)) return Messages.getString("PsqlSummaryIntelligenceQueryEngine.NotFollowedUpHeaderLabel", l);
		if (item.equals(NOFOLLOWUP_SHORTNAME)) return Messages.getString("PsqlSummaryIntelligenceQueryEngine.NotFollowedUpHeaderLabel", l);
		
		return null;
	}
}
