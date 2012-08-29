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
package org.wcs.smart.internal.ca.export;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.ca.export.ICaDataExporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * A conservation area exporter that export
 * the smart.db_att_tree_nodes as this is not
 * mapped by a hibernate entity and not included
 * in the default hibernate exporter.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DataModelExporter implements ICaDataExporter {

	public DataModelExporter() {
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExporter#exportData(org.wcs.smart.ca.export.ICaDataExportEngine, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void exportData(ICaDataExportEngine exportEngine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask("Exporting datamodel tables", 1);
		exportAttTreeNodesTable(exportEngine);
		exportAttAggMapTable(exportEngine);
		exportAggregationTable(exportEngine);
		monitor.worked(1);
	}
	
	private void exportAttTreeNodesTable(ICaDataExportEngine exportEngine) throws Exception{
		String tableName = "smart.dm_att_tree_nodes";
		
		String columns[] = exportEngine.getTableColumns(tableName);
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		for (int i = 0; i < columns.length; i ++){
			query.append("a." + columns[i]);
			if (i != columns.length - 1){
				query.append(", ");
			}
		}
		query.append(" FROM ");
		query.append(tableName + " a join ");
		query.append(HibernateManager.getTableName(Attribute.class));
		query.append(" b on a.attribute_uuid = b.uuid ");
		query.append(" WHERE b.ca_uuid = x''");
		query.append(SmartUtils.encodeHex(exportEngine.getConservationArea().getUuid()));
		query.append("''");
		
		exportEngine.writeTableDefinitionFile(tableName, columns);
		exportEngine.writeQuery(tableName, query.toString());
			
	}

	private void exportAttAggMapTable(ICaDataExportEngine exportEngine) throws Exception{
		String tableName = "smart.dm_att_agg_map";
		
		String columns[] = exportEngine.getTableColumns(tableName);
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		for (int i = 0; i < columns.length; i ++){
			query.append("a." + columns[i]);
			if (i != columns.length - 1){
				query.append(", ");
			}
		}
		query.append(" FROM ");
		query.append(tableName + " a join ");
		query.append(HibernateManager.getTableName(Attribute.class));
		query.append(" b on a.attribute_uuid = b.uuid ");
		query.append(" WHERE b.ca_uuid = x''");
		query.append(SmartUtils.encodeHex(exportEngine.getConservationArea().getUuid()));
		query.append("''");
		
		exportEngine.writeTableDefinitionFile(tableName, columns);
		exportEngine.writeQuery(tableName, query.toString());
	}
	
	private void exportAggregationTable(ICaDataExportEngine exportEngine) throws Exception{
		String tableName = "smart.dm_aggregation";
		
		String columns[] = exportEngine.getTableColumns(tableName);		
		
		//create a query that returns nothing as the aggregation table does not contain ca specific data
		//but is required so dm_att_agg_map dependencies can be resolved
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		for (int i = 0; i < columns.length; i ++){
			query.append(columns[i]);
			if (i != columns.length - 1){
				query.append(", ");
			}
		}
		query.append(" FROM ");
		query.append(tableName);
		query.append(" WHERE false");
		
		exportEngine.writeTableDefinitionFile(tableName, columns);
		exportEngine.writeQuery(tableName, query.toString());
		
	}
}
