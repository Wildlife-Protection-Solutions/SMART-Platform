package org.wcs.smart.data.oda.smart.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.report.engine.extension.IBaseResultSet;
import org.eclipse.birt.report.engine.extension.IExecutorContext;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.wcs.smart.data.oda.smart.query.common.GriddedQueryResultSetMetadata;
import org.wcs.smart.report.birt.map.IRasterCreator;
import org.wcs.smart.report.birt.map.execute.raster.BirtRasterBuilder;

public class GriddedQueryRasterCreator implements IRasterCreator{

	private Double minValue = null;
	private Double maxValue = null;
	private List<File> cleanUp =  new ArrayList<File>();
	
	private GriddedQueryResultSetMetadata metadata;
	
	@Override
	public File createRaster(IExecutorContext context, String datasetId, IBaseResultSet qresult) throws Exception {
		File f = null;
		//metadata should be configured by canprocess function
		if (metadata != null){
			//build the raster
			BirtRasterBuilder builder = new BirtRasterBuilder(metadata.getCoordinateReferenceSystem(), 
					metadata.getOrigin(), metadata.getCellSize(), metadata.getXColumn(), 
					metadata.getYColumn(), metadata.getValueColumn());
		
			builder.buildRaster((IQueryResults)qresult.getQueryResults());
			f = builder.getFileImage();
			cleanUp.addAll(builder.getAllFiles());
							
			minValue = builder.getMinValue();
			maxValue = builder.getMaxValue();
		}
		return f;
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
	public List<File> getFilesToCleanUp() {
		return cleanUp;
	}

	@Override
	public boolean canProcess(IExecutorContext context, String datasetId, String queryText) throws Exception {
		if (!datasetId.equals(AbstractSmartBirtQuery.SMART_DATASET_TYPE)) return false;
		SmartConnection connection = (SmartConnection) context.getAppContext().get(SmartConnection.class.getCanonicalName());
		IQuery tmp = connection.newQuery(datasetId);
		tmp.prepare(queryText);
		IResultSetMetaData md = tmp.getMetaData();
		if (md instanceof GriddedQueryResultSetMetadata){
			metadata = (GriddedQueryResultSetMetadata) md;
			return true;
		}
		return false;
	}

}
