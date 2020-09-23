/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.map.engine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.views.map.CombinedOverviewColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.asset.ui.views.map.StationData;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Column engine for computing values for
 * the asset overview map table.
 * 
 * @author Emily
 *
 */
public class OverviewmapColumnEngine {

	protected List<StationData> data;
	private HashMap<UUID, StationData> map;
	
	private Set<IOverviewTableColumn> computedColumns;
	protected ConservationArea ca;
	
	/**
	 * Creates a new engine
	 */
	public OverviewmapColumnEngine(ConservationArea ca) {
		data = new ArrayList<>();
		map = new HashMap<>();
		computedColumns = new HashSet<>();
		this.ca = ca;
	}
	
	/**
	 * Called when new data is available for display.  Subclasses
	 * should override
	 */
	public void refreshData() {
		
	}
	
	/**
	 * Called when all data has been computed. Subclasses
	 * should override
	 */
	public void finish() {
		
	}
	
	/**
	 * 
	 * @return all the data computed to date
	 */
	public List<StationData> getData(){
		return this.data;
	}
	
	/**
	 * 
	 * @param column
	 * @return true of false if the column has been processed or not
	 */
	public boolean isComputed(IOverviewTableColumn column) {
		return computedColumns.contains(column);
	}
	
	/**
	 * Clears any existing data and recomputes all values
	 * @param column the set of columns to process
	 * @param dFilter may be null if should include all dates
	 * @param groupBy type of statistic to compute
	 * 
	 */
	public void computeStatistics(List<IOverviewTableColumn> column, LocalDate[] dFilter, IOverviewTableColumn.GroupByOption groupBy, IProgressMonitor monitor) {
		//clear existing datay
		data.clear();
		map.clear();
		computedColumns.clear();
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			
			List<IColumnEngine> engines = new ArrayList<>();
			engines.add(new FixedColumnEngine(dFilter, groupBy, ca, session));
			engines.add(new CategoryColumnEngine(dFilter, groupBy, ca, session));
			
			try {
				//we don't compute combined columns here; do that after
				List<IOverviewTableColumn> toCompute = new ArrayList<>();
				List<CombinedOverviewColumn> combinedColumns = new ArrayList<>();
				for (IOverviewTableColumn c : column) {
					if (c instanceof CombinedOverviewColumn) {
						combinedColumns.add((CombinedOverviewColumn)c);
					}else {
						toCompute.add(c);
					}
				}
				
				//compute single columns
				for (IOverviewTableColumn c : toCompute) {
					for (IColumnEngine e : engines) {
						if (e.canProcess(c)){
							processColumn(e, c, groupBy, session);
						}
					}
					computedColumns.add(c);
					if (monitor.isCanceled()) return ;
				}
				
				//dipose engines
				engines.forEach(e->e.dispose(session));
				
				//compute combined columns
				for (CombinedOverviewColumn c : combinedColumns) {
					for (StationData d : data) CombinedColumnEngine.computeValue(d, c);
					if (monitor.isCanceled()) return ;
					refreshData();
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
			HashMap<UUID, Object> columnData = engine.computeValues(column);
			
			
			for (Entry<UUID, Object> entry : columnData.entrySet()) {
				
				StationData sd = map.get(entry.getKey());
				if (sd == null) {
					sd = new StationData(entry.getKey());
					if (groupBy == GroupByOption.STATION) {
						AssetStation station = (AssetStation)session.get(AssetStation.class, sd.getKeyUuid());
						station.computeStatus(session);
						sd.setAssetStationObject(station);
						
						for (AssetDeployment d : station.getActiveDeployments(session)) {
							sd.addAsset(d.getAsset());
						}
					}else if (groupBy == GroupByOption.LOCATION) {
						AssetStationLocation l = (AssetStationLocation)session.get(AssetStationLocation.class, sd.getKeyUuid());
						l.computeStatus(session);
						sd.setAssetLocationObject(l);
						for (AssetDeployment d : l.getActiveDeployments(session)) {
							sd.addAsset(d.getAsset());
						}
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
