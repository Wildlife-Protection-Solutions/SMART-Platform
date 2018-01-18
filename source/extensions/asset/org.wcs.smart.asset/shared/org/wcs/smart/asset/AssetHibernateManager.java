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
package org.wcs.smart.asset;

import java.text.Collator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.QueryFactory;


/**
 * Common hibernate queries for intelligence.
 * 
 * @author Emily
 *
 */
public class AssetHibernateManager {
	
	private static Logger logger = Logger.getLogger(AssetHibernateManager.class.getCanonicalName());
	
	public static AssetAttribute getAttribute(String keyId, ConservationArea ca, Session session){ 
		return QueryFactory.buildQuery(session, AssetAttribute.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", keyId}).uniqueResult(); //$NON-NLS-1$
	}
	
	
	public static AssetAttributeListItem getAttributeListItem(AssetAttribute attribute, String keyId, Session session){ 
		if (attribute == null) return null;
		return QueryFactory.buildQuery(session, AssetAttributeListItem.class, 
				new Object[] {"attribute", attribute}, //$NON-NLS-1$
				new Object[] {"keyId", keyId}).uniqueResult(); //$NON-NLS-1$
	}
	
	public static AssetType getAssetType(String keyId, ConservationArea ca, Session session){ 
		return QueryFactory.buildQuery(session, AssetType.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", keyId}).uniqueResult(); //$NON-NLS-1$

	}
	
	public static Asset getAsset(UUID entityUuid, Session session){
		return (Asset) session.get(Asset.class, entityUuid);
	}
	
	/**
	 * Gets all attributes sorted by name. Does not lazily load list items, translations etc.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public static List<AssetAttribute> getAttributes(Session session, ConservationArea ca){
		List<AssetAttribute> types = QueryFactory.buildQuery(session, AssetAttribute.class, "conservationArea", ca).getResultList();  //$NON-NLS-1$
		types.sort((AssetAttribute a, AssetAttribute b) -> Collator.getInstance().compare(a.getName() == null ? "" : a.getName(), b.getName() == null ? "" : b.getName())); //$NON-NLS-1$ //$NON-NLS-2$
		return types;
	}
	
	/**
	 * converts an attribute type to an sql type
	 * @param type
	 * @return
	 */
	public static int getAttributeSqlType(AttributeType type){
		switch(type){
		case BOOLEAN:
			return java.sql.Types.BOOLEAN;
		case DATE:
			return java.sql.Types.DATE;
		case NUMERIC:
			return java.sql.Types.DOUBLE;
		case POSITION:
			return java.sql.Types.VARCHAR;
		case LIST:
		case TEXT:
			return java.sql.Types.VARCHAR;
		};
		return -1;
	}
	
	/**
	 * Return the current value for the station buffer.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public static double getStationBuffer(Session session, ConservationArea ca) {
		AssetModuleSettings setting = QueryFactory.buildQuery(session, AssetModuleSettings.class, 
				new Object[]{"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", AssetModuleSettings.STATION_BUFFER_KEY}).uniqueResult(); //$NON-NLS-1$
		if (setting == null) {
			return AssetModuleSettings.STATION_BUFFER_DEFAULT_VALUE;
		}
		try {
			Double d = Double.valueOf(setting.getValue());
			return d;
		}catch (Exception ex) {
			logger.log(Level.WARNING, ex.getMessage(), ex);
		}
		return AssetModuleSettings.STATION_BUFFER_DEFAULT_VALUE;
	}
	
	/**
	 * Return the current value for the station location buffer.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public static double getStationLocationBuffer(Session session, ConservationArea ca) {
		AssetModuleSettings setting = QueryFactory.buildQuery(session, AssetModuleSettings.class, 
				new Object[]{"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", AssetModuleSettings.LOCATION_BUFFER_KEY}).uniqueResult(); //$NON-NLS-1$
		if (setting == null) {
			return AssetModuleSettings.LOCATION_BUFFER_DEFAULT_VALUE;
		}
		try {
			Double d = Double.valueOf(setting.getValue());
			return d;
		}catch (Exception ex) {
			logger.log(Level.WARNING, ex.getMessage(), ex);
		}
		return AssetModuleSettings.LOCATION_BUFFER_DEFAULT_VALUE;
	}
}
