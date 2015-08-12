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
package org.wcs.smart.shared.labels;

import java.util.Locale;

import org.wcs.smart.intelligence.query.IIntelligenceQueryLabelProvider;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.intelligence.query.model.RecievedDateFilter;

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
		if (item == FixedQueryColumn.FixedColumns.CA_ID) return "Conservation Area ID";
		if (item == FixedQueryColumn.FixedColumns.CA_NAME) return "Conservation Area Name";
		if (item == FixedQueryColumn.FixedColumns.INTEL_NAME) return "Name";
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_RECIEVED) return "Recieved Date";
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_FROM) return "From Date";
		if (item == FixedQueryColumn.FixedColumns.INTEL_DATE_TO) return "To Date";
		if (item == FixedQueryColumn.FixedColumns.INTEL_SOURCE) return "Source";
		if (item == FixedQueryColumn.FixedColumns.INTEL_PATROL_SOURCE) return "Source Patrol";
		if (item == FixedQueryColumn.FixedColumns.INTEL_INFORMANT_ID) return "Informant ID";
		if (item == FixedQueryColumn.FixedColumns.INTEL_DESCRIPTION) return "Description";
		
		if (item == IntelligenceFilterOption.NAME) return "Name";
		if (item == IntelligenceFilterOption.SOURCE) return "Source";
		if (item == IntelligenceFilterOption.PATROLID) return "Source Patrol ID";
		if (item == IntelligenceFilterOption.DESCRIPTION) return "Description";
		if (item == IntelligenceFilterOption.INFORMANTID) return "Informant ID";
		
		if (item instanceof RecievedDateFilter) return "Received Date";
		return null;
	}
}
