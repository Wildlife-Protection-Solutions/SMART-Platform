package org.wcs.smart.asset.query.map.geotools;

import java.io.IOException;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;

public class QueryFeatureSource extends ContentFeatureSource {

	private QueryDataSource dataSource;
	
	
	
	public QueryFeatureSource(ContentEntry entry, QueryDataSource dataSource) {
		super(entry, null);
		this.dataSource = dataSource;		
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {		
		String typeName = entry.getTypeName();
		try {
			if (typeName.equals(QueryDataSource.WAYPOINT_TYPE)) {
				return createWaypointSchema();
			} 
		}catch(SchemaException ex){
			throw new IOException(Messages.QueryFeatureSource_SchemaError + ex.getLocalizedMessage(), ex);
		}
		return null;
	}

	@Override
	protected ReferencedEnvelope getBoundsInternal(Query arg0)
			throws IOException {
		return null;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		if (dataSource.getQuery() instanceof IPagedQuery){
			IQueryResult cachedResults = dataSource.getQuery().getCachedResults();
			if (cachedResults instanceof IPagedQueryResultSet) {
				return ((IPagedQueryResultSet)cachedResults).getItemCount(); 
			}
		}
		return -1;
	}

	/**
	 * Creates the simple feature type for the query
	 * 
	 * @return the simple feature type for the query
	 * 
	 * @throws SchemaException
	 */
	private SimpleFeatureType createWaypointSchema() throws SchemaException{
		SimpleFeatureType type =  DataUtilities.createType("smart." + QueryDataSource.WAYPOINT_TYPE, getFeatureSchemaDef(dataSource.getColumns(), true, false)); //$NON-NLS-1$
		return type;
	}
	
	
	public static String getFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime, boolean forShape){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:Point:srid=4326"); //$NON-NLS-1$
		sb.append(",fid:String"); //$NON-NLS-1$
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, forShape));
		return sb.toString();
	}
	
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		if (dataSource.getQuery() instanceof SimpleQuery) {
			return new QueryFeatureReader((SimpleQuery)dataSource.getQuery(), getSchema(), dataSource.getColumns());
		}else if (dataSource.getQuery() instanceof AssetSummaryQuery) {
			return new SummaryQueryFeatureReader((AssetSummaryQuery)dataSource.getQuery(), getSchema(), dataSource.getColumns());
		}
		return null;
	}

}

