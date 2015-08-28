package org.wcs.smart.connect.uploader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.util.UuidUtils;

public class PackageMetadata {
	
	public static final String CA_UUID_KEY = "conservationareauuid";
	public static final String VERSION_KEY = "version";
	public static final String CLIENT_REVISION_KEY = "client_revision";
	public static final String SERVER_REVISION_KEY = "server_revision";
	public static final String PLUGIN_KEY_PREFIX = "pluginid.";
	
	public static PackageMetadata readMeadata(Path file) throws Exception{
		Properties prop = new Properties();
		try(InputStream is = Files.newInputStream(file)){
			prop.load(is);
		}
		
		PackageMetadata m = new PackageMetadata();
		
		for (Object okey : prop.keySet()){
			String key = (String)okey;
			if (key.equals(CA_UUID_KEY)){
				m.conservationArea = UuidUtils.stringToUuid(prop.getProperty(key));
			}else if (key.equals(VERSION_KEY)){
				m.version = UuidUtils.stringToUuid(prop.getProperty(key));
			}else if (key.equals(CLIENT_REVISION_KEY)){
				m.clientRevision = Long.valueOf(prop.getProperty(key));
			}else if (key.equals(SERVER_REVISION_KEY)){
				m.serverRevision = Long.valueOf(prop.getProperty(key));
			}else if (key.startsWith(PLUGIN_KEY_PREFIX)){
				String pluginid = key.substring(key.indexOf(PLUGIN_KEY_PREFIX) + PLUGIN_KEY_PREFIX.length());	
				m.plugins.put(pluginid, prop.getProperty(key));
			}	
		}
		return m;
	}
	
	public static void generateMetadata(Session session, 
			UUID caUuid, Path file, long revision) throws Exception{
		
		ConservationAreaInfo ca = (ConservationAreaInfo) session.load(ConservationAreaInfo.class, caUuid);
		if (ca == null){
			throw new Exception("Could not determine conservation area info.");
		}
		
		Properties prop = new Properties();
		prop.setProperty(CA_UUID_KEY, UuidUtils.uuidToString(caUuid));
		prop.setProperty(VERSION_KEY, UuidUtils.uuidToString(ca.getVersion()));
		prop.setProperty(CLIENT_REVISION_KEY, String.valueOf(revision));
		prop.setProperty(SERVER_REVISION_KEY, String.valueOf(revision));
				
		//plugin versions
		SQLQuery q = session.createSQLQuery("SELECT version, plugin_id FROM connect.connect_plugin_version");
		List<Object[]> plugins = q.list();
		for (Object[] version : plugins){
			prop.setProperty(PLUGIN_KEY_PREFIX + (String)version[1], (String)version[0]);
		}
		
		try(OutputStream out = Files.newOutputStream(file)){
			prop.store(out, "");
		}
	}
	
	private UUID conservationArea;
	private UUID version;
	private UUID client;
	private Long serverRevision;
	private Long clientRevision;
	
	private HashMap<String, String> plugins = new HashMap<String, String>();
	
	protected PackageMetadata(){
		
	}
	
	public UUID getConservationArea(){
		return this.conservationArea;
	}
	public UUID getVersion(){
		return this.version;
	}
	public UUID getClient(){
		return this.client;
	}
	public long getServerRevision(){
		return this.serverRevision;
	}
	public long getClientRevision(){
		return this.clientRevision;
	}
	public Set<String> getPlugins(){
		return plugins.keySet();
	}
	public String getPluginVersion(String pluginid){
		return plugins.get(pluginid);
	}
}
