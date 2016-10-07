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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IDataQueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.report.data.adapter.api.DataAdapterUtil;
import org.eclipse.birt.report.engine.extension.ReportItemQueryBase;
import org.eclipse.birt.report.model.api.ColumnHintHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.ResultSetColumnHandle;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;
import org.eclipse.birt.report.model.elements.interfaces.IDataSetModel;
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

	private IDataQueryDefinition processChildQuery(LayerItem layer) throws BirtException{

		IBaseQueryDefinition def =  (IBaseQueryDefinition)context.createQuery(null, layer.getHandle())[0];
		
		//configure bindings on-the-fly to reduce the number of problems with changing
		//datamodels; here we also update the geometry column name to use the alias if provided
		Iterator<?> it = layer.getHandle().getDataSet().columnHintsIterator();
		HashMap<String, String> name2Expression = new HashMap<String, String>();
		while(it.hasNext()){
			Object x = it.next();
			if (x instanceof ColumnHintHandle){
				ColumnHintHandle hh = (ColumnHintHandle) x;
				if (hh.getColumnName().equals(layer.getGeometryColumn())){
					if (hh.getAlias() != null){
						layer.setGeometryColumn(hh.getAlias());
					}
				}
				name2Expression.put(hh.getColumnName(), hh.getAlias());
			}
		}
		HashSet<String> names = new HashSet<String>();
		PropertyHandle resultSet = ((OdaDataSetHandle)layer.getHandle().getDataSet()).getPropertyHandle(IDataSetModel.RESULT_SET_PROP);
		 if (resultSet.getListValue() != null) {
			for (int i=0; i < resultSet.getListValue().size(); i++) {
				ResultSetColumnHandle col =(ResultSetColumnHandle)resultSet.getAt(i);
				String column = col.getColumnName();
				String name = name2Expression.get(column);
				if (names.contains(name)){
					name = column;
				}
				names.add(name);
				if (name == null) name = column;
				Binding bind = new Binding( name,new ScriptExpression( ExpressionUtil.createJSDataSetRowExpression( column ) ) ) ;
				bind.setDataType(DataAdapterUtil.adaptModelDataType(col.getDataType()));
				def.addBinding( bind);
			}
		}
		
		 //create layer definition
		MapQueryDefinition wrapper = new MapQueryDefinition(layer, def);	
		MapLayerInfo info = new MapLayerInfo(layer.getLayerName(), 
				layer.getLayerStyle(), layer.getLayerType(), layer.getGeometryColumn());
		wrapper.setMapInfo(info);
		
		return wrapper;
		
	}

}
