package org.wcs.smart.data.oda.smart.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.report.engine.extension.IBaseResultSet;
import org.eclipse.birt.report.engine.extension.IExecutorContext;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.elements.structures.OdaResultSetColumn;
import org.eclipse.birt.report.model.elements.interfaces.IDataSetModel;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.wcs.smart.data.oda.smart.query.common.GriddedQueryResultSetMetadata;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.report.birt.map.IRasterCreator;
import org.wcs.smart.report.birt.map.execute.raster.BirtRasterBuilder;

public class GriddedQueryRasterCreator implements IRasterCreator{

	private Double minValue = null;
	private Double maxValue = null;
	private List<Path> cleanUp =  new ArrayList<Path>();
	
	private GriddedQueryResultSetMetadata metadata;
	
	@Override
	public Path createRaster(IExecutorContext context, String datasetId, IBaseResultSet qresult) throws Exception {
		Path f = null;
		//metadata should be configured by canprocess function
		if (metadata != null){
			//build the raster
			BirtRasterBuilder builder = new BirtRasterBuilder(metadata.getCoordinateReferenceSystem(), 
					metadata.getOrigin(), metadata.getCellSize(), metadata.getXColumnIndex(), 
					metadata.getYColumnIndex(), metadata.getValueColumnIndex());
		
			builder.buildRaster((IQueryResults)qresult.getQueryResults());
			f = builder.getFileImage();
			cleanUp.addAll(builder.getAllFiles());
							
			minValue = builder.getMinValue();
			maxValue = builder.getMaxValue();
		}
		return f;
	}

	public GriddedQueryResultSetMetadata getMetadata() {
		return this.metadata;
	}
	@Override
	public double getRasterMinValue() {
		return minValue;
	}

	@Override
	public double getRasterMaxValue() {
		return maxValue;
	}

	@Override
	public List<Path> getFilesToCleanUp() {
		return cleanUp;
	}

	@Override
	public boolean canProcess(IExecutorContext context, String datasetId, String queryText, DataSetHandle handle) throws Exception {
		if (!datasetId.equals(AbstractSmartBirtQuery.SMART_DATASET_TYPE)) return false;
		SmartConnection connection = (SmartConnection) context.getAppContext().get(SmartConnection.class.getCanonicalName());
		IQuery tmp = connection.newQuery(datasetId);
		tmp.prepare(queryText);
		IResultSetMetaData md = tmp.getMetaData();
		if (md instanceof GriddedQueryResultSetMetadata){
			metadata = (GriddedQueryResultSetMetadata) md;
			
			//https://app.assembla.com/spaces/smart-cs/tickets/3552		
			@SuppressWarnings("unchecked")
			ArrayList<OdaResultSetColumn> resultset = (ArrayList<OdaResultSetColumn>) handle.getProperty(IDataSetModel.RESULT_SET_PROP);
			int ycol = 1;
			int xcol = 2;
			int valcol = 3;
			for (OdaResultSetColumn c : resultset) {
				if (c.getColumnName().equals(GridQueryColumn.GridColumns.TILE_Y.getKey())) {
					ycol = c.getPosition();
				}else if (c.getColumnName().equals(GridQueryColumn.GridColumns.TILE_X.getKey())) {
					xcol = c.getPosition();
				}else if (c.getColumnName().equals(GridQueryColumn.GridColumns.VALUE.getKey())) {
					valcol = c.getPosition();		
				}
			}			
			((GriddedQueryResultSetMetadata)metadata).setColumnPositions(xcol, ycol, valcol); 			
			return true;
		}
		return false;
	}

}
