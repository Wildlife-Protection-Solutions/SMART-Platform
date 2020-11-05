/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.er.query.export.distance;

import org.wcs.smart.er.query.engine.DerbyObservationEngine;
import org.wcs.smart.er.query.engine.DerbyPagedObservationResult;

/**
 * Tools to support the Distance query exporter.
 *
 * @author Emily
 * @since 7.0.0
 *
 */
public class DistanceExportHelper {
	
	private String originalTable = null;
	private DerbyObservationEngine engine;
	
	public DistanceExportHelper(DerbyPagedObservationResult result) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException {
		originalTable = result.getResultsTable();
		engine = (DerbyObservationEngine) result.getEngine();
	}
	

	public String getSrcDataTable() {
		return originalTable;
	}

	public String getDataTable() {
		return originalTable + "_edistance"; //$NON-NLS-1$
	}
	
	public DerbyPagedObservationResult createResultSet(boolean orderbystratum) {
		return new DerbyPagedObservationResult(engine) {
			
			@Override
			public String getResultsTable() {
				return DistanceExportHelper.this.getDataTable();
			}
			
			protected String buildSortSql() {
				//always sort by stratum, and sampling unit id
				StringBuilder sb = new StringBuilder();
				sb.append(" ORDER BY "); //$NON-NLS-1$
				if (orderbystratum) {
					sb.append(DistanceQueryExporter.STRATUM_SORT_COLUMN);
					sb.append("," ); //$NON-NLS-1$
				}
				sb.append("samplingunit_id"); //$NON-NLS-1$
				return sb.toString();
			}
		};
	}

}
