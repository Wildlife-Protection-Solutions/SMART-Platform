package org.wcs.smart.ct2smart.ui;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;

public class MatchSession {
	
	private Ct2Smart ct2Smart;
	private Connection connection;
	private Map<String, AttributeType> key2Attribute;
	private DataModel dataModel;
	
	public Ct2Smart getCt2Smart() {
		return ct2Smart;
	}
	public void setCt2Smart(Ct2Smart ct2Smart) {
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
		key2Attribute = createKey2AttributeMap(dataModel);
	}
	
	public Map<String, AttributeType> getKey2Attribute() {
		return key2Attribute;
	}
	
	private Map<String, AttributeType> createKey2AttributeMap(DataModel dm) {
		Map<String, AttributeType> map = new HashMap<String, AttributeType>();
		for (AttributeType a : dm.getAttributes().getAttributes()) {
			map.put(a.getKey(), a);
		}
		return map;
	}

	
}
