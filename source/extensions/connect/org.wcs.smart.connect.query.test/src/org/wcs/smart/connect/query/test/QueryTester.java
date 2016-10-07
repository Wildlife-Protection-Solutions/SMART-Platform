package org.wcs.smart.connect.query.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.ConnectClient;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.model.PasswordAesManager;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.filter.MissionTrackDateField;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.query.export.IntelligenceSummaryCsvExporter;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.intelligence.query.model.ReceivedDateFilter;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.common.importexport.CsvSimpleQueryExporter;
import org.wcs.smart.query.common.importexport.CsvSummaryExporter;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.QueryGridResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.util.UuidUtils;

import au.com.bytecode.opencsv.CSVReader;

public class QueryTester {

	private Session session;
//	private SmartConnectQuery connect;
	
	private SmartConnect connect;
	
	private IStatusRecorder status;
	private boolean cancelRequested = false;
	
	public QueryTester(IStatusRecorder status){
		this.status = status;
	}
	
	private void init() throws Exception{
		cancelRequested = false;
		if (session == null){
			session = HibernateManager.openSession();
			
			ConnectServer cs = (ConnectServer) session.createCriteria(ConnectServer.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.uniqueResult();
			ConnectUser user = (ConnectUser)session.get(ConnectUser.class, SmartDB.getCurrentEmployee().getUuid());
			
//			connect = new SmartConnectQuery(cs, user.getConnectUsername(), PasswordAesManager.getInstance().decryptPassword(user.getConnectPassword(), SmartDB.getPlainTextPassword()));
			connect = SmartConnect.findInstance(cs, user.getConnectUsername(), PasswordAesManager.getInstance().decryptPassword(user.getConnectPassword(), SmartDB.getPlainTextPassword()));
		}
	}
	
	public void cancel(){
		cancelRequested = true;
	}
	private void shutDown(){
		if (session != null){
			session.close();
			session = null;
		}
		if (connect != null){
			connect.close();
			connect = null;
		}
	}
	
	public void testAllQueries() {
		try{
			init();
		}catch (Exception ex){
			status.updateStatus("Could not initalize" + ex.getMessage());
			ex.printStackTrace();
			return;
		}
		try{
			HashMap<UUID, List<QueryEditorInput>> queries = QueryHibernateManager.getInstance().getQueryProxies(session);
			List<QueryEditorInput> allQueries = new ArrayList<QueryEditorInput>();
			for (List<QueryEditorInput> temp : queries.values()){
				allQueries.addAll(temp);
			}
			int i = 1;
			for (QueryEditorInput in : allQueries){
				status.updateStatus("Processing: " + (i++) + " / " + allQueries.size());
				testQuery(in.getUuid(), in.getType(), in.getId() + ": " + in.getName());
				status.updateStatus("");
				if (cancelRequested){
					status.updateStatus("CANCELLED");
					return;
				}
			}
		}catch (Exception ex){
			status.updateStatus("Error: " + ex.getMessage());
			ex.printStackTrace();
		}finally{
			shutDown();
		}
		status.updateStatus("Done.");
	}
	
	public void testQuery(UUID uuid) {
		try{
			init();
		}catch (Exception ex){
			status.updateStatus("Could not initalize" + ex.getMessage());
			ex.printStackTrace();
			return;
		}
		try{
			QueryEditorInput found = null;
			HashMap<UUID, List<QueryEditorInput>> queries = QueryHibernateManager.getInstance().getQueryProxies(session);
			for (List<QueryEditorInput> temp : queries.values()){
				for (QueryEditorInput i : temp){
					if (i.getUuid().equals(uuid)){
						found = i;
						break;
					}
				}
				if (found != null){
					break;
				}
			}
			testQuery(found.getUuid(), found.getType(), found.getId() + ": " + found.getName());
		}catch (Exception ex){
			status.updateStatus("Error: " + ex.getMessage());
			ex.printStackTrace();
		}finally{
			shutDown();
		}
		status.updateStatus("Done.");
	}
	
	
	private void testQuery(UUID uuid, IQueryType queryType, String name) throws Exception{
		status.updateStatus("Running: " + name + " (" + uuid.toString() + ")");
		IDateFieldFilter dateField = WaypointDateField.INSTANCE;
		if (queryType.getKey().equals(PatrolQuery.KEY)){
			dateField = PatrolStartDateField.INSTANCE;
		}else if (queryType.getKey().equals(IntelligenceSummaryQuery.KEY) 
				|| queryType.getKey().equals(IntelligenceRecordQuery.KEY)){
			dateField = ReceivedDateFilter.INSTANCE;
		}else if (queryType.getKey().equals(MissionTrackQuery.KEY)){
			dateField = MissionTrackDateField.INSTANCE;
		}else if (queryType.getKey().equals(MissionQuery.KEY)){
			dateField = MissionStartDateField.INSTANCE;
		}
		
		DateFilter dateFilter = new DateFilter(dateField, AllDatesFilter.INSTANCE);
		
		Query query = QueryHibernateManager.getInstance().findQuery(session, uuid, queryType);
		query.setDateFilter(dateFilter);
		
		
		IQueryResult desktopResult = null;
		try{
			desktopResult = QueryExecutor.INSTANCE.executeQuery(query, session, new NullProgressMonitor());
		}catch (Exception ex){
			status.updateStatus("Desktop Failed - " + ex.getMessage());
			ex.printStackTrace();
		}
		
		List<String[]> connectResults = null;
		try{
			connectResults = runQueryConnect(query, dateFilter);
		}catch (Exception ex){
			status.updateStatus("Server Failter - " + ex.getMessage());
			ex.printStackTrace();
		}
		
		if (desktopResult != null && connectResults != null){
			compareResults(query, desktopResult, connectResults);
		}
	}
	
	private void compareResults(Query query, IQueryResult desktop, List<String[]> server){
		if (desktop instanceof GridQueryResult){
			compareResults((GridQueryResult)desktop, server);
		}else if (query instanceof SimpleQuery){
			try{
				compareResults(desktop,  (SimpleQuery)query, server);
			}catch (Exception ex){
				ex.printStackTrace();
				status.updateStatus("error exception thrown: " + ex.getMessage());	
			}
		}else if (query instanceof SummaryQuery){
			try{
				compareResults(desktop, (SummaryQuery)query, server);
			}catch (Exception ex){
				ex.printStackTrace();
				status.updateStatus("error exception thrown: " + ex.getMessage());
			}
		}else if (query instanceof IntelligenceSummaryQuery){
			try{
				compareResults(desktop, (IntelligenceSummaryQuery)query, server);
			}catch (Exception ex){
				ex.printStackTrace();
				status.updateStatus("error exception thrown: " + ex.getMessage());
			}
		}else{
			status.updateStatus("error not comparator found:" + server.size());
		}
	}
	
	private void compareCsv(File f, List<String[]> server) throws FileNotFoundException, IOException{
		try(CSVReader reader = new CSVReader(new FileReader(f))){
			String[] line = reader.readNext();	//skip header line
			int lineCnt = 1;
			while((line = reader.readNext()) != null){
				if (lineCnt == 1){
					if (line.length != server.get(0).length){
						status.updateStatus("ERROR: different number of columns");
						return;
					}
				}
				lineCnt ++;
				//find connect result that matches
				boolean found = false;
				for (String[] serverRow : server){
					boolean allMatch = true;
					for (int i = 0; i < serverRow.length; i ++){
						if (!line[i].equals(serverRow[i])){
							if (!compareDouble(line[i], serverRow[i])){
								allMatch = false;
								break;
							}
						}
					}
					if (allMatch){
						found = true;
						break;
					}
				}
				//once matched we should remove so we don't match again
				if (!found){
					status.updateStatus("ERROR: now matching row from for desktop result result " + lineCnt + " " + Arrays.toString(line) );
				}
			}
		}
	}
	private void compareResults(IQueryResult desktop, SimpleQuery query, List<String[]> server) throws Exception{
		CsvSimpleQueryExporter ex = new CsvSimpleQueryExporter();
		File f = File.createTempFile("smart_querytester", ".csv");
		
		if (!ex.canExport(query)){
			status.updateStatus("ERROR: cannot export query results to csv for comparison");
			return;
		}
		
		ex.export(query,  desktop, f, new HashMap<String, Object>(), new NullProgressMonitor());
		compareCsv(f, server);
		f.delete();
		
	}
	
	
	private void compareResults(IQueryResult desktop, SummaryQuery query, List<String[]> server) throws Exception{
		
		CsvSummaryExporter ex = new CsvSummaryExporter();
		File f = File.createTempFile("smart_querytester", ".csv");
		
		if (!ex.canExport(query)){
			status.updateStatus("ERROR: cannot export query results to csv for comparison");
			return;
		}
		
		ex.export(query,  desktop, f, new HashMap<String, Object>(), new NullProgressMonitor());
		compareCsv(f, server);
		f.delete();
		
	}

	private void compareResults(IQueryResult desktop, IntelligenceSummaryQuery query, List<String[]> server) throws Exception{
		
		IntelligenceSummaryCsvExporter ex = new IntelligenceSummaryCsvExporter();
		File f = File.createTempFile("smart_querytester", ".csv");
		
		if (!ex.canExport(query)){
			status.updateStatus("ERROR: cannot export query results to csv for comparison");
			return;
		}
		
		ex.export(query,  desktop, f, new HashMap<String, Object>(), new NullProgressMonitor());
		compareCsv(f, server);
		f.delete();
		
	}

	private boolean compareDouble(String d1, String d2){
		try{
			Double localValue = Double.valueOf(d1);
			Double serverValue = Double.valueOf(d2);
		
			if (localValue.toString().equals(d1)){
				if (Math.abs(localValue - serverValue) < .00000001){
					//assume same
					return true;
				}else if (Math.abs(localValue - serverValue) < .001){
					status.updateStatus("potential error: " + localValue + " - " + serverValue + " = " + (localValue - serverValue));
					return true;
				}
			}	
		}catch (Exception ex){
			
		}
		return false;
	}
	private void compareResults(GridQueryResult desktop, List<String[]> server){
		Collection<QueryGridResultItem> items = desktop.getData();
		if (items.size() != server.size()-1){ //server returns header row; desktop does not
			status.updateStatus("ERROR: Invalid Number of Rows.  Desktop = " + items.size() + "; Server = " + server.size());
			return;
		}
		if (server.isEmpty()) return;
		//check each row
		if (server.get(0).length != 3){
			status.updateStatus("ERROR: Server did not return the correct number of columns for gridded query (expected 3)");
			return;
		}
		for (QueryGridResultItem i : items){
			
			String x = String.valueOf(i.getTileX());
			String y = String.valueOf(i.getTileY());
			String value = String.valueOf(i.getValue());
			
			boolean found = false;
			for (String[] row : server){
				if (row[0].equals(x) && row[1].equals(y)){
					found = true;
					if (!compareDouble(row[2], value)){
						status.updateStatus("ERROR: Different values computed (x,y, desktop, server) (" + x + "," + y + "," + value + "," + row[2] + ")");			
					}
					break;
				}
			}
			if (!found){
				status.updateStatus("ERROR: No server value for (x,y) (" + x + "," + y + ")");
			}
		}
		status.updateStatus("ok: " + server.size());
		
	}
	private List<String[]> runQueryConnect(Query query, DateFilter dFilter) throws Exception{
		return executeQuery(query.getUuid(), dFilter);
	}
	
	public static interface IStatusRecorder{
		void updateStatus(String newInfo);
	}
	
	/**
	 * Gets a list of Conservation Areas on a connect
	 * server.
	 * 
	 * @return List of ConservationAreaInfo 
	 */
	public List<String[]> executeQuery(UUID queryUuid, DateFilter dFilter) throws Exception{
		
		ResteasyWebTarget target = connect.getClient().target(connect.getServer().getServerUrl() + SmartConnect.API_URL);
		
		QueryConnectClient simple = target.proxy(QueryConnectClient.class);
		Response r = null;
		try{
			r = simple.getQueryResults(UuidUtils.uuidToString(queryUuid), "csv", null, null, dFilter.getDateFieldOption().getKey(), ",");
			try(CSVReader reader = new CSVReader(new InputStreamReader(r.readEntity(InputStream.class)))){
				return reader.readAll();
			}
		}catch (NotFoundException ex){
			return null;
		}finally{
			if (r != null) r.close();
		}
	}
	
}
