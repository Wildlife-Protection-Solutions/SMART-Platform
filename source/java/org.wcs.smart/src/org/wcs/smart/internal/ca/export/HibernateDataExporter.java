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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.ca.export.ICaDataExporter;
import org.wcs.smart.ca.export.TableInfo;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.internal.Messages;

/**
 * Conservation area data exporter that will export
 * all tables with an associated hibernate entity.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class HibernateDataExporter implements ICaDataExporter {

	public HibernateDataExporter() {
	}

	@Override
	public int getRunLevel() {
		return 0;
	}

	@Override
	public void exportData(ICaDataExportEngine exportEngine, IProgressMonitor monitor) throws Exception {
		/* export all mapped tables */
		List<TableInfo> info = HibernateManager.getTableInformation();
		monitor.beginTask(Messages.HibernateDataExporter_Progress_ExportMappedTables, info.size());
		for (TableInfo in : info) {
			if (exportEngine.getConservationArea().getIsCcaa() && !SmartHibernateManager.supportsCcaa(in.getClazz())){
				continue;
			}
			if (monitor.isCanceled()){
				return;
			}
			monitor.subTask(Messages.HibernateDataExporter_SubProgress_ProcessingTable + in.getTableName());
			monitor.worked(1);
			
			if (in.getCaPropertyName() != null) {
				String[] columns = exportEngine.getTableColumns(in.getTableName());
				exportEngine.writeTableDefinitionFile(in.getTableName(), in.getClazz().getSimpleName(), columns);
				exportEngine.exportTableData(in.getTableName(), in.getClazz().getSimpleName(), columns, in.getCaPropertyName());
			} else {
				
				String hqlQuery = SmartHibernateManager.getHqlExportQuery(in.getClazz());
				if (hqlQuery == null || hqlQuery.trim().length() == 0){
					//skip this table as we can't figure out what conservation area data it is associated with
					continue;
				}
				
				String[] columns = exportEngine.getTableColumns(in.getTableName());
				exportEngine.writeTableDefinitionFile(in.getTableName(), in.getClazz().getSimpleName(), columns);
				exportEngine.writeHibernateQuery(in.getTableName(), in.getClazz().getSimpleName(), columns, hqlQuery);				
			}
		}
		monitor.done();
	}

	/**
	 * Hibernate table support is determined for each table
	 * by the extension point.
	 * @returns true
	 */
	@Override
	public boolean supportsCcaa() {
		return true;
	}
}
