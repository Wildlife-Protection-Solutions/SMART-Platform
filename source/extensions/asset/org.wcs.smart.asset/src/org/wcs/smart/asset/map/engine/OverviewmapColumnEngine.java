package org.wcs.smart.asset.map.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.views.map.CombinedOverviewColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.asset.ui.views.map.StationData;
import org.wcs.smart.hibernate.HibernateManager;

public class OverviewmapColumnEngine {

	protected List<StationData> data;
	private HashMap<UUID, StationData> map;
	
	public Set<IOverviewTableColumn> computedColumns;
	
	public OverviewmapColumnEngine() {
		data = new ArrayList<>();
		map = new HashMap<>();
		computedColumns = new HashSet<>();
	}
	
	public void refreshData() {
		
	}
	
	public void finish() {
		
	}
	
	public List<StationData> getData(){
		return this.data;
	}
	
	public boolean isComputed(IOverviewTableColumn column) {
		return computedColumns.contains(column);
	}
	
	/**
	 * 
	 * @param column
	 * @param dFilter may be null if should include all dates
	 * @param groupBy
	 * @return
	 */
	public void computeStatistics(List<IOverviewTableColumn> column, Date[] dFilter, IOverviewTableColumn.GroupByOption groupBy) {
		data.clear();
		map.clear();
		computedColumns.clear();
		List<IColumnEngine> engines = new ArrayList<>();
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				engines.add(new FixedColumnEngine(session, dFilter));
				engines.add(new CategoryColumnEngine(session, dFilter));
				
				List<IOverviewTableColumn> toCompute = new ArrayList<>();
				for (IOverviewTableColumn c : column) {
					if (!(c instanceof CombinedOverviewColumn)) {
						toCompute.add(c);
					}
				}
				
				for (IOverviewTableColumn c : toCompute) {
					for (IColumnEngine e : engines) {
						
						if (e.canProcess(c)){
							processColumn(e, c, groupBy, session);
						}
					}
					computedColumns.add(c);
				}
			}finally {
				session.getTransaction().rollback();
			}

		}
		//add all combined columns
		for (IOverviewTableColumn c : column) {
			computedColumns.add(c);
		}
		finish();
	}
	
	private void processColumn(IColumnEngine engine, IOverviewTableColumn column, IOverviewTableColumn.GroupByOption groupBy, Session session) {
		try {
			HashMap<UUID, Object> columnData = engine.computeValues(column, groupBy);
			
			
			for (Entry<UUID, Object> entry : columnData.entrySet()) {
				
				StationData sd = map.get(entry.getKey());
				if (sd == null) {
					sd = new StationData(entry.getKey());
					if (groupBy == GroupByOption.STATION) {
						sd.setAssetStationObject(session.get(AssetStation.class, sd.getKeyUuid()));
					}else if (groupBy == GroupByOption.LOCATION) {
						sd.setAssetLocationObject(session.get(AssetStationLocation.class, sd.getKeyUuid()));
					}
					data.add(sd);
					map.put(sd.getKeyUuid(), sd);
				}
				sd.setData(column, entry.getValue());
			}
		}catch (Exception ex) {
			AssetPlugIn.log(ex.getMessage(), ex);
			data.forEach(d->d.setData(column, ex));
		}
		
		refreshData();
	}
}
