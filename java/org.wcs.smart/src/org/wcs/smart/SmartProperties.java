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

import java.io.InputStream;
import java.util.Properties;

/**
 * Reads the smart properties file
 * 
 * @author Emily
 *
 */
public class SmartProperties {

	private static final String SMART_PROPERTIES = "properties/smart.properties";
	
	private static final String IUCN_DATAMODEL_FILE = "properties/IUCN_Threats.xml";

	/**
	 * Database location key
	 */
	public static final String SMART_DB_KEY = "db.dbname";
	
	private Properties prop = null;
	
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
		return SmartApp.class.getClassLoader().getResourceAsStream(IUCN_DATAMODEL_FILE);
	}
	
	private void readProperties(){
		prop = new Properties();
		try{
			InputStream stream = SmartApp.class.getClassLoader().getResourceAsStream(SMART_PROPERTIES);
			prop.load(stream);
		}catch (Exception ex){
			//cannot load properties files
			SmartPlugIn.displayLogExit("Cannot read properties file.", ex);
		}
	}
	
	/**
	 * Loads a given property from the property file.
	 * @param key
	 * @return
	 */
	public String getProperty(String key){
		if (prop == null){
			readProperties();
		}
		return prop.getProperty(key);
		
	}
}
