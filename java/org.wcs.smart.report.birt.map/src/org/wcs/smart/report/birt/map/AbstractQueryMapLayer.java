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
package org.wcs.smart.report.birt.map;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.SmartQuery;

/**
 * Abstract map layer for linking a SMART query to a BIRT map layer.  This
 * implements default behaviour for common SMART queries.
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryMapLayer implements IBirtMapLayerManager {
	
	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)) {
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(AbstractSmartBirtQuery.SMART_DATASET_TYPE)) {
			String queryText = odaHandle.getQueryText();
			String queryTypeKey = queryText.split(":")[0]; //$NON-NLS-1$
			if (canAddToMap(queryTypeKey)){
				return true;
			}
		}
		return false;
	}
	
	public abstract boolean canAddToMap(String queryTypeKey);
	
	public abstract List<MapLayerInfo> getGeometryOptions(String queryTypeKey);
	
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle) throws Exception{
		
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(SmartQuery.SMART_DATASET_TYPE)) {
			String queryText = odaHandle.getQueryText();
			String queryTypeKey = queryText.split(":")[0]; //$NON-NLS-1$
			if (canAddToMap(queryTypeKey)){
				return getGeometryOptions(queryTypeKey);
			}
		}
		return new ArrayList<MapLayerInfo>();
	}
	
//	
//	@Override
//	public StyleBlackboard getDefaultStyle(DataSetHandle handle, IGeoResource resource) {
//		if (!(handle instanceof OdaDataSetHandle)){
//			return null;
//		}
//		
//		String queryText = ((OdaDataSetHandle)handle).getQueryText();
//		UUID quuid = null;
//		try {
//			quuid = UuidUtils.stringToUuid(queryText.split(":")[1]); //$NON-NLS-1$
//		} catch (Exception e) {
//			Activator.log(e.getMessage(), e);
//			return null;
//		}
//		
//		String queryType = queryText.split(":")[0]; //$NON-NLS-1$
//		IQueryType qtype = QueryTypeManager.INSTANCE.findQueryType(queryType);
//		/* for historic support */
//		if (qtype == null) {
//			qtype = QueryTypeManager.INSTANCE.findDeprecatedQueryType(queryType);
//		}
//		if (qtype == null) return null;
//		
//		if (!StyledQuery.class.isAssignableFrom(qtype.getHibernateClass())){
//			return null;
//		}
//		//load query and see if we have a style for the query
//		StyledQuery sq = (StyledQuery) getQuery(quuid, qtype);
//		if (sq != null && sq.getStyle() != null){
//			String key = null;
//			if (sq instanceof GriddedQuery){
//				key = "raster"; //$NON-NLS-1$
//			}else{
//				key = resource.getIdentifier().getRef();
//				
//			}
//			try {
//				return StyleManager.INSTANCE.fromStringMap(sq.getStyle()).get(key);
//			} catch (Exception e) {
//				Activator.log(e.getMessage(), e);
//				return null;
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * Gets the query associated with the layer.  
//	 * 
//	 */
//	protected Query getQuery(UUID quuid, IQueryType qtype){
//		//do not close session as assume it is managed by SmartConnection is BIRT report
//		Session session = HibernateManager.openSession();
//		return QueryHibernateManager.getInstance().findQuery(session,quuid, qtype);
//	}
	
	
	
	

}
