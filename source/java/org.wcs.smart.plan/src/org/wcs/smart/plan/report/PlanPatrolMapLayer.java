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
package org.wcs.smart.plan.report;

import java.util.Collections;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.plan.report.oda.PlanPatrolQuery;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;

/**
 * Converts Patrol Plan Query into a map layer
 * for adding to smart plan map. 
 * 
 * @author Emily
 *
 */
public class PlanPatrolMapLayer implements IBirtMapLayerManager {
	
	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		//only support queries without any query strings
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(PlanPatrolQuery.SMART_DATASET_TYPE)) {
			String queryText = odaHandle.getQueryText();
			if (queryText.length() == 0){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle)
			throws Exception {
		return Collections.singletonList(new MapLayerInfo(null, null, LayerType.MULTILINE, PatrolQueryResultItem.TRACK_GEOMCOLUMN_KEY));
	}

	/**
	 * Creates the default style for the patrol plan map layer
	 * @return
	 */
	public static Style createDefaultTrackStyle(){
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		LineSymbolizer ls = sf.createLineSymbolizer();
		ls.setStroke(sf.createStroke(ff.literal("#0000FF"), ff.literal(1))); //$NON-NLS-1$
		    	
		FeatureTypeStyle fts = sf.createFeatureTypeStyle();
		    	
		Style style = sf.createStyle();
		style.featureTypeStyles().add(fts);
		    	
		Rule r= sf.createRule();
		fts.rules().add(r);
		r.symbolizers().add(ls);
		    
		return style;
	}
}
