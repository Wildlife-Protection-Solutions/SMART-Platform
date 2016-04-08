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
package org.wcs.smart.report.birt.map.execute;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IDataQueryDefinition;
import org.eclipse.birt.report.engine.extension.ReportItemQueryBase;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.item.LayerItem;
import org.wcs.smart.report.birt.map.item.SmartMapItem;

public class MapQuery extends ReportItemQueryBase {

	private SmartMapItem crosstabItem;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.engine.extension.ReportItemQueryBase#setModelObject
	 * (org.eclipse.birt.report.model.api.ExtendedItemHandle)
	 */
	public void setModelObject(ExtendedItemHandle modelHandle) {
		super.setModelObject(modelHandle);

		try {
			crosstabItem = (SmartMapItem) modelHandle.getReportItem();
		} catch (ExtendedElementException e) {
			e.printStackTrace();
			crosstabItem = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.birt.report.engine.extension.ReportItemQueryBase#
	 * createReportQueries
	 * (org.eclipse.birt.data.engine.api.IDataQueryDefinition)
	 */
	public IDataQueryDefinition[] createReportQueries(
			IDataQueryDefinition parent) throws BirtException {
		if (crosstabItem == null) {
			throw new BirtException("Invalid Map Item"); //$NON-NLS-1$
		}
		
		// build child element query
		if (context != null) {
			// process grandtotal header
			IDataQueryDefinition[] results = new IDataQueryDefinition[crosstabItem.getLayersProperty().getContentCount()];
			// process layers
			for ( int i = 0; i < crosstabItem.getLayersProperty().getContentCount(); i++ ){
				LayerItem layer = crosstabItem.getLayer(i);
				results[i] = processChildQuery(layer);
			}
			return results;
		}
		return new IDataQueryDefinition[]{};
	}

	private IDataQueryDefinition processChildQuery(LayerItem layer) {
		IBaseQueryDefinition def =  (IBaseQueryDefinition)context.createQuery(null, layer.getHandle())[0];
		MapQueryDefinition wrapper = new MapQueryDefinition(layer, def);	
		MapLayerInfo info = new MapLayerInfo(layer.getLayerName(), 
				layer.getLayerStyles(), layer.getLayerType(), layer.getGeometryColumn());
		wrapper.setMapInfo(info);
		return wrapper;
		
	}

}
