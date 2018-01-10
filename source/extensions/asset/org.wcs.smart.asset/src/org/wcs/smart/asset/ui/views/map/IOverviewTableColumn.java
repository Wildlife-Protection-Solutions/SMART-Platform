package org.wcs.smart.asset.ui.views.map;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.ui.SmartLabelProvider;

public interface IOverviewTableColumn {

	public static enum GroupByOption{
		STATION,LOCATION
	};
	
	/**
	 * Possible column types
	 */
	public enum ColumnType{
		INTEGER("Integer", java.sql.Types.INTEGER), //$NON-NLS-1$
		LONG("Integer", java.sql.Types.INTEGER), //$NON-NLS-1$
		NUMBER("Double", java.sql.Types.DOUBLE), //$NON-NLS-1$
		STRING("String", java.sql.Types.VARCHAR), //$NON-NLS-1$
		BOOLEAN("Integer", java.sql.Types.BOOLEAN), //$NON-NLS-1$
		DATE("Date", java.sql.Types.DATE), //$NON-NLS-1$
		TIME("Date", java.sql.Types.TIME), //$NON-NLS-1$
		TIME_STR("String", java.sql.Types.VARCHAR); //$NON-NLS-1$
		
		public String geotoolsType;
		public int sqlType;
		
		ColumnType(String geotoolsType, int sqlType){
			this.geotoolsType = geotoolsType;
			this.sqlType = sqlType;
		}
		
		public int getSqlType(){
			return this.sqlType;
		}
		
		public String asString(Object value) {
			if (value == null) return "";
			switch(this) {
			case BOOLEAN:
				if ((Boolean)value) {
					return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
				}else {
					return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
				}
			case DATE:
				return DateFormat.getDateInstance().format((Date)value);
			case INTEGER:
				return ((Integer)value).toString();
			case LONG:
				return ((Long)value).toString();
			case NUMBER:
				return ((Number)value).toString();
			case TIME_STR:
			case STRING:
				return ((String)value);
			case TIME:
				return DateFormat.getTimeInstance().format((Date)value);
			}
			return value.toString();
		}
	}
	
	/**
	 * 
	 * @return the gui name of the column
	 */
	public String getName();
	
	/**
	 * A unique key for identifying the column.  This is used for
	 * the udig feature fields so that the ui can have different names for
	 * various languages but the udig/geotools styles will
	 * still work. 
	 * 
	 * Key should only contain a-z and 0-9 characters
	 * 
	 * @return
	 */
	public String getKey();
	
	/**
	 * Get the value for the column given the data row
	 * @param data
	 * @return
	 */
	public Object getValue(StationData data);
	
	/**
	 * Get the type of column
	 * @return
	 */
	public ColumnType getType();
	
	/**
	 * 
	 * @param session
	 * @param dFilter may be null if all dates should be included
	 * @return
	 */
	public HashMap<AssetStation, Object> computeValuesByStation(Session session, Date[] dFilter);
	
	/**
	 * 
	 * @param session
	 * @param dFilter may be null if all dates should be included
	 * @return
	 */
	public HashMap<AssetStationLocation, Object> computeValuesByStationLocation(Session session, Date[] dFilter);
	
	/**
	 * convert the column to a JSON object that defines the column
	 * 
	 * @return
	 */
	public JSONObject serialize();
}
