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
package org.wcs.smart.connect.cybertracker.alert;

import org.hibernate.Session;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.cybertracker.ConnectCtHibernateManager;
import org.wcs.smart.connect.cybertracker.model.ConnectCtProperties;
import org.wcs.smart.cybertracker.export.alert.IDataTargetProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Data target provider that provides a Connect URL as the target.
 * 
 * @author Emily
 *
 */
public class ConnectCtDataTargetProvider implements IDataTargetProvider {

	private ConfigurableModel cm;
	
	private String username;
	private String password;
	private String url;
	
	private ConnectConfigurationExtension ext;
	private Session session;
	
	public ConnectCtDataTargetProvider(ConfigurableModel cm, ConnectConfigurationExtension ext, Session session){
		this.ext = ext;
		this.cm = cm;
		this.session = session;
	}
	
	@Override
	public DataTarget getTarget() throws Exception {
		ConnectCtProperties properties = ConnectCtHibernateManager.getCtProperties(cm, session);
		if (properties.getDataFrequency() == null || properties.getDataFrequency() <= 0){
			return null;
		}
		initConnectFields();
		return new DataTarget(url, username, password, properties.getDataFrequency());
	}

	private void initConnectFields() throws Exception {		
		String[] data = ext.getConnectData();
		if (data.length == 3){
			url = data[0] + SmartConnect.API_URL + "/ctdata"; //$NON-NLS-1$
			username = data[1];
			password = data[2];
		}else{
			//TODO: fix this error message
			throw new Exception("A target Connect server is defined for this configurable model, however we could not connect to the server to valid the connection information.  Either need to remove data sending from configurable model or fix connect server information.");
		}
		
	}
}
