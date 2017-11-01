package org.wcs.smart.asset.ui.views.asset;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetHistoryRecord;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.hibernate.SmartDB;

public class AssetDeploymentTableColumn extends ColumnLabelProvider{

	public enum FixedColumn{
		STATION("Station"),
		LOCATION("Location"),
		START_DATE("Start Date"),
		END_DATE("End Date"),
		TOTAL_TIME("Total Time"),
		NUM_INCIDENTS("Number Incidents");
		
		String guiName;
		
		FixedColumn(String name) {
			this.guiName = name;
		}
	}
	

	private FixedColumn column;
	private AssetAttribute attribute;
	
	public AssetDeploymentTableColumn(FixedColumn col) {
		this.column = col;
	}
	
	public AssetDeploymentTableColumn(AssetAttribute attribute) {
		this.attribute = attribute;
	}
	
	public String getColumnName() {
		if (column != null) return column.guiName;
		if (attribute != null) return attribute.getName();
		return "";
		
	}
	
	@Override
	public String getText(Object element) {
		if (! (element instanceof AssetDeployment)) return super.getText(element);
		AssetDeployment deployment = (AssetDeployment) element;
		if (column != null) {
			switch(column) {
			case END_DATE:
				if (deployment.getEndDate() == null) return "Current";
				return DateFormat.getDateTimeInstance().format(deployment.getEndDate());
			case LOCATION:
				return deployment.getStationLocation().getId();
			case NUM_INCIDENTS:
				return "TODO:"; //TODO:
			case START_DATE:
				return DateFormat.getDateTimeInstance().format(deployment.getStartDate());
			case STATION:
				return deployment.getStationLocation().getStation().getId();
			case TOTAL_TIME:
				Date end = new Date();
				if (deployment.getEndDate() != null) end = deployment.getEndDate();
				long diff = end.getTime() - deployment.getStartDate().getTime();
				return AssetUtils.formatTime(diff / 1000.0);			
			}
		}else if (attribute != null ) {
			if (deployment.getAttributeValues() != null) {
				for (AssetDeploymentAttributeValue v : deployment.getAttributeValues()) {
					if (v.getAttribute().equals(attribute)) {
						return v.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS); //TODO: crs provider
					}
				}
			}
			return "";
		}
		return "TODO: this hsould never happen";//TODO:
	}
	
//	public int compare(Object v1, Object v2) {
//		
//	}
	
	
	public static List<AssetDeploymentTableColumn> getTableColumns(Asset asset, Session session){
		AssetType type = session.get(AssetType.class, asset.getAssetType().getUuid());
		if (type == null) return Collections.emptyList();
		
		 List<AssetDeploymentTableColumn> columns = new ArrayList<>();
		 for (FixedColumn c : FixedColumn.values()) {
			 columns.add(new AssetDeploymentTableColumn(c));
		 }
		 
		 for(AssetTypeDeploymentAttribute attribute : type.getAssetDeploymentAttributes()) {
			 columns.add(new AssetDeploymentTableColumn(attribute.getAttribute()));
		 }
		 return columns;
	}
}
