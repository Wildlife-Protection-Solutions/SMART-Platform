package org.wcs.smart.asset.query.model;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.ui.IQueryAssetLabelProvider;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Valid asset filter options.
 */
public enum AssetFilterOption  {
	
	ASSET("asset", "uuid", Asset.class, Asset.class, AssetQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	STATION("station", "uuid", AssetStation.class, Station.class, AssetQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	STATIONLOCATION("location", "uuid", AssetStationLocation.class, Station.class, AssetQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	ASSETTYPE("assettype", "uuid", AssetType.class, AssetType.class, AssetQueryOptionType.UUID),
	
	CONSERVATION_AREA("ca", "ca_uuid", Waypoint.class, ConservationArea.class, AssetQueryOptionType.UUID);  //$NON-NLS-1$//$NON-NLS-2$
	
	private String key;			//unique identifier key
	private String columnName;	//column name in database table
	private Class<?> clazz;		//class containing the attribute
	private Class<?> sourceClazz; //class that represents the object
	private AssetQueryOptionType type; //data type; if boolean, uuid or string field
	
	AssetFilterOption(String queryKey, 
			String columnName, Class<?> clazz, 
			Class<?> sourceClazz, AssetQueryOptionType type){

		this.key = queryKey;
		this.columnName = columnName;
		this.clazz = clazz;
		this.sourceClazz = sourceClazz;
		this.type = type;
	}
	
	
	
	/**
	 * @return gui name
	 */
	public String getGuiName(Locale l){
		return SmartContext.INSTANCE.getClass(IQueryAssetLabelProvider.class).getLabel(this, l);
	}
	
	/**
	 * @return the option key
	 */
	public String getKey(){
		return this.key;
	}
	
	/**
	 * @return the database column name
	 */
	public String getColumnName(){
		return this.columnName;
	}
	
	/**
	 * @return option type
	 */
	public AssetQueryOptionType getType(){
		return this.type;
	}
	
	/**
	 * 
	 * @return the class this asset option is an attribute of
	 */
	public Class<?> getAssetAttributeClass(){
		return this.clazz;
	}

	/**
	 * @return the class that represents the option
	 */
	public Class<?> getSourceClass(){
		return this.sourceClazz;
	}

	
	/**
	 * Given a particular uuid (key) determine the string
	 * name for the given option.
	 * 
	 * @param session
	 * @param uuid
	 * @return
	 */
	public String getName(Session session, UUID uuid, Locale l){
		Object x = getObject(session, uuid);
		if (x != null){
			if (x instanceof Asset) {
				return ((Asset) x).getId();
			}else if (x instanceof AssetStation) {
				return ((AssetStation) x).getId();
			}else if (x instanceof AssetStationLocation) {
				return ((AssetStationLocation) x).getId();
			}else if (x instanceof NamedItem){
				return ((NamedItem) x).getName();
			}
		}
		return null;
	}
	
//	/**
//	 * Return an array of names that represent
//	 * the uuid for a given option.  Name provided in 
//	 * the conservation area default language.
//	 * 
//	 * @param session
//	 * @param uuid
//	 * @return if this option represents employee then
//	 * an array of the employeeid, givenname, familyname is returned,
//	 * otherwise a simple element array with the object 
//	 * name is returned.
//	 */
//	public String[] getNames(Session session, UUID uuid, Locale l){
//		Object x = getObject(session, uuid);
//		if (x != null){
//			if (x instanceof NamedItem){
//				return new String[]{((NamedItem)x).getDefaultName()};
//			}else if (x instanceof Employee){
//				Employee e = (Employee)x;
//				return new String[]{e.getId(), e.getGivenName(), e.getFamilyName()};
//			}
//		}
//		return null;
//	}
	
	/**
	 * Give a particular uuid return the source 
	 * object (returns a Team, Station etc. object)
	 * @param session
	 * @param uuid
	 * @return
	 */
	public Object getObject(Session session, UUID uuid){
		List<?> data = QueryFactory.buildQuery(session, sourceClazz, new Object[] {"uuid", uuid}).getResultList(); //$NON-NLS-1$
		if (data.size() == 0){
			return null; //nothing found
		}else if (data.size() > 1){
			assert false; //should never happen
		}
		return data.get(0);
	}
	
	
}