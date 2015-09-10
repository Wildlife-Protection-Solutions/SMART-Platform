package org.wcs.smart.connect.replication.metadata;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Properties;

import org.wcs.smart.util.UuidUtils;

public enum MetadataPackager {
	INSTANCE;
	public static final String CA_UUID_KEY = "conservationareauuid";
	public static final String VERSION_KEY = "version";
	public static final String CLIENT_REVISION_KEY = "client_revision";
	public static final String SERVER_REVISION_KEY = "server_revision";
	public static final String PLUGIN_KEY_PREFIX = "pluginid.";
	
	public void writeMetadata(Path output, PackageMetadata metadata) throws Exception{
		Properties prop = new Properties();
		prop.setProperty(CA_UUID_KEY, UuidUtils.uuidToString(metadata.getConservationArea()));
		prop.setProperty(VERSION_KEY, UuidUtils.uuidToString(metadata.getVersion()));
		prop.setProperty(CLIENT_REVISION_KEY, String.valueOf(metadata.getClientRevision()));
		prop.setProperty(SERVER_REVISION_KEY, String.valueOf(metadata.getServerRevision()));
				
		//plugin versions
		for(Entry<String, String> e : metadata.getPluginVersions().entrySet()){
			prop.setProperty(PLUGIN_KEY_PREFIX + e.getKey(), e.getValue());
		}
		
		try(OutputStream out = Files.newOutputStream(output)){
			prop.store(out, "");
		}
	}
	
	public PackageMetadata readMetadata(Path input) throws Exception{
		Properties prop = new Properties();
		
		try(InputStream is = Files.newInputStream(input)){
			prop.load(is);
		}
		
		PackageMetadata m = new PackageMetadata();
		
		for (Object okey : prop.keySet()){
			String key = (String)okey;
			if (key.equals(CA_UUID_KEY)){
				m.setConservationArea(UuidUtils.stringToUuid(prop.getProperty(key)));
			}else if (key.equals(VERSION_KEY)){
				m.setVersion(UuidUtils.stringToUuid(prop.getProperty(key)));
			}else if (key.equals(CLIENT_REVISION_KEY)){
				m.setClientRevision(Long.valueOf(prop.getProperty(key)));
			}else if (key.equals(SERVER_REVISION_KEY)){
				m.setServerRevision(Long.valueOf(prop.getProperty(key)));
			}else if (key.startsWith(PLUGIN_KEY_PREFIX)){
				String pluginid = key.substring(key.indexOf(PLUGIN_KEY_PREFIX) + PLUGIN_KEY_PREFIX.length());
				m.setPluginVersion(pluginid, prop.getProperty(key));
			}	
		}
		return m;
	}
}
