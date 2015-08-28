package org.wcs.smart.connect.ca.exporter;

import org.wcs.smart.ca.export.TableInfo;

public class PostgresqlTableInfo extends TableInfo {

	public PostgresqlTableInfo(Class<?> clazz, String tableName) {
		super(clazz, tableName);
	}

	public String caLink;
	
	public void setCaLink(String link){
		this.caLink = link;
	}
	public String getCaLink(){
		return this.caLink;
	}
}
