/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.replication.metadata;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

/**
 * Class to represent the metadata provided in data sync, upload
 * and download packages.
 * 
 * @author Emily
 *
 */
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
