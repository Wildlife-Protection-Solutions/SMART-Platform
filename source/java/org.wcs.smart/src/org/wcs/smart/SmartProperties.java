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
package org.wcs.smart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.wcs.smart.internal.Messages;

/**
 * Reads the smart properties file
 * 
 * @author Emily
 *
 */
public class SmartProperties {

	/**
	 * Names of directory containing all properties files
	 */
	public static final String PROPERTIES_DIR = "properties"; //$NON-NLS-1$
	
	private static final String DEFAULT_DATAMODEL_FILE ="/" + PROPERTIES_DIR + "/datamodel.xml"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String SMART_PROPERTIES ="/" + PROPERTIES_DIR + "/smart.properties"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Database location key
	 */
	public static final String PROP_SMART_DB = "DATABASE_LOC"; //$NON-NLS-1$
	public static final String PROP_FILESTORE = "FILESTORE_LOC"; //$NON-NLS-1$
	public static final String PROP_GPS_BABEL = "GPSBABEL"; //$NON-NLS-1$
	public static final String PROP_BACKUP_DIR = "BACKUP"; //$NON-NLS-1$
	
	private static final String SYSPROP_GPS_BABEL = "GPSBABEL"; //$NON-NLS-1$
	public static final String SYSPROP_DATASTORE = "DATASTORE_DIR"; //$NON-NLS-1$
	private static final String SYSPROP_BACKUPDIR = "BACKUP_DIR"; //$NON-NLS-1$
	
	
	//subloction of filestore within the data directory
	private static final String FILESTORE_DIR_NAME = "filestore" + File.separator; //$NON-NLS-1$
	//subloction of database within the data directory
	private static final String DB_DIR_NAME = "database" + File.separator + "smartdb" + File.separator; //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Current supported database version key
	 */
	public static final String DB_VERSION_KEY = "dbversion"; //$NON-NLS-1$
	
	private static SmartProperties instance = null;
	
	
	private SmartProperties(){
	}
	
	/**
	 * Create a new instance of the smart properties
	 * @return
	 */
	public static SmartProperties getInstance(){
		if (instance == null){
			instance = new SmartProperties();
		}
		return instance;
	}
	
	public static InputStream getIucnDataModelFile(){
		return SmartProperties.class.getResourceAsStream(DEFAULT_DATAMODEL_FILE);
	}
	
	public String getProperty(String key){
		if (key.equals(PROP_FILESTORE)){
			return getSystemProperty(SYSPROP_DATASTORE) + File.separator + FILESTORE_DIR_NAME;
		}else if (key.equals(PROP_SMART_DB)){
			return getSystemProperty(SYSPROP_DATASTORE) + File.separator + DB_DIR_NAME;
		}else if (key.equals(PROP_GPS_BABEL)){
			return getSystemProperty(SYSPROP_GPS_BABEL);
		}else if (key.equals(PROP_BACKUP_DIR)){
			return getSystemProperty(SYSPROP_BACKUPDIR);
		}else if (key.equals(DB_VERSION_KEY)){
			return getSmartProperties(key);
		}else if (key.equals(SYSPROP_DATASTORE)){
			return getSystemProperty(SYSPROP_DATASTORE);
		}
		throw new IllegalStateException(MessageFormat.format(Messages.SmartProperties_InvalidProperty, new Object[]{key}));
	}
	
	/**
	 * 
	 * @param key
	 * @return default value for system preference
	 */
	public String getSystemDefaultValue(String key){
		return DefaultScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).get(key, ""); //$NON-NLS-1$
	}
	
	/**
	 * Reads the given key from the SMART properties file.
	 * @param key
	 * @return
	 */
	private String getSmartProperties(String key){
		Properties prop = new Properties();
		try {
			prop.load(SmartProperties.class.getResourceAsStream(SMART_PROPERTIES));
		} catch (IOException e) {
			SmartPlugIn.log("Error determining database version from properties file.", e); //$NON-NLS-1$
			return null;
		}
		return prop.getProperty(key);
		
	}
		
	/**
	 * Loads a given property from the SMART properties
	 * @param key
	 * @return
	 */
	private String getSystemProperty(String key){
		return ConfigurationScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).get(key, getSystemDefaultValue(key));
	}
	
	/**
	 * Sets smart property.
	 * <p>Only supports setting of PROP_GPS_BABEL and SYSPROP_DATASTORE. All other properties
	 * not supported.</p>
	 * 
	 * @param key smart property key
	 * @param value new value
	 * @throws Exception
	 */
	public void setKey(String key, String value) throws Exception{
		if (key.equals(PROP_GPS_BABEL)){
			ConfigurationScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).put(key, value);
		}else if (key.equals(SYSPROP_DATASTORE)){
			ConfigurationScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).put(key, value);
		}
	}
}
