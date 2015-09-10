package org.wcs.smart.connect.replication.metadata;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

public class PackageMetadata {

	private UUID caUuid;
	private UUID version;
	private Long serverRevision;
	private Long clientRevision;
	
	private HashMap<String, String> pluginVersions;
	
	public PackageMetadata(){
		this(null, null, null, null, null, new HashMap<String, String>());
		
	}
	
	public PackageMetadata(Path output,
			UUID caUuid,
			UUID version,
			Long clientRevision,
			Long serverRevision,
			HashMap<String, String> pluginVersions){
		this.caUuid = caUuid;
		this.version = version;
		this.clientRevision = clientRevision;
		this.serverRevision = serverRevision;
		this.pluginVersions = pluginVersions;
	}
	
	public UUID getConservationArea(){
		return this.caUuid;
	}
	
	public UUID getVersion(){
		return this.version;
	}
	public Long getClientRevision(){
		return this.clientRevision;
	}
	public Long getServerRevision(){
		return this.serverRevision;
	}
	public HashMap<String, String> getPluginVersions(){
		return this.pluginVersions;
	}
	public String getPluginVersion(String plugin){
		return this.pluginVersions.get(plugin);
	}
	
	public void setConservationArea(UUID ca){
		this.caUuid = ca;
	}
	public void setVersion(UUID version){
		this.version = version;
	}
	public void setClientRevision(Long revision){
		this.clientRevision = revision;
	}
	public void setServerRevision(Long revision){
		this.serverRevision = revision;
	}
	
	public void setPluginVersions(HashMap<String,String> pluginVersions){
		this.pluginVersions.putAll(pluginVersions);
	}
	public void setPluginVersion(String pluginid, String version){
		this.pluginVersions.put(pluginid, version);
	}
}
