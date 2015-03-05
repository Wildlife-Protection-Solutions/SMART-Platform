package org.wcs.smart.conversion.tool;

import java.sql.Connection;

import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class MatchSession {
	
	private SmartMapping ct2Smart;
	private Connection connection;
	private DataModel dataModel;
	
	public SmartMapping getSmartMapping() {
		return ct2Smart;
	}
	public void setSmartMapping(SmartMapping ct2Smart) {
		this.ct2Smart = ct2Smart;
	}
	public Connection getConnection() {
		return connection;
	}
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	public DataModel getDataModel() {
		return dataModel;
	}
	public void setDataModel(DataModel dataModel) {
		this.dataModel = dataModel;
	}
	
}
