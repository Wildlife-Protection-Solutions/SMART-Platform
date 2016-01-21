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
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.UuidUtils;

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
	
	@Override
	public int getRunLevel() {
		return 0;
	}

	/**
	 * @see org.wcs.smart.ca.export.ICaDataExporter#exportData(org.wcs.smart.ca.export.ICaDataExportEngine, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void exportData(ICaDataExportEngine exportEngine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.DataModelExporter_Progress_DataModelTables, 1);
		exportAttAggMapTable(exportEngine);
		exportAggregationTable(exportEngine);
		monitor.worked(1);
		monitor.done();
	}

	private void exportAttAggMapTable(ICaDataExportEngine exportEngine) throws Exception{
		String tableName = "smart.dm_att_agg_map"; //$NON-NLS-1$
		
		String columns[] = exportEngine.getTableColumns(tableName);
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT "); //$NON-NLS-1$
		for (int i = 0; i < columns.length; i ++){
			query.append("a." + columns[i]); //$NON-NLS-1$
			if (i != columns.length - 1){
				query.append(", "); //$NON-NLS-1$
			}
		}
		query.append(" FROM "); //$NON-NLS-1$
		query.append(tableName + " a join "); //$NON-NLS-1$
		query.append(HibernateManager.getTableName(Attribute.class));
		query.append(" b on a.attribute_uuid = b.uuid "); //$NON-NLS-1$
		query.append(" WHERE b.ca_uuid = x''"); //$NON-NLS-1$
		query.append(UuidUtils.uuidToString(exportEngine.getConservationArea().getUuid()));
		query.append("''"); //$NON-NLS-1$
		
		String hibernateClass = "AttributeAggregation"; //$NON-NLS-1$
		exportEngine.writeTableDefinitionFile(tableName, hibernateClass, columns);
		exportEngine.writeQuery(tableName + "." + hibernateClass, query.toString()); //$NON-NLS-1$
	}
	
	private void exportAggregationTable(ICaDataExportEngine exportEngine) throws Exception{
		String tableName = "smart.dm_aggregation"; //$NON-NLS-1$
		
		String columns[] = exportEngine.getTableColumns(tableName);		
		
		//create a query that returns nothing as the aggregation table does not contain ca specific data
		//but is required so dm_att_agg_map dependencies can be resolved
		StringBuilder query = new StringBuilder();
		query.append("SELECT "); //$NON-NLS-1$
		for (int i = 0; i < columns.length; i ++){
			query.append(columns[i]);
			if (i != columns.length - 1){
				query.append(", "); //$NON-NLS-1$
			}
		}
		query.append(" FROM "); //$NON-NLS-1$
		query.append(tableName);
		query.append(" WHERE false"); //$NON-NLS-1$
		
		String hibernateClass = "Aggregation"; //$NON-NLS-1$
		exportEngine.writeTableDefinitionFile(tableName, hibernateClass, columns);
		exportEngine.writeQuery(tableName + "." + hibernateClass, query.toString()); //$NON-NLS-1$
		
	}

	/**
	 * CCAA do not have data models; therefore we having to export here.
	 * 
	 * @return false
	 */
	@Override
	public boolean supportsCcaa() {
		return false;
	}
}
