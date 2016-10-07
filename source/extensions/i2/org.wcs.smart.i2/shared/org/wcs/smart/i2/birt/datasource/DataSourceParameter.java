package org.wcs.smart.i2.birt.datasource;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;

public class DataSourceParameter {

	public static DataSourceParameter ENTITY_UUID = new DataSourceParameter(
			"Entity UUID", IParameterMetaData.parameterModeIn,
			java.sql.Types.VARCHAR);
	
	public static DataSourceParameter START_DATE = new DataSourceParameter("Start Date",
			IParameterMetaData.parameterModeIn, java.sql.Types.DATE);
	
	public static DataSourceParameter END_DATE = new DataSourceParameter("End Date",
			IParameterMetaData.parameterModeIn, java.sql.Types.DATE);

	private String name;
	private int parameterMode;
	private int type;

	DataSourceParameter(String name, int parameterMode, int type) {
		this.name = name;
		this.parameterMode = parameterMode;
		this.type = type;
	}

	public String getName() {
		return this.name;
	}

	public int getParameterMode() {
		return this.parameterMode;
	}

	public int getType() {
		return type;
	}

}
