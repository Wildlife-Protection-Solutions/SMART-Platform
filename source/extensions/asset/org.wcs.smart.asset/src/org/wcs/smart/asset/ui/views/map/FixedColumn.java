package org.wcs.smart.asset.ui.views.map;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;

public class FixedColumn implements IOverviewTableColumn{

	private static IOverviewTableColumn[] columns = null;
	
	public static IOverviewTableColumn[] getFixedColumns() {
		if (columns != null) return columns;
		
		FixedColumn c0 = new FixedColumn(Column.STATUS);
		FixedColumn c1 = new FixedColumn(Column.ACTIVE_DAYS);
		FixedColumn c2 = new FixedColumn(Column.ASSET_DAYS);
		FixedColumn c3 = new FixedColumn(Column.INCIDENTS);
		CombinedOverviewColumn c4 = new CombinedOverviewColumn(Column.INCIDENTS_PER_DAY.guiName, c3,c2);
		
		columns = new IOverviewTableColumn[] {
				c0,c1,c2,c3,c4
		};
		return columns;
	}
		
	private enum Column{
		
		ACTIVE_DAYS("Total Active Days"),
		ASSET_DAYS("Total Asset Days"),
		INCIDENTS("Total Incidents"),
		INCIDENTS_PER_DAY("Total Incident Per Asset Day"),
		STATUS("Current Status");
		
		String guiName;
		
		Column(String name){
			this.guiName = name;
		}
	}
	
	private Column column;
	
	private FixedColumn(Column column) {
		this.column = column;
	}
	
	@Override
	public String getName() {
		return column.guiName;
	}

	@Override
	public String getValue(StationData data) {
		if (this.column == Column.STATUS) {
			if (data.getStation() != null) return data.getStation().getStatus().getGuiName(Locale.getDefault());
			if (data.getStationLocation() != null) return data.getStationLocation().getStatus().getGuiName(Locale.getDefault());
			return "unknown";
		}
		Object number = data.getData(this);
		if (number == null) return "0";
		return String.valueOf(number);
	}
	
	@Override
	public HashMap<AssetStation, Object> computeValuesByStation(Session session, Date[] dFilter){
		
		switch(column) {
		case ACTIVE_DAYS:
			return StatisticComputer.computeNumberOfDaysPerStation(session, dFilter);
		case ASSET_DAYS:
			return StatisticComputer.computeAssetDaysPerStation(session, dFilter);
		case INCIDENTS:
			return StatisticComputer.incidentsPerStation(session, dFilter);
		}
		return new HashMap<>();	
	}
	
	@Override
	public HashMap<AssetStationLocation, Object> computeValuesByStationLocation(Session session, Date[] dFilter){
		switch(column) {
		case ACTIVE_DAYS:
			return StatisticComputer.computeNumberOfDaysPerStationLocation(session, dFilter);
		case ASSET_DAYS:
			return StatisticComputer.computeAssetDaysPerStationLocation(session, dFilter);
		case INCIDENTS:
			return StatisticComputer.incidentsPerLocation(session, dFilter);
		}
		return new HashMap<>();
		
	}

}
