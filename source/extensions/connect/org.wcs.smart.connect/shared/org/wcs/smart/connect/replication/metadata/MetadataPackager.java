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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Properties;

import org.wcs.smart.util.UuidUtils;

/**
 * Class for serializing and deserializing PackageMetadata
 * to a file.
 * 
 * @author Emily
 *
 */
public enum MetadataPackager {
	
	INSTANCE;
	
	public static final String CA_UUID_KEY = "conservationareauuid"; //$NON-NLS-1$
	public static final String VERSION_KEY = "version"; //$NON-NLS-1$
	public static final String CLIENT_REVISION_KEY = "client_revision"; //$NON-NLS-1$
	public static final String SERVER_REVISION_KEY = "server_revision"; //$NON-NLS-1$
	public static final String PLUGIN_KEY_PREFIX = "pluginid."; //$NON-NLS-1$
	
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
			prop.store(out, ""); //$NON-NLS-1$
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
