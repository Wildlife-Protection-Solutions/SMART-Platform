package org.wcs.smart.connect.downloader.ca;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.hibernate.SmartTable;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.util.UuidUtils;

public class PostgresqlExporters {

	public static final String CONFIG_TABLE_NAME = "db_versions"; //$NON-NLS-1$
	public static final String PLUGIN_VERSION_TBL = "connect.ca_plugin_version";
	
	public void exportAll(ICaDataExportEngine exportEngine) throws Exception{
		exportConservationAreaInfo(exportEngine);
		exportAggregationTable(exportEngine);
		exportHibernateData(exportEngine);
		exportAttAggMapTable(exportEngine);
		exportPlugInConfiguration(exportEngine);
		exportDataStore(exportEngine);
		
		exportServerStatus(exportEngine);
	}
	
	private void exportDataStore(ICaDataExportEngine exportEngine) throws Exception {
		Path filestore = exportEngine.getExportLocation().toPath().resolve(ICaDataExportEngine.FILESTORE_DIR);
		
		Files.createDirectories(filestore);
		
		ConservationAreaInfo info = (ConservationAreaInfo) exportEngine.getSession().get(ConservationAreaInfo.class, exportEngine.getConservationArea().getUuid());
		Path filestoreLocation = DataStoreManager.INSTANCE.getConservationAreaFullPath(info).toPath();
		if (Files.exists(filestoreLocation)){
			FileUtils.copyDirectory(filestoreLocation.toFile(), filestore.toFile());
		}

	}
	
	private void exportHibernateData(ICaDataExportEngine exportEngine) throws Exception {
		/* export all mapped tables */
		List<PostgresqlTableInfo> info = getTableInformation(exportEngine);
		
		for (PostgresqlTableInfo in : info) {
			if (in.getCaPropertyName() != null) {
				String[] columns = exportEngine.getTableColumns(in.getTableName());
				exportEngine.writeTableDefinitionFile(in.getTableName(), in.getClazz().getSimpleName(), columns);
				exportEngine.exportTableData(in.getTableName(), in.getClazz().getSimpleName(), columns, in.getCaPropertyName());
			} else {
				
				String hqlQuery = in.getCaLink();
				if (hqlQuery == null || hqlQuery.trim().length() == 0){
					//skip this table as we can't figure out what conservation area data it is associated with
					continue;
				}
				
				String[] columns = exportEngine.getTableColumns(in.getTableName());
				exportEngine.writeTableDefinitionFile(in.getTableName(), in.getClazz().getSimpleName(), columns);
				exportEngine.writeHibernateQuery(in.getTableName(), in.getClazz().getSimpleName(), columns, hqlQuery);				
			}
		}
	}
	
	private void exportPlugInConfiguration(ICaDataExportEngine exportEngine) throws Exception {
		exportEngine.writeQuery(CONFIG_TABLE_NAME, "SELECT plugin_id, version FROM " + PLUGIN_VERSION_TBL + " WHERE ca_uuid = '" + exportEngine.getConservationArea().getUuid().toString() + "'"); //$NON-NLS-1$
	}
	
	private void exportServerStatus(ICaDataExportEngine exportEngine) throws Exception {
		
		String tableName = "smart.connect_status";
		exportEngine.writeTableDefinitionFile(tableName, ConnectServerStatus.class.getSimpleName(), new String[]{"CA_UUID", "CONNECT_UUID", "VERSION", "SERVER_REVISION", "STATUS", "UPLOADURL", "LOCALFILE"});
		
		String filename = tableName + "." + ConnectServerStatus.class.getSimpleName();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT a.ca_uuid, b.uuid, a.version, ");
		sb.append(" case when c.max_revision is null then -1 else c.max_revision end, 'DONE', null, null ");
		sb.append("FROM connect.ca_info a, smart.connect_server b left join ");
		sb.append(" (SELECT max(revision) as max_revision, ca_uuid as ca_uuid FROM connect.change_log WHERE ca_uuid = '" + exportEngine.getConservationArea().getUuid().toString() + "' GROUP BY ca_uuid) c");
		sb.append(" on b.ca_uuid = c.ca_uuid ");
		sb.append("WHERE b.ca_uuid = a.ca_uuid and a.ca_uuid = '" + exportEngine.getConservationArea().getUuid().toString()  + "'");
		
		System.out.println(sb.toString());
		exportEngine.writeQuery(filename, sb.toString());
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
		
		String attributeTableName = null;
		ClassMetadata m = exportEngine.getSession().getSessionFactory().getClassMetadata(Attribute.class);
		if (m instanceof Joinable){
			attributeTableName = ((Joinable)m).getTableName();
		}
		
		query.append(attributeTableName);
		query.append(" b on a.attribute_uuid = b.uuid "); //$NON-NLS-1$
		query.append(" WHERE b.ca_uuid = '"); //$NON-NLS-1$
		query.append(exportEngine.getConservationArea().getUuid().toString());
		query.append("'"); //$NON-NLS-1$
		
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
	
	 /*
	  * ca info exporter
	  */
	private void exportConservationAreaInfo(ICaDataExportEngine exportEngine) throws IOException{
		
		String version = (String)exportEngine
			.getSession()
			.createSQLQuery("SELECT version FROM " + PLUGIN_VERSION_TBL + " WHERE ca_uuid = '" + exportEngine.getConservationArea().getUuid().toString() + "' AND plugin_id = 'org.wcs.smart'")
			.uniqueResult();
		
		Path file = exportEngine.getExportLocation().toPath().resolve(ICaDataExportEngine.CA_INFO_FILENAME);
		ConservationArea ca = exportEngine.getConservationArea();
		try(BufferedWriter writer = Files.newBufferedWriter(file)){
			writer.write(UuidUtils.uuidToString(ca.getUuid()));
			writer.newLine();
			writer.write(ca.getId());
			writer.newLine();
			writer.write(ca.getName());
			writer.newLine();
			writer.write(ca.getDescription());
			writer.newLine();
			writer.write(version);
		};
	}
	/* helper function */
	private List<PostgresqlTableInfo> getTableInformation(ICaDataExportEngine engine){
		List<PostgresqlTableInfo> tables = new ArrayList<PostgresqlTableInfo>();
		
		Map<String, ClassMetadata> x = engine.getSession().getSessionFactory().getAllClassMetadata();
		
		for (SmartTable st : SmartTable.values()){
			ClassMetadata metadata = x.get(st.hibernateClass.getName());
			if (metadata == null || metadata.hasSubclasses()){
				//this is not mapped to a db table
				System.out.println("NOT MAPPED:" + st.hibernateClass.getName());
				continue;
			}
			String tableName = ((AbstractEntityPersister)metadata).getTableName();
			
			PostgresqlTableInfo info = new PostgresqlTableInfo(st.hibernateClass, tableName);
			
			if (st.caProperty == null){
				for (int k = 0; k < metadata.getPropertyTypes().length; k ++){
					if (metadata.getPropertyTypes()[k].getReturnedClass() == ConservationArea.class){
						info.setCaPropertyName(((AbstractEntityPersister)metadata).getPropertyColumnNames(k)[0]);
					}
				}
			}else{
				info.setCaLink(st.caProperty);
			}
			tables.add(info);
		}
		
		return tables;
		
	}
}
