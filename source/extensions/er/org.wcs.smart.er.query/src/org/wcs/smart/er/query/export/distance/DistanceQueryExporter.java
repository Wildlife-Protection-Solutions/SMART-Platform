/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.er.query.export.distance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.model.SamplingUnit.State;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.engine.DerbyPagedObservationResult;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.ISamplingUnitResultItem;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.SurveyQueryColumn.FixedColumns;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.common.engine.IColumnInfoProvider;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;
import org.wcs.smart.query.common.model.IColumnAutoConfigQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.importexport.ICsvQueryExporter;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * ER query exporter to support exporting results to 
 * a DISTANCE csv file.  This is a normal csv file with additional
 * rows and columns added.  See ticket https://app.assembla.com/spaces/smart-cs/tickets/2845
 * for details
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class DistanceQueryExporter implements ICsvQueryExporter {

	protected CSVWriter writer;
	protected char delimiter = DEFAULT_DELIMITER;

	public static final String STRATUM_SORT_COLUMN = "stratumsort"; //$NON-NLS-1$
	public static final String STRATUM_COL_KEY = "stratum"; //$NON-NLS-1$
	private static final String SU_COLUMN_KEY_PREFIX = "suatt"; //$NON-NLS-1$

	private DistanceExportHelper helpers;
	
	public DistanceQueryExporter() {
	}

	@Override
	public String getId() {
		return "org.wcs.smart.query.export.simple.csv"; //$NON-NLS-1$

	}

	@Override
	public boolean supportsProjection() {
		return true;
	}

	@Override
	public String getName() {
		return Messages.DistanceQueryExporter_ExporterName;
	}

	@Override
	public String getDefaultExtension() {
		return "csv"; //$NON-NLS-1$
	}

	@Override
	public boolean canExport(Query query) {
		if (SmartDB.getCurrentConservationArea().getIsCcaa()) return false;
		return query.getTypeKey().equalsIgnoreCase(SurveyObservationQuery.KEY);
	}

	private void init(Path file, List<QueryColumn> columns) throws IOException {
		writer = new CSVWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8),
				delimiter, '"', SharedUtils.LINE_SEPARATOR); 
		
		String data[] = new String[columns.size()]; 
		for (int i = 0; i < data.length; i ++){
			data[i] = columns.get(i).getName(); 
		}
		writer.writeNext(data);
	}
	
	protected void finish() throws Exception{
		writer.close();
	}
	
	@Override
	public void export(Query query, IQueryResult results, Path file, HashMap<String, Object> parameters,
			IProgressMonitor monitor) throws Exception {
		
		try {
			exportInternal(query, results, file, parameters, monitor);
		} finally {
			//try to drop temporary table
			if (helpers != null) {
				try (Session session = HibernateManager.openSession()){
					session.beginTransaction();
					session.createNativeMutationQuery("DROP TABLE " + helpers.getDataTable()).executeUpdate(); //$NON-NLS-1$
					session.getTransaction().commit();
				}catch(Throwable t) {
					EcologicalRecordsPlugIn.log(t.getMessage(), t);
				}
			}
		}
	}
	
	private void exportInternal(Query query, IQueryResult results, Path file, HashMap<String, Object> parameters,
			IProgressMonitor monitor) throws Exception {
		SurveyObservationQuery suquery = (SurveyObservationQuery)query;
		SurveyDesign sd = null;
		
		if (suquery.getSurveyDesign() == null) throw new Exception(Messages.DistanceQueryExporter_CrossDesignNotSupported);
		
		//delimiter
		if (parameters.get(DELIMITER_KEY) != null){
			try{
				this.delimiter = (Character) parameters.get(DELIMITER_KEY);
			}catch(Exception ex){}
		}

		//projection
		IProjectionProvider provider = null;
		if (parameters.get(IQueryExporter.PROJECTION_PARAM_KEY) != null){
			final Projection prj = (Projection) parameters.get(IQueryExporter.PROJECTION_PARAM_KEY);
			provider = new IProjectionProvider() {
				@Override
				public Projection getProjection() {
					return prj;
				}
			};
		}
		
		
		// query columns
		SimpleQuery simpleQuery = (SimpleQuery) query;
		List<QueryColumn> columns = simpleQuery.computeQueryColumns(Locale.getDefault(), null, provider);
		boolean isDataFiltering = query instanceof IColumnAutoConfigQuery && results instanceof IColumnInfoProvider && ((IColumnAutoConfigQuery)simpleQuery).isShowDataColumnsOnly();
		for (Iterator<QueryColumn> iterator = columns.iterator(); iterator.hasNext();) {
			QueryColumn column = iterator.next();
			boolean isVisibleColumn = isDataFiltering ? true : column.isVisible();
			if (!isVisibleColumn){
				iterator.remove();
			}
		}
		
		// configure sort columns
		// always sort by sampling unit it
		// if "Stratum" custom attribute column also sort by that column name
		String stratumAttributeKey = null;
		for (QueryColumn qc : columns) {
			if (!qc.getKey().contains(":")) continue; //$NON-NLS-1$
			if (qc.getKey().startsWith(SU_COLUMN_KEY_PREFIX + ":")) { //$NON-NLS-1$
				String[] bit = qc.getKey().split(":"); //$NON-NLS-1$
				if (bit[1].equalsIgnoreCase(STRATUM_COL_KEY)) { 
					stratumAttributeKey = bit[1]; 
					break;
				}
			}
		}
		
		DerbyPagedObservationResult qresults = (DerbyPagedObservationResult)results;
		helpers = new DistanceExportHelper(qresults);
		qresults = helpers.createResultSet(stratumAttributeKey != null);
		
		Set<UUID> foundSu = new HashSet<>();				
		HashMap<UUID, Double> efforts = new HashMap<>();
		HashMap<UUID, SamplingUnit> units = new HashMap<>();
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				sd = SurveyHibernateManager.getInstance().getSurveyDesign(suquery.getSurveyDesign(), session);
				if (sd == null) throw new Exception(MessageFormat.format(Messages.DistanceQueryExporter_SurveyDesignNotFound, suquery.getSurveyDesign()));
				
				//duplicate the results table so we can add more rows/columns
				session.createNativeQuery("CREATE TABLE " + helpers.getDataTable() + " AS SELECT * FROM " + helpers.getSrcDataTable() + " WITH NO DATA", Integer.class).executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				session.createNativeQuery("INSERT INTO " + helpers.getDataTable() + " SELECT * FROM " + helpers.getSrcDataTable(), Integer.class ).executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
				

				//--add missing sampling units to results table--
				//get sampling units
				try(ScrollableResults<byte[]> scroll = session.createNativeQuery("SELECT samplingunit_uuid FROM " + helpers.getDataTable(), byte[].class ).scroll()){ //$NON-NLS-1$
					while(scroll.next()) {
						foundSu.add(UuidUtils.byteToUUID(scroll.get()));
					}
				}
				
				List<SamplingUnit> sunits = SurveyHibernateManager.getInstance().getSamplingUnits(sd, session, State.ACTIVE);
				//add su's that don't exist
				for (SamplingUnit e : sunits) {
					if (!foundSu.contains(e.getUuid())) {
						//add this to to data table
						String sb = "INSERT INTO " + helpers.getDataTable() + " (samplingunit_uuid, samplingunit_id ) VALUES (:suuid, :sid)";  //$NON-NLS-1$//$NON-NLS-2$
						session.createNativeMutationQuery(sb)
							.setParameter("suuid", e.getUuid()) //$NON-NLS-1$
							.setParameter("sid", e.getId()) //$NON-NLS-1$
							.executeUpdate();							
					}
				}
				for (SamplingUnit e : sunits) {
					units.put(e.getUuid(), e);
					for (SamplingUnitAttributeValue a : e.getAttributes()) a.getValueAsString();
				}
				
				//update stratum sort if required
				if (stratumAttributeKey != null) {
					//add a sort column for this attribute
					String sql = "ALTER TABLE " + helpers.getDataTable() + " ADD COLUMN " + STRATUM_SORT_COLUMN + " VARCHAR(3200) ";  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
					session.createNativeMutationQuery(sql).executeUpdate();
					
					for (SamplingUnit e : sunits) {
						units.put(e.getUuid(), e);
						String stratumvalue = null;
						for (SamplingUnitAttributeValue a : e.getAttributes()) {
							if (a.getSamplingUnitAttribute().getKeyId().equals(stratumAttributeKey)) {
								stratumvalue = a.getValueAsString();
								break;
							}
						};
						if (stratumvalue != null) {
							sql = "UPDATE " + helpers.getDataTable() + " SET " + STRATUM_SORT_COLUMN + " = :strvalue WHERE samplingunit_uuid = :uuid "; //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
							session.createNativeMutationQuery(sql)
								.setParameter("strvalue", stratumvalue)  //$NON-NLS-1$
								.setParameter("uuid", e.getUuid()).executeUpdate();  //$NON-NLS-1$
						}
					}
				}
				
				//compute effort
				String sql = "SELECT t FROM Mission m JOIN m.missionDays d JOIN d.tracks t ";  //$NON-NLS-1$
				sql += " WHERE t.samplingUnit in (select s from SamplingUnit s WHERE s.surveyDesign.keyId = :sd) "; //$NON-NLS-1$
				DateFilter df = ((SimpleQuery)query).getDateFilter();
				boolean params = false;
				if (df.getDateFilterOption() != AllDatesFilter.INSTANCE) {
					if (df.getDateFieldOption() == MissionEndDateField.INSTANCE) {
						sql += " AND m.endDate > :date1 and m.endDate <";  //$NON-NLS-1$
						if (df.getDateFilterOption().isEndDateInclusive()) sql += "=";  //$NON-NLS-1$
						sql += " :date2 ";  //$NON-NLS-1$
						params = true;
					}else if (df.getDateFieldOption() == MissionStartDateField.INSTANCE) {
						sql += " AND m.startDate > :date1 and m.startDate <";  //$NON-NLS-1$
						if (df.getDateFilterOption().isEndDateInclusive()) sql += "=";  //$NON-NLS-1$
						sql += " :date2 ";  //$NON-NLS-1$
						params = true;
					}else if (df.getDateFieldOption() == WaypointDateField.INSTANCE) {
						sql += " AND d.date > :date1 and d.date <";  //$NON-NLS-1$
						if (df.getDateFilterOption().isEndDateInclusive()) sql += "=";  //$NON-NLS-1$
						sql += " :date2 ";  //$NON-NLS-1$
						params = true;
					}else {
						throw new Exception(MessageFormat.format(Messages.DistanceQueryExporter_DateNotSupported, df.getDateFieldOption().getGuiName(Locale.getDefault())));
					}	
				}
				
				org.hibernate.query.Query<MissionTrack> q = session.createQuery(sql, MissionTrack.class);
				if (params) {
					q.setParameter("date1",  df.getDateFilterOption().getDates()[0]);  //$NON-NLS-1$
					q.setParameter("date2",  df.getDateFilterOption().getDates()[1]);  //$NON-NLS-1$
				}
				q.setParameter("sd",  sd.getKeyId() ); //$NON-NLS-1$
				List<MissionTrack> tracks = q.list();
				for (MissionTrack t : tracks) {
					if (t.getSamplingUnit() == null) continue;
					
					Double d = efforts.get(t.getSamplingUnit().getUuid());
					if (d == null) d = 0.0;
					d += t.getGeometryLengthKm();
					efforts.put(t.getSamplingUnit().getUuid(), d);
				}
				
				//get count for results
				Integer cnt = session.createNativeQuery("SELECT count(*) from " + helpers.getDataTable(), Integer.class ).uniqueResult();  //$NON-NLS-1$
				qresults.setItemCount(cnt.intValue());
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}
			
		//ADD additional columns to the query output 
		for (int i = 0; i < columns.size(); i ++) {
			if (columns.get(i).getKey().equals(FixedColumns.SAMPLING_UNIT.getKey())) {
				
				//add length
				QueryColumn lengthcolumns = new QueryColumn(Messages.DistanceQueryExporter_LengthColumnName, "su:length", ColumnType.NUMBER) { //$NON-NLS-1$
					@Override
					public Object getValue(IResultItem item) {
						if (item instanceof ISamplingUnitResultItem) {
							ISamplingUnitResultItem ii = (ISamplingUnitResultItem)item;
							SamplingUnit u  = units.get(ii.getSamplingUnitUuid());
							if (u != null && u.getType() == GeometryType.TRANSECT) {
								try {
									return u.getGeometryLengthKm();
								} catch (Exception e) {
									return -1;
								}
							}
						}
						return 0;
					}

					@Override
					public QueryColumn clone() {
						return null;
					}
					
				};
				columns.add(i+1, lengthcolumns);
				
				//add effort
				QueryColumn effortcolumn = new QueryColumn(Messages.DistanceQueryExporter_EffortColumnName, "su:effort", ColumnType.NUMBER) { //$NON-NLS-1$
					@Override
					public Object getValue(IResultItem item) {
						if (item instanceof ISamplingUnitResultItem) {
							ISamplingUnitResultItem ii = (ISamplingUnitResultItem)item;
							SamplingUnit u  = units.get(ii.getSamplingUnitUuid());
							if (u != null && efforts.containsKey(u.getUuid())) return efforts.get(u.getUuid());
						}
						return 0;
					}

					@Override
					public QueryColumn clone() {
						return null;
					}
					
				};
				columns.add(i+1, effortcolumn);
				
				break;
			}
		}
		
		init(file, columns);
		
		//export results to file
		try(IQueryResultSetIterator<?> it = qresults.iterator(IPagedQueryResultSet.MAP_PAGE_SIZE)){
			while(it.hasNext()){
				IResultItem ri = (IResultItem)it.next();
				
				UUID wpuuid = null;
				if (ri instanceof WaypointQueryResultItem) {
					wpuuid = ((WaypointQueryResultItem) ri).getWaypointUuid();
				}
				
				String data[] = new String[columns.size()];
				
				if (wpuuid == null) {
					for (int i = 0; i < data.length; i ++){
						QueryColumn qc = columns.get(i);
						
						data[i] = "";  //$NON-NLS-1$
						if (qc.getKey().startsWith(SU_COLUMN_KEY_PREFIX + ":")) { //$NON-NLS-1$
							SamplingUnit su = units.get(((ISamplingUnitResultItem)ri).getSamplingUnitUuid());
							if (su != null) {
								for (SamplingUnitAttributeValue v : su.getAttributes()) {
									if (v.getSamplingUnitAttribute().getKeyId().equals(qc.getKey().split(":")[1])) { //$NON-NLS-1$
										data[i] = v.getValueAsString();
										break;
									}
								}
							}
						}else if (qc.getKey().equals("su:length") || qc.getKey().contentEquals("su:effort")) { //$NON-NLS-1$ //$NON-NLS-2$
							data[i] = qc.getValueAsString(qc.getValue(ri), Locale.getDefault());
						}else if (qc instanceof SurveyQueryColumn
								&& ((SurveyQueryColumn)qc).getKey().equals(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT.getKey())) {
							data[i] = qc.getValueAsString(qc.getValue(ri), Locale.getDefault());
						}
					}
				}else {
					for (int i = 0; i < data.length; i ++){
						QueryColumn qc = columns.get(i);
						data[i] = qc.getValueAsString(qc.getValue(ri), Locale.getDefault());
					}
				}
				writer.writeNext(data);
	
			}
		}
		
		
		
		finish();
	}

}
