package org.wcs.smart.connect.uploader;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.PostgresUUIDType;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.replication.metadata.MetadataPackager;
import org.wcs.smart.connect.replication.metadata.PackageMetadata;

public class PostgresqlMetadataCreator {
	
	public static void generateMetadata(Session session, 
			UUID caUuid, Path file, long revision) throws Exception{
		
		ConservationAreaInfo ca = (ConservationAreaInfo) session.load(ConservationAreaInfo.class, caUuid);
		if (ca == null){
			throw new Exception("Could not determine conservation area info.");
		}
		
		PackageMetadata metadata = new PackageMetadata();
		metadata.setConservationArea(caUuid);
		metadata.setVersion(ca.getVersion());
		metadata.setClientRevision(revision);
		metadata.setServerRevision(revision);
		
		//plugin versions
		SQLQuery q = session.createSQLQuery("SELECT version, plugin_id FROM connect.ca_plugin_version WHERE ca_uuid = :ca ");
		q.setParameter("ca", ca.getUuid(), PostgresUUIDType.INSTANCE);
		List<Object[]> plugins = q.list();
		for (Object[] version : plugins){
			metadata.setPluginVersion((String)version[1], (String)version[0]);
		}
		
		MetadataPackager.INSTANCE.writeMetadata(file, metadata);
	}
	
}
