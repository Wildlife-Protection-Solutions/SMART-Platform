package org.wcs.smart.nunavut;

import java.time.LocalDateTime;

public class MysqlCoordinate implements Comparable<MysqlCoordinate> {

	private LocalDateTime date;
	private double lon;
	private double lat;
	private int id;  //mobile_data_geometry_id from table mobile_data_geometry in Mysql
	
	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	
	
	//compareTo method overridden for sorting array of objects
    @Override
    public int compareTo(MysqlCoordinate c) {
        return this.date.compareTo(c.getDate());
    }



}

