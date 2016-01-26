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
package org.wcs.smart.intelligence.query.internal;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.query.IIntelligenceQueryLabelProvider;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.intelligence.query.model.ReceivedDateFilter;
import org.wcs.smart.patrol.SmartPatrolPlugIn;

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
		if (item == FixedQueryColumn.FixedColumns.CA_ID) return Messages.FixedQueryColumn_CaIdCol;
		if (item == FixedQueryColumn.FixedColumns.CA_NAME) return Messages.FixedQueryColumn_CaNameCol;
		if (item == FixedQueryColumn.FixedColumns.INTEL_NAME) return Messages.FixedQueryColumn_NameCol;
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_RECIEVED) return Messages.FixedQueryColumn_ReceivedDateCol;
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_FROM) return Messages.FixedQueryColumn_FromDateColumn;
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_TO) return Messages.FixedQueryColumn_ToDateColumn;
		if (item == FixedQueryColumn.FixedColumns.INTEL_SOURCE) return Messages.FixedQueryColumn_SourceColumn;
		if (item == FixedQueryColumn.FixedColumns.INTEL_PATROL_SOURCE) return Messages.FixedQueryColumn_PatrolColumn;
		if (item == FixedQueryColumn.FixedColumns.INTEL_INFORMANT_ID) return Messages.FixedQueryColumn_InformantCol;
		if (item == FixedQueryColumn.FixedColumns.INTEL_DESCRIPTION) return Messages.FixedQueryColumn_DescriptionCol;
		
		
		if (item == IntelligenceFilterOption.NAME) return Messages.IntelligenceFilterOption_NameOption;
		if (item == IntelligenceFilterOption.SOURCE) return Messages.IntelligenceFilterOption_SoureOption;
		if (item == IntelligenceFilterOption.PATROLID) return Messages.IntelligenceFilterOption_PatrolIdOption;
		if (item == IntelligenceFilterOption.DESCRIPTION) return Messages.IntelligenceFilterOption_DescriptionOption;
		if (item == IntelligenceFilterOption.INFORMANTID) return Messages.IntelligenceFilterOption_InformationIdOption;
		
		if (item instanceof ReceivedDateFilter) return Messages.RecievedDateFilter_ReceivedDateFilterName;
		return null;
	}

	public static Image getImage(IntelligenceFilterOption option){
		if (option == IntelligenceFilterOption.PATROLID) return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
		if (option == IntelligenceFilterOption.INFORMANTID) return IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INFORMANT_ICON);
		return null;
	}
}
