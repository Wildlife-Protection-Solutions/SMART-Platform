package org.wcs.smart.connect.replication;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.commons.math.distribution.CauchyDistribution;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

public class MetadataPackager {

	public static final String CA_UUID_KEY = "conservationareauuid";
	public static final String VERSION_KEY = "version";
	public static final String CLIENT_REVISION_KEY = "client_revision";
	public static final String SERVER_REVISION_KEY = "server_revision";
	public static final String PLUGIN_KEY_PREFIX = "pluginid.";
	
	public static void generateMetadata(Session session, ConnectServer server, 
			Path file, long revision) throws Exception{
		
		ConservationArea ca = (ConservationArea) session.load(ConservationArea.class, server.getConservationArea().getUuid());
		
		ConnectServerStatus status = (ConnectServerStatus) session.get(ConnectServerStatus.class, ca.getUuid());
		if (status == null){
			 throw new Exception("Could not determine server status.");
		}
		
		Properties prop = new Properties();
		prop.setProperty(CA_UUID_KEY, UuidUtils.uuidToString(ca.getUuid()));
		prop.setProperty(VERSION_KEY, UuidUtils.uuidToString(status.getVersion()));
		prop.setProperty(CLIENT_REVISION_KEY, String.valueOf(revision));
		prop.setProperty(SERVER_REVISION_KEY, String.valueOf(status.getServerRevision()));
				
		//plugin versions
		SQLQuery q = session.createSQLQuery("SELECT version, plugin_id FROM " + SmartDB.PLUGIN_VERSION_TBL);
		List<Object[]> plugins = q.list();
		for (Object[] version : plugins){
			prop.setProperty(PLUGIN_KEY_PREFIX + (String)version[1], (String)version[0]);
		}
		
		try(OutputStream out = Files.newOutputStream(file)){
			prop.store(out, "");
		}
	}
}
