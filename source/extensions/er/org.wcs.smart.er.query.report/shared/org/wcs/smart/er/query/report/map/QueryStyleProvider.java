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
package org.wcs.smart.er.query.report.map;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.styling.Style;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.style.sld.SLDContent;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.ParsedQuery;
import org.wcs.smart.data.oda.smart.impl.table.SmartTableQuery;
import org.wcs.smart.er.map.SamplingUnitStyleProvider;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.er.query.report.table.SurveySamplingUnitTable;
import org.wcs.smart.report.birt.map.AbstractQueryStyleProvider;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.udig.style.StyleManager;

import jakarta.persistence.Tuple;

/**
 * Query style provider for survey queries.
 * 
 * @author Emily
 *
 */
public class QueryStyleProvider extends AbstractQueryStyleProvider{

	@Override
	public StyleBlackboard getStyle(String extensionId, String queryText, MapLayerInfo info, Session s) {
		if (extensionId.equals(AbstractSmartBirtQuery.SMART_DATASET_TYPE)){
		
			ParsedQuery pquery = AbstractSmartBirtQuery.parseQueryText(queryText);
			return getStyle(pquery.getType(), pquery.getUuid(), info, s);
			
		}else if (extensionId.equals(SmartTableQuery.SMART_DATASET_TYPE)){
			Style style = null;
			if (queryText.toUpperCase(Locale.ROOT).startsWith(SurveySamplingUnitTable.SU_PREFIX + ":" + GeometryType.TRANSECT.name())){ //$NON-NLS-1$
				style = SamplingUnitStyleProvider.createDefaultStyle(GeometryType.TRANSECT);
			}
			if (queryText.toUpperCase(Locale.ROOT).startsWith(SurveySamplingUnitTable.SU_PREFIX + ":" + GeometryType.PLOT.name())){ //$NON-NLS-1$
				style = SamplingUnitStyleProvider.createDefaultStyle(GeometryType.PLOT);
			}
			if (style != null){
				StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
				sb.put(SLDContent.ID, style);
				return sb;
			}
		}
		return null;
	}
	
	@Override
	public StyleBlackboard getStyle(String queryType, UUID queryUuid, MapLayerInfo info, Session s) {
		if (queryUuid == null) return null;
		String tableName = null;
		
		if (queryType.equals(SurveyObservationQuery.KEY)){
			tableName = SurveyObservationQuery.class.getSimpleName(); 
		}else if (queryType.equals(SurveyWaypointQuery.KEY)){
			tableName = SurveyWaypointQuery.class.getSimpleName(); 
		}else if (queryType.equals(MissionQuery.KEY)){	
			tableName = MissionQuery.class.getSimpleName(); 
		}else if (queryType.equals(MissionTrackQuery.KEY)){	
			tableName = MissionTrackQuery.class.getSimpleName(); 
		}else if (queryType.equals(SurveyGriddedQuery.KEY)){	
			tableName = SurveyGriddedQuery.class.getSimpleName(); 
		}else{
			return null;
		}
		
		Query<Tuple> query = s.createQuery("SELECT style, conservationArea FROM " + tableName + " WHERE uuid = :uuid", Tuple.class); //$NON-NLS-1$ //$NON-NLS-2$
		query.setParameter("uuid", queryUuid); //$NON-NLS-1$
		List<Tuple> results = query.list();
		if (results.size() == 0 || results.get(0) == null) return null;
		
		String stylemap = (String) results.get(0).get(0);
		ConservationArea ca = (ConservationArea) results.get(0).get(1);
		
		try {
			StyleBlackboard x = StyleManager.INSTANCE.fromStringMap(stylemap).get(info.getGeometryColumnId());
			if (x != null) return x;
		} catch (Exception e) {
			Logger.getLogger(QueryStyleProvider.class.getName()).log(Level.WARNING, "Error parsing SMART Query style.", e); //$NON-NLS-1$
		}
		
		return super.findDataModelAttributeStyle(s, ca, info.getGeometryColumnId());
	}
}
